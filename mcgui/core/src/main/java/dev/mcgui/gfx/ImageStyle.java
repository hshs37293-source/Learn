package dev.mcgui.gfx;

import dev.mcgui.math.Insets;

/** Styl obrazu: tint, filtr i opcjonalny 9-slice (marginesy w pikselach tekstury). */
public record ImageStyle(int tint, Filter filter, Insets nineSlice, float textureWidth, float textureHeight) {
    public enum Filter { NEAREST, LINEAR }
    public static final ImageStyle DEFAULT = new ImageStyle(Color.WHITE, Filter.LINEAR, null, 0, 0);
    public static ImageStyle pixelArt() { return new ImageStyle(Color.WHITE, Filter.NEAREST, null, 0, 0); }
    public static ImageStyle nineSlice(Insets slice, float texW, float texH) {
        return new ImageStyle(Color.WHITE, Filter.NEAREST, slice, texW, texH);
    }
    public boolean isNineSlice() { return nineSlice != null && textureWidth > 0 && textureHeight > 0; }
}
