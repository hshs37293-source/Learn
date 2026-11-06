package dev.learn.ui.math;

public record Vec3(float x, float y, float z) {
    public Vec3 add(Vec3 o) { return new Vec3(x + o.x(), y + o.y(), z + o.z()); }
    public Vec3 sub(Vec3 o) { return new Vec3(x - o.x(), y - o.y(), z - o.z()); }
    public Vec3 mul(float s) { return new Vec3(x * s, y * s, z * s); }
    public float dot(Vec3 o) { return x * o.x() + y * o.y() + z * o.z(); }
    public Vec3 cross(Vec3 o) { return new Vec3(y * o.z() - z * o.y(), z * o.x() - x * o.z(), x * o.y() - y * o.x()); }
    public float length() { return (float) Math.sqrt(dot(this)); }
    public Vec3 normalize() { float l = length(); return new Vec3(x / l, y / l, z / l); }
}
