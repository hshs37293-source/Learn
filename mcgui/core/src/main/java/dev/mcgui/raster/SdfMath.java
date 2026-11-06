package dev.mcgui.raster;

import dev.mcgui.gfx.CornerRadii;

/**
 * Odpowiednik CPU matematyki z shaderów {@code sdf_box.fsh} (te same wzory, ta sama konwencja osi: y w dół).
 * Służy do testów poprawności wzorów (pokrycie pikseli, obrys, cień) bez GPU oraz jako dokumentacja shadera.
 */
public final class SdfMath {
    private SdfMath() {}

    /** Odległość ze znakiem do zaokrąglonego prostokąta (d &lt; 0 wewnątrz). p względem środka, hsX/hsY = połowa rozmiaru. */
    public static float distance(float px, float py, float hsX, float hsY, CornerRadii r) {
        float rr = (px > 0) ? ((py > 0) ? r.br() : r.tr()) : ((py > 0) ? r.bl() : r.tl());
        float qx = Math.abs(px) - hsX + rr, qy = Math.abs(py) - hsY + rr;
        float outside = (float) Math.sqrt(Math.max(qx, 0) * Math.max(qx, 0) + Math.max(qy, 0) * Math.max(qy, 0));
        return Math.min(Math.max(qx, qy), 0) + outside - rr;
    }

    /** Pokrycie piksela dla wypełnienia: {@code clamp(0.5 - d/aa)}; aa ≈ fwidth(d) ≈ 1 dla pola odległości w pikselach. */
    public static float fillCoverage(float d, float aa) { return clamp(0.5f - d / Math.max(aa, 1e-4f), 0f, 1f); }

    /** Pokrycie obrysu o grubości {@code line} (wnętrze odcięte). */
    public static float outlineCoverage(float d, float line, float aa) {
        return fillCoverage(d, aa) * clamp(0.5f + (d + line) / Math.max(aa, 1e-4f), 0f, 1f);
    }

    /** Przezroczystość cienia o promieniu rozmycia {@code blur}: 1 − smoothstep(−blur/2, blur/2, d). */
    public static float shadowCoverage(float d, float blur) {
        float t = clamp((d + 0.5f * blur) / Math.max(blur, 1e-4f), 0f, 1f);
        return 1f - t * t * (3f - 2f * t);
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
