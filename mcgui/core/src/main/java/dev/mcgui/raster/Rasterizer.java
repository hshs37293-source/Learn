package dev.mcgui.raster;

import dev.mcgui.gfx.CornerRadii;

/**
 * Programowa rasteryzacja kształtów na prostokąty (strips) — dla backendów, które potrafią
 * tylko "wypełnij prostokąt". Współrzędne w pikselach fizycznych. Krawędzie mają częściowe
 * pokrycie (antyaliasing), przekazywane jako {@code coverage} 0..1 do {@link Sink}.
 */
public final class Rasterizer {
    private Rasterizer() {}

    /** Odbiorca prostokątów z pokryciem (0..1). */
    public interface Sink { void fill(float x0, float y0, float x1, float y1, float coverage); }

    // ---------- zaokrąglony prostokąt ----------

    /** Wypełnia zaokrąglony prostokąt; kolejne wiersze o identycznych zakresach są scalane w bloki, chyba że {@code perRow}. */
    public static void roundedRect(float x, float y, float w, float h, CornerRadii radii, boolean perRow, Sink sink) {
        if (w <= 0 || h <= 0) return;
        CornerRadii r = radii.clampTo(w, h);
        int rowStart = (int) Math.floor(y), rowEnd = (int) Math.ceil(y + h);
        float[] prev = null;
        int blockStart = rowStart;
        for (int row = rowStart; row < rowEnd; row++) {
            float top = Math.max(row, y), bot = Math.min(row + 1, y + h);
            float vCov = bot - top;                                  // częściowe pokrycie pionowe (krawędź górna/dolna)
            float yc = (top + bot) / 2f;
            float l = leftEdge(yc, x, y, h, r), rt = rightEdge(yc, x, y, w, h, r);
            float[] cur = { l, rt, vCov };
            if (!perRow && prev != null && same(prev, cur)) continue;
            if (!perRow && prev != null) flushBlock(sink, prev, blockStart, row);
            if (perRow) { emitRow(sink, cur, row, row + 1); prev = null; blockStart = row + 1; }
            else { prev = cur; blockStart = row; }
        }
        if (!perRow && prev != null) flushBlock(sink, prev, blockStart, rowEnd);
    }

    private static void flushBlock(Sink s, float[] span, int r0, int r1) { emitRow(s, span, r0, r1); }

    private static boolean same(float[] a, float[] b) {
        return Math.abs(a[0] - b[0]) < 1e-4f && Math.abs(a[1] - b[1]) < 1e-4f && Math.abs(a[2] - b[2]) < 1e-4f;
    }

    /** Emituje: lewy częściowy piksel, środek, prawy częściowy piksel dla wierszy [r0, r1). */
    private static void emitRow(Sink s, float[] span, int r0, int r1) {
        float l = span[0], r = span[1], vcov = span[2];
        if (r <= l || vcov <= 0) return;
        int li = (int) Math.ceil(l), ri = (int) Math.floor(r);
        if (ri < li) {                                               // wąski pasek w obrębie jednego piksela
            s.fill((float) Math.floor(l), r0, (float) Math.floor(l) + 1, r1, (r - l) * vcov);
            return;
        }
        if (li > l) s.fill(li - 1, r0, li, r1, (li - l) * vcov);
        if (ri > li) s.fill(li, r0, ri, r1, vcov);
        if (r > ri) s.fill(ri, r0, ri + 1, r1, (r - ri) * vcov);
    }

    private static float leftEdge(float yc, float x, float y, float h, CornerRadii r) {
        float inset = 0;
        if (yc < y + r.tl()) inset = Math.max(inset, arcInset(r.tl(), y + r.tl() - yc));
        if (yc > y + h - r.bl()) inset = Math.max(inset, arcInset(r.bl(), yc - (y + h - r.bl())));
        return x + inset;
    }

    private static float rightEdge(float yc, float x, float y, float w, float h, CornerRadii r) {
        float inset = 0;
        if (yc < y + r.tr()) inset = Math.max(inset, arcInset(r.tr(), y + r.tr() - yc));
        if (yc > y + h - r.br()) inset = Math.max(inset, arcInset(r.br(), yc - (y + h - r.br())));
        return x + w - inset;
    }

