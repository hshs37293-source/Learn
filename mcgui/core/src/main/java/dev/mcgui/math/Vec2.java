package dev.mcgui.math;

public record Vec2(float x, float y) {
    public static final Vec2 ZERO = new Vec2(0, 0);
    public Vec2 add(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
    public Vec2 sub(Vec2 o) { return new Vec2(x - o.x, y - o.y); }
    public Vec2 scale(float s) { return new Vec2(x * s, y * s); }
    public float length() { return (float) Math.sqrt(x * x + y * y); }
    public float dot(Vec2 o) { return x * o.x + y * o.y; }
    public float cross(Vec2 o) { return x * o.y - y * o.x; }
    public Vec2 normalized() { float l = length(); return l == 0 ? this : new Vec2(x / l, y / l); }
    public Vec2 perp() { return new Vec2(-y, x); }
}
