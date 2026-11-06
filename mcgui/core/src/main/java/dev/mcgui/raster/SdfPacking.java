package dev.mcgui.raster;

import dev.mcgui.gfx.CornerRadii;

/**
 * Pakowanie parametrów elementu SDF w atrybuty wierzchołka (ivec2 po 2×16 bitów ze znakiem, jak UV1/UV2 w vanilli).
 * Ten sam kod koduje po stronie Javy i jest odzwierciedlony w shaderach (patrz sdf_box.vsh).
 *
 * Rozmiar: jednostka 0.25 px, zakres do 32767 (≈ 8191 px).
 * Promienie: 7 bitów każdy, jednostka 0.5 px (0..63.5 px), po dwa w jednym kanale.
 */
public final class SdfPacking {
    private SdfPacking() {}

    public static final float SIZE_UNIT = 0.25f;
    public static final float RADIUS_UNIT = 0.5f;
    public static final int MAX_RADIUS_STEPS = 127;

    public static int encodeSize(float px) { return Math.max(0, Math.min(32767, Math.round(px / SIZE_UNIT))); }
    public static float decodeSize(int v) { return v * SIZE_UNIT; }

    public static int encodeRadius(float px) { return Math.max(0, Math.min(MAX_RADIUS_STEPS, Math.round(px / RADIUS_UNIT))); }
    public static float decodeRadius(int v) { return v * RADIUS_UNIT; }

    /** Kanał x: tl | tr << 7. */
    public static int radiiX(CornerRadii r) { return encodeRadius(r.tl()) | (encodeRadius(r.tr()) << 7); }
    /** Kanał y: br | bl << 7. */
    public static int radiiY(CornerRadii r) { return encodeRadius(r.br()) | (encodeRadius(r.bl()) << 7); }

    public static CornerRadii unpackRadii(int x, int y) {
        return new CornerRadii(decodeRadius(x & 127), decodeRadius((x >> 7) & 127),
                               decodeRadius(y & 127), decodeRadius((y >> 7) & 127));
    }
}
