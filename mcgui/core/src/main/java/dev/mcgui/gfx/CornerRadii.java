package dev.mcgui.gfx;

/** Promienie czterech rogów: lewy-górny, prawy-górny, prawy-dolny, lewy-dolny. */
public record CornerRadii(float tl, float tr, float br, float bl) {
    public static final CornerRadii NONE = new CornerRadii(0, 0, 0, 0);
    public static CornerRadii all(float r) { return new CornerRadii(r, r, r, r); }
    public static CornerRadii top(float r) { return new CornerRadii(r, r, 0, 0); }
    public static CornerRadii bottom(float r) { return new CornerRadii(0, 0, r, r); }
    public boolean isZero() { return tl == 0 && tr == 0 && br == 0 && bl == 0; }
    public boolean isUniform() { return tl == tr && tr == br && br == bl; }
    public float max() { return Math.max(Math.max(tl, tr), Math.max(br, bl)); }

    /** Ogranicza promienie tak, by nie przekraczały połowy krótszego boku (jak CSS). */
    public CornerRadii clampTo(float w, float h) {
        float lim = Math.max(0, Math.min(w, h) / 2f);
        return new CornerRadii(Math.min(tl, lim), Math.min(tr, lim), Math.min(br, lim), Math.min(bl, lim));
    }
    public CornerRadii scale(float s) { return new CornerRadii(tl * s, tr * s, br * s, bl * s); }
    public CornerRadii lerp(CornerRadii o, float t) {
        return new CornerRadii(tl + (o.tl - tl) * t, tr + (o.tr - tr) * t, br + (o.br - br) * t, bl + (o.bl - bl) * t);
    }
}
