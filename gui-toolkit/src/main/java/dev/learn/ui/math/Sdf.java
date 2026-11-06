package dev.learn.ui.math;

/** Wzorcowa (CPU) wersja matematyki SDF; shader GLSL ma liczyć dokładnie to samo. */
public final class Sdf {
    private Sdf() {}

    /** Odległość ze znakiem do zaokrąglonego prostokąta o środku w (0,0). Ujemna = wewnątrz. */
    public static float roundedBox(float px, float py, float halfW, float halfH, float radius) {
        float r = Math.min(radius, Math.min(halfW, halfH));
        float qx = Math.abs(px) - (halfW - r);
        float qy = Math.abs(py) - (halfH - r);
        float outside = (float) Math.hypot(Math.max(qx, 0), Math.max(qy, 0));
        return outside + Math.min(Math.max(qx, qy), 0) - r;
    }

    /** Pokrycie piksela 0..1 z antyaliasingiem o szerokości ok. 1 px. */
    public static float coverage(float dist) {
        return Math.max(0f, Math.min(1f, 0.5f - dist));
    }
}
