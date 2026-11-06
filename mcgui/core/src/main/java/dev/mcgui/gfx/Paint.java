package dev.mcgui.gfx;

/**
 * Wypełnienie: kolor jednolity lub gradient.
 * Współrzędne gradientów są znormalizowane do prostokąta elementu (0..1).
 */
public sealed interface Paint {
    record Solid(int argb) implements Paint {}

    /** Gradient liniowy od (x0,y0) do (x1,y1), współrzędne w jednostkach prostokąta (0..1). */
    record Linear(float x0, float y0, float x1, float y1, int c0, int c1) implements Paint {}

    /** Gradient radialny od środka (cx,cy) o promieniu r (0..1 względem połowy przekątnej). */
    record Radial(float cx, float cy, float r, int inner, int outer) implements Paint {}

    static Paint solid(int argb) { return new Solid(argb); }
    static Paint vertical(int top, int bottom) { return new Linear(0, 0, 0, 1, top, bottom); }
    static Paint horizontal(int left, int right) { return new Linear(0, 0, 1, 0, left, right); }
    static Paint diagonal(int c0, int c1) { return new Linear(0, 0, 1, 1, c0, c1); }
    static Paint radial(int inner, int outer) { return new Radial(0.5f, 0.5f, 1f, inner, outer); }
    Paint NONE = new Solid(Color.TRANSPARENT);

    /** Kolor reprezentatywny (do uproszczonych backendów bez gradientów). */
    default int representative() {
        if (this instanceof Solid s) return s.argb();
        if (this instanceof Linear l) return Color.mix(l.c0(), l.c1(), 0.5f);
        if (this instanceof Radial r) return Color.mix(r.inner(), r.outer(), 0.5f);
        return 0;
    }

    default boolean isVisible() {
        if (this instanceof Solid s) return Color.a(s.argb()) > 0;
        if (this instanceof Linear l) return Color.a(l.c0()) > 0 || Color.a(l.c1()) > 0;
        if (this instanceof Radial r) return Color.a(r.inner()) > 0 || Color.a(r.outer()) > 0;
        return false;
    }

    /** Mnoży alfę wszystkich kolorów przez f. */
    default Paint withAlphaFactor(float f) {
        if (this instanceof Solid s) return new Solid(Color.multiplyAlpha(s.argb(), f));
        if (this instanceof Linear l)
            return new Linear(l.x0(), l.y0(), l.x1(), l.y1(), Color.multiplyAlpha(l.c0(), f), Color.multiplyAlpha(l.c1(), f));
        Radial r = (Radial) this;
        return new Radial(r.cx(), r.cy(), r.r(), Color.multiplyAlpha(r.inner(), f), Color.multiplyAlpha(r.outer(), f));
    }
}
