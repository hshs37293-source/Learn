package dev.mcgui.gfx;

/** Styl ścieżki: wypełnienie i/lub obrys. */
public record PathStyle(Paint fill, int strokeColor, float strokeWidth) {
    public static PathStyle fill(int argb) { return new PathStyle(Paint.solid(argb), 0, 0); }
    public static PathStyle fill(Paint p) { return new PathStyle(p, 0, 0); }
    public static PathStyle stroke(int argb, float width) { return new PathStyle(Paint.NONE, argb, width); }
    public static PathStyle both(Paint fill, int strokeColor, float strokeWidth) { return new PathStyle(fill, strokeColor, strokeWidth); }
    public boolean hasStroke() { return strokeWidth > 0 && Color.a(strokeColor) > 0; }
}
