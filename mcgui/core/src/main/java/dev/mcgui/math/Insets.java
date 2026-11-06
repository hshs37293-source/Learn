package dev.mcgui.math;

public record Insets(float left, float top, float right, float bottom) {
    public static final Insets NONE = new Insets(0, 0, 0, 0);
    public static Insets all(float v) { return new Insets(v, v, v, v); }
    public static Insets xy(float x, float y) { return new Insets(x, y, x, y); }
    public float horizontal() { return left + right; }
    public float vertical()   { return top + bottom; }
}
