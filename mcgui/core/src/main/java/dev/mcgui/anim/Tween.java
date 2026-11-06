package dev.mcgui.anim;

/** Animacja liczby od {@code from} do {@code to} w czasie rzeczywistym (ms). */
public final class Tween {
    private float from, to;
    private long start;
    private int durationMs;
    private Easing easing = Easing.EASE;

    public Tween(float initial) { this.from = this.to = initial; }

    /** Zaczyna nowe przejście od bieżącej wartości do {@code target}. */
    public void animateTo(float target, long now, int durationMs, Easing easing) {
        this.from = value(now);
        this.to = target;
        this.start = now;
        this.durationMs = Math.max(0, durationMs);
        this.easing = easing;
    }

    /** Ustawia wartość natychmiast (bez animacji). */
    public void jump(float v) { from = to = v; durationMs = 0; }

    public float target() { return to; }

    public float value(long now) {
        if (durationMs <= 0 || now >= start + durationMs) return to;
        float t = (now - start) / (float) durationMs;
        return from + (to - from) * easing.apply(t);
    }

    public boolean isRunning(long now) { return durationMs > 0 && now < start + durationMs; }
}
