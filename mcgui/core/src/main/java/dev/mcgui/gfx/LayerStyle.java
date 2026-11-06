package dev.mcgui.gfx;

/** Warstwa kompozycji: przezroczystość, rozmycie tła (szkło), tryb mieszania. */
public record LayerStyle(float alpha, float backdropBlur, Blend blend) {
    public enum Blend { NORMAL, ADD, MULTIPLY }
    public static final LayerStyle IDENTITY = new LayerStyle(1f, 0f, Blend.NORMAL);
    public static LayerStyle alpha(float a) { return new LayerStyle(a, 0f, Blend.NORMAL); }
    public static LayerStyle glass(float blurRadius) { return new LayerStyle(1f, blurRadius, Blend.NORMAL); }
    public boolean isIdentity() { return alpha >= 1f && backdropBlur <= 0f && blend == Blend.NORMAL; }
}
