package dev.mcgui.raster;

import dev.mcgui.gfx.BoxStyle;
import dev.mcgui.gfx.Border;
import dev.mcgui.gfx.Color;
import dev.mcgui.gfx.CornerRadii;
import dev.mcgui.gfx.Paint;
import dev.mcgui.gfx.Shadow;
import dev.mcgui.math.Rect;

import java.util.ArrayList;
import java.util.List;

/** Zamienia {@link BoxStyle} na listę elementów SDF: cienie, wypełnienie, obrys (w tej kolejności). Czysta logika, testowalna. */
public final class SdfEmitter {
    private SdfEmitter() {}

    /**
     * @param px    prostokąt w pikselach fizycznych
     * @param scale piksele fizyczne na jednostkę logiczną (promienie, grubości i cienie w stylu są logiczne)
     * @param alpha mnożnik przezroczystości warstwy
     */
    public static List<SdfElement> emit(Rect px, BoxStyle style, float scale, float alpha) {
        List<SdfElement> out = new ArrayList<>(4);
        if (px.w() <= 0 || px.h() <= 0) return out;
        CornerRadii radii = style.radii().scale(scale).clampTo(px.w(), px.h());

        for (Shadow sh : style.shadows()) {
            if (sh.inset() || !sh.isVisible()) continue;          // cienie wewnętrzne: nieobsługiwane
            float spread = sh.spread() * scale, blur = Math.max(sh.blur() * scale, 0.5f);
            float sx = px.x() + sh.dx() * scale - spread, sy = px.y() + sh.dy() * scale - spread;
            float sw = px.w() + 2 * spread, shh = px.h() + 2 * spread;
            if (sw <= 0 || shh <= 0) continue;
            CornerRadii sr = grow(radii, spread).clampTo(sw, shh);
            int c = Color.multiplyAlpha(sh.color(), alpha);
            out.add(new SdfElement(SdfElement.Kind.SHADOW, sx - blur, sy - blur, sw + 2 * blur, shh + 2 * blur, sr, c, c, c, c, blur));
        }

        if (style.fill().isVisible()) {
            int[] c = corners(style.fill(), alpha);
            out.add(new SdfElement(SdfElement.Kind.FILL, px.x(), px.y(), px.w(), px.h(), radii, c[0], c[1], c[2], c[3], 0));
        }

        Border b = style.border();
        if (b.isVisible()) {
            float w = b.width() * scale;
            float off = switch (b.placement()) { case INSIDE -> 0; case CENTER -> w / 2f; case OUTSIDE -> w; };
            float ow = px.w() + 2 * off, oh = px.h() + 2 * off;
            CornerRadii or = grow(radii, off).clampTo(ow, oh);
            int c = Color.multiplyAlpha(b.color(), alpha);
            out.add(new SdfElement(SdfElement.Kind.OUTLINE, px.x() - off, px.y() - off, ow, oh, or, c, c, c, c, Math.min(w, Math.min(ow, oh) / 2f)));
        }
        return out;
    }

    private static CornerRadii grow(CornerRadii r, float d) {
        return new CornerRadii(Math.max(0, r.tl() + d), Math.max(0, r.tr() + d), Math.max(0, r.br() + d), Math.max(0, r.bl() + d));
    }

    /** Kolory narożników [TL, TR, BR, BL] dla wypełnienia. Radialne: kolor średni (SDF nie ma na to atrybutów). */
    public static int[] corners(Paint p, float alpha) {
        if (p instanceof Paint.Solid s) { int c = Color.multiplyAlpha(s.argb(), alpha); return new int[] { c, c, c, c }; }
        if (p instanceof Paint.Linear l) {
            float dx = l.x1() - l.x0(), dy = l.y1() - l.y0(), len2 = dx * dx + dy * dy;
            int[] out = new int[4];
            float[][] pts = { {0, 0}, {1, 0}, {1, 1}, {0, 1} };
            for (int i = 0; i < 4; i++) {
                float t = len2 == 0 ? 0 : ((pts[i][0] - l.x0()) * dx + (pts[i][1] - l.y0()) * dy) / len2;
                out[i] = Color.multiplyAlpha(Color.mix(l.c0(), l.c1(), t), alpha);
            }
            return out;
        }
        int c = Color.multiplyAlpha(p.representative(), alpha);
        return new int[] { c, c, c, c };
    }
}
