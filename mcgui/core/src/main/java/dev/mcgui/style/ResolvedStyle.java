package dev.mcgui.style;

import dev.mcgui.anim.Easing;
import dev.mcgui.gfx.BoxStyle;
import dev.mcgui.gfx.Color;
import dev.mcgui.gfx.FontRef;
import dev.mcgui.gfx.LayerStyle;
import dev.mcgui.gfx.Paint;
import dev.mcgui.gfx.TextStyle;
import dev.mcgui.math.Insets;

/** Styl po rozwiązaniu (wszystkie pola ustawione), gotowy do rysowania. Potrafi się interpolować. */
public record ResolvedStyle(BoxStyle box, TextStyle text, Insets padding, float minWidth, float minHeight,
                            LayerStyle layer, int transitionMs, Easing easing) {

    public static ResolvedStyle from(RuleData d) {
        BoxStyle box = new BoxStyle(d.fill, d.radii, d.border, d.shadows);
        TextStyle text = new TextStyle(FontRef.DEFAULT, d.textSize == null ? 9f : d.textSize,
            d.textColor == null ? Color.WHITE : d.textColor, TextStyle.Align.LEFT,
            d.textShadow != null && d.textShadow, false);
        LayerStyle layer = new LayerStyle(d.layerAlpha == null ? 1f : d.layerAlpha,
            d.backdropBlur == null ? 0f : d.backdropBlur, LayerStyle.Blend.NORMAL);
        return new ResolvedStyle(box, text, d.padding == null ? Insets.NONE : d.padding,
            d.minWidth == null ? 0 : d.minWidth, d.minHeight == null ? 0 : d.minHeight, layer,
            d.transitionMs == null ? 0 : d.transitionMs, Easing.parse(d.easing));
    }

    /** Płynne przejście między dwoma stylami (t = 0 → this, t = 1 → o). */
    public ResolvedStyle lerp(ResolvedStyle o, float t) {
        if (t <= 0) return this;
        if (t >= 1) return o;
        BoxStyle a = box, b = o.box;
        Paint fill = lerpPaint(a.fill(), b.fill(), t);
        java.util.List<dev.mcgui.gfx.Shadow> shadows = lerpShadows(a.shadows(), b.shadows(), t);
        BoxStyle nb = new BoxStyle(fill, a.radii().lerp(b.radii(), t), a.border().lerp(b.border(), t), shadows);
        return new ResolvedStyle(nb, text.lerp(o.text, t),
            new Insets(padding.left() + (o.padding.left() - padding.left()) * t, padding.top() + (o.padding.top() - padding.top()) * t,
                padding.right() + (o.padding.right() - padding.right()) * t, padding.bottom() + (o.padding.bottom() - padding.bottom()) * t),
            minWidth + (o.minWidth - minWidth) * t, minHeight + (o.minHeight - minHeight) * t,
            new LayerStyle(layer.alpha() + (o.layer.alpha() - layer.alpha()) * t,
                layer.backdropBlur() + (o.layer.backdropBlur() - layer.backdropBlur()) * t, layer.blend()),
            o.transitionMs, o.easing);
    }

    private static Paint lerpPaint(Paint a, Paint b, float t) {
        if (a instanceof Paint.Solid sa && b instanceof Paint.Solid sb) return Paint.solid(Color.mix(sa.argb(), sb.argb(), t));
        if (a instanceof Paint.Linear la && b instanceof Paint.Linear lb)
            return new Paint.Linear(la.x0() + (lb.x0() - la.x0()) * t, la.y0() + (lb.y0() - la.y0()) * t,
                la.x1() + (lb.x1() - la.x1()) * t, la.y1() + (lb.y1() - la.y1()) * t,
                Color.mix(la.c0(), lb.c0(), t), Color.mix(la.c1(), lb.c1(), t));
        // różne rodzaje wypełnień: płynnie przez zanik jednego i pojawienie się drugiego nie jest możliwe w jednym Paint,
        // więc przełączamy w połowie
        return t < 0.5f ? a : b;
    }

    private static java.util.List<dev.mcgui.gfx.Shadow> lerpShadows(java.util.List<dev.mcgui.gfx.Shadow> a,
                                                                      java.util.List<dev.mcgui.gfx.Shadow> b, float t) {
        int n = Math.max(a.size(), b.size());
        java.util.List<dev.mcgui.gfx.Shadow> out = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            dev.mcgui.gfx.Shadow sa = i < a.size() ? a.get(i) : transparent(b.get(i));
            dev.mcgui.gfx.Shadow sb = i < b.size() ? b.get(i) : transparent(a.get(i));
            out.add(sa.lerp(sb, t));
        }
        return out;
    }

    private static dev.mcgui.gfx.Shadow transparent(dev.mcgui.gfx.Shadow s) {
        return new dev.mcgui.gfx.Shadow(s.dx(), s.dy(), s.blur(), s.spread(), Color.withAlpha(s.color(), 0), s.inset());
    }
}
