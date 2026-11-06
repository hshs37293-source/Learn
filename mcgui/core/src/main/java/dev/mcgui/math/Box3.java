package dev.mcgui.math;

/** Prostopadłościan osiowy (AABB). */
public record Box3(Vec3 min, Vec3 max) {
    public static Box3 of(float x0, float y0, float z0, float x1, float y1, float z1) {
        return new Box3(new Vec3(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1)),
                        new Vec3(Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1)));
    }
    public static Box3 centered(Vec3 c, float sx, float sy, float sz) {
        return of(c.x() - sx / 2, c.y() - sy / 2, c.z() - sz / 2, c.x() + sx / 2, c.y() + sy / 2, c.z() + sz / 2);
    }
    public Vec3 center() { return min.lerp(max, 0.5f); }
    public Vec3 size() { return max.sub(min); }
    public Box3 inflate(float d) {
        return new Box3(new Vec3(min.x() - d, min.y() - d, min.z() - d), new Vec3(max.x() + d, max.y() + d, max.z() + d));
    }
    /** 8 narożników: bit0 = x, bit1 = y, bit2 = z (0 = min, 1 = max). */
    public Vec3 corner(int i) {
        return new Vec3((i & 1) == 0 ? min.x() : max.x(), (i & 2) == 0 ? min.y() : max.y(), (i & 4) == 0 ? min.z() : max.z());
    }
}