    /** Odsunięcie krawędzi od bokiem prostokąta dla łuku o promieniu r na wysokości dy od środka łuku. */
    private static float arcInset(float r, float dy) {
        float d = Math.min(dy, r);
        return r - (float) Math.sqrt(Math.max(0, r * r - d * d));
    }

    // ---------- pierścień (obramowanie) ----------

    /** Obszar między zewnętrznym a wewnętrznym zaokrąglonym prostokątem. Zawsze wiersz po wierszu. */
    public static void ring(float x, float y, float w, float h, CornerRadii radii, float thickness, Sink sink) {
        if (w <= 0 || h <= 0 || thickness <= 0) return;
        CornerRadii ro = radii.clampTo(w, h);
        float t = Math.min(thickness, Math.min(w, h) / 2f);
        float iw = w - 2 * t, ih = h - 2 * t;
        CornerRadii ri = new CornerRadii(Math.max(0, ro.tl() - t), Math.max(0, ro.tr() - t),
                                         Math.max(0, ro.br() - t), Math.max(0, ro.bl() - t)).clampTo(Math.max(iw, 0), Math.max(ih, 0));
        int rowStart = (int) Math.floor(y), rowEnd = (int) Math.ceil(y + h);
        for (int row = rowStart; row < rowEnd; row++) {
            float top = Math.max(row, y), bot = Math.min(row + 1, y + h);
            float vCov = bot - top, yc = (top + bot) / 2f;
            float ol = leftEdge(yc, x, y, h, ro), or = rightEdge(yc, x, y, w, h, ro);
            boolean insideInner = iw > 0 && ih > 0 && yc > y + t && yc < y + h - t;
            if (!insideInner) { emitRow(sink, new float[] { ol, or, vCov }, row, row + 1); continue; }
            float il = leftEdge(yc, x + t, y + t, ih, ri), ir = rightEdge(yc, x + t, y + t, iw, ih, ri);
            emitRow(sink, new float[] { ol, Math.max(ol, il), vCov }, row, row + 1);       // lewy pas
            emitRow(sink, new float[] { Math.min(or, ir), or, vCov }, row, row + 1);       // prawy pas
        }
    }

    // ---------- trójkąt ----------

    /** Rasteryzuje trójkąt wierszami (próbkowanie w środku wiersza), z częściowym pokryciem krawędzi poziomych. */
    public static void triangle(float x0, float y0, float x1, float y1, float x2, float y2, Sink sink) {
        float minY = Math.min(y0, Math.min(y1, y2)), maxY = Math.max(y0, Math.max(y1, y2));
        if (maxY - minY < 1e-6f) return;
        int rs = (int) Math.floor(minY), re = (int) Math.ceil(maxY);
        float[] xs = new float[3];
        for (int row = rs; row < re; row++) {
            float top = Math.max(row, minY), bot = Math.min(row + 1, maxY);
            float yc = (top + bot) / 2f, vcov = bot - top;
            int n = 0;
            n = edge(x0, y0, x1, y1, yc, xs, n);
            n = edge(x1, y1, x2, y2, yc, xs, n);
            n = edge(x2, y2, x0, y0, yc, xs, n);
            if (n < 2) continue;
            float l = Math.min(xs[0], Math.min(xs[1], n > 2 ? xs[2] : xs[1]));
            float r = Math.max(xs[0], Math.max(xs[1], n > 2 ? xs[2] : xs[1]));
            emitRow(sink, new float[] { l, r, vcov }, row, row + 1);
        }
    }

    private static int edge(float ax, float ay, float bx, float by, float y, float[] out, int n) {
        if ((ay <= y && by > y) || (by <= y && ay > y)) {
            float t = (y - ay) / (by - ay);
            if (n < out.length) out[n++] = ax + (bx - ax) * t;
        }
        return n;
    }

    // ---------- gruba linia ----------

    /** Linia o grubości {@code width} jako dwa trójkąty. */
    public static void line(float x0, float y0, float x1, float y1, float width, Sink sink) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6f) return;
        float nx = -dy / len * width / 2f, ny = dx / len * width / 2f;
        triangle(x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, sink);
        triangle(x0 + nx, y0 + ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, sink);
    }
}
