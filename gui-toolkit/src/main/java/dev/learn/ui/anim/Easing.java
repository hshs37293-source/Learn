package dev.learn.ui.anim;

public final class Easing {
    private Easing() {}
    public static float clamp01(float t) { return Math.max(0f, Math.min(1f, t)); }
    public static float outCubic(float t) { t = clamp01(t); float u = 1 - t; return 1 - u * u * u; }
    public static float inOutCubic(float t) {
        t = clamp01(t);
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }
}
