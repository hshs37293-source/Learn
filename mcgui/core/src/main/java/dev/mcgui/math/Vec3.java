package dev.mcgui.math;

/** Wektor 3D. W integracji z Minecraftem: współrzędne świata lub względem kamery. */
public record Vec3(float x, float y, float z) {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public static final Vec3 UP = new Vec3(0, 1, 0);
    public Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
    public Vec3 sub(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
    public Vec3 scale(float s) { return new Vec3(x * s, y * s, z * s); }
    public float dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }
    public float length() { return (float) Math.sqrt(x * x + y * y + z * z); }
    public Vec3 normalized() { float l = length(); return l == 0 ? this : scale(1f / l); }
    public Vec3 lerp(Vec3 o, float t) { return new Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t); }
}
