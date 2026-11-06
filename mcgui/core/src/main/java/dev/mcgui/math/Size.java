package dev.mcgui.math;

public record Size(float w, float h) {
    public static final Size ZERO = new Size(0, 0);
    public static Size of(float w, float h) { return new Size(w, h); }
    public Size max(Size o) { return new Size(Math.max(w, o.w), Math.max(h, o.h)); }
    public Size clamp(Size min, Size max) {
        return new Size(Math.min(max.w, Math.max(min.w, w)), Math.min(max.h, Math.max(min.h, h)));
    }
}
