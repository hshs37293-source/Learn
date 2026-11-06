package dev.mcgui.gfx;

import dev.mcgui.math.Vec2;

import java.util.ArrayList;
import java.util.List;

/** Ścieżka wektorowa (jak w SVG/Canvas): linie, krzywe kwadratowe i sześcienne. */
public final class Path {
    private sealed interface Seg permits Move, Line, Quad, Cubic, Close {}
    private record Move(Vec2 p) implements Seg {}
    private record Line(Vec2 p) implements Seg {}
    private record Quad(Vec2 c, Vec2 p) implements Seg {}
    private record Cubic(Vec2 c1, Vec2 c2, Vec2 p) implements Seg {}
    private record Close() implements Seg {}

    private final List<Seg> segs = new ArrayList<>();

    public Path moveTo(float x, float y) { segs.add(new Move(new Vec2(x, y))); return this; }
    public Path lineTo(float x, float y) { segs.add(new Line(new Vec2(x, y))); return this; }
    public Path quadTo(float cx, float cy, float x, float y) { segs.add(new Quad(new Vec2(cx, cy), new Vec2(x, y))); return this; }
    public Path cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
        segs.add(new Cubic(new Vec2(c1x, c1y), new Vec2(c2x, c2y), new Vec2(x, y)));
        return this;
    }
    public Path close() { segs.add(new Close()); return this; }
    public boolean isEmpty() { return segs.isEmpty(); }

    public static Path rect(float x, float y, float w, float h) {
        return new Path().moveTo(x, y).lineTo(x + w, y).lineTo(x + w, y + h).lineTo(x, y + h).close();
    }
    public static Path circle(float cx, float cy, float r) {
        // 4 krzywe sześcienne; k = 0.5522847498
        float k = 0.5522847498f * r;
        return new Path().moveTo(cx + r, cy)
            .cubicTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
            .cubicTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
            .cubicTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
            .cubicTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
            .close();
    }
    public static Path polygon(float... xy) {
        Path p = new Path();
        for (int i = 0; i + 1 < xy.length; i += 2) {
            if (i == 0) p.moveTo(xy[0], xy[1]); else p.lineTo(xy[i], xy[i + 1]);
        }
        return p.close();
    }
    public static Path star(float cx, float cy, float outer, float inner, int points) {
        float[] xy = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = -Math.PI / 2 + i * Math.PI / points;
            float r = (i % 2 == 0) ? outer : inner;
            xy[i * 2] = cx + (float) Math.cos(a) * r;
            xy[i * 2 + 1] = cy + (float) Math.sin(a) * r;
        }
        return polygon(xy);
    }

    /** Jedna spłaszczona konturowa łamana. */
    public record Contour(List<Vec2> points, boolean closed) {}

    /** Zamienia krzywe na łamane; {@code tolerance} = maksymalne odchylenie w jednostkach GUI. */
    public List<Contour> flatten(float tolerance) {
        List<Contour> out = new ArrayList<>();
        List<Vec2> cur = null;
        Vec2 pos = Vec2.ZERO, start = Vec2.ZERO;
        for (Seg s : segs) {
            if (s instanceof Move m) {
                if (cur != null && cur.size() > 1) out.add(new Contour(cur, false));
                cur = new ArrayList<>();
                cur.add(m.p());
                pos = start = m.p();
            } else if (s instanceof Line l) {
                if (cur == null) { cur = new ArrayList<>(); cur.add(pos); }
                cur.add(l.p());
                pos = l.p();
            } else if (s instanceof Quad q) {
                if (cur == null) { cur = new ArrayList<>(); cur.add(pos); }
                int n = segmentsFor(pos, q.c(), q.p(), tolerance);
                for (int i = 1; i <= n; i++) {
                    float t = i / (float) n, u = 1 - t;
                    cur.add(new Vec2(u * u * pos.x() + 2 * u * t * q.c().x() + t * t * q.p().x(),
                                     u * u * pos.y() + 2 * u * t * q.c().y() + t * t * q.p().y()));
                }
                pos = q.p();
            } else if (s instanceof Cubic c) {
                if (cur == null) { cur = new ArrayList<>(); cur.add(pos); }
                int n = Math.max(segmentsFor(pos, c.c1(), c.p(), tolerance), segmentsFor(pos, c.c2(), c.p(), tolerance));
                for (int i = 1; i <= n; i++) {
                    float t = i / (float) n, u = 1 - t;
                    float b0 = u * u * u, b1 = 3 * u * u * t, b2 = 3 * u * t * t, b3 = t * t * t;
                    cur.add(new Vec2(b0 * pos.x() + b1 * c.c1().x() + b2 * c.c2().x() + b3 * c.p().x(),
                                     b0 * pos.y() + b1 * c.c1().y() + b2 * c.c2().y() + b3 * c.p().y()));
                }
                pos = c.p();
            } else if (s instanceof Close) {
                if (cur != null && cur.size() > 1) {
                    // usuń duplikat ostatniego punktu równego pierwszemu
                    Vec2 first = cur.get(0), last = cur.get(cur.size() - 1);
                    if (first.sub(last).length() < 1e-5f) cur.remove(cur.size() - 1);
                    out.add(new Contour(cur, true));
                }
                cur = null;
                pos = start;
            }
        }
        if (cur != null && cur.size() > 1) out.add(new Contour(cur, false));
        return out;
    }

    private static int segmentsFor(Vec2 a, Vec2 c, Vec2 b, float tol) {
        float d = c.sub(a).length() + b.sub(c).length();
        return Math.max(2, Math.min(64, (int) Math.ceil(Math.sqrt(d / Math.max(tol, 0.01f)) * 1.5)));
    }
}
