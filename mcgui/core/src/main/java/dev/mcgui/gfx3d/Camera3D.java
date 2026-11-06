package dev.mcgui.gfx3d;

import dev.mcgui.math.Mat4;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Vec2;
import dev.mcgui.math.Vec3;

/** Kamera perspektywiczna do projekcji 3D → ekran (podgląd 3D w GUI i debug). */
public final class Camera3D {
    public Vec3 eye = new Vec3(3, 2, 4);
    public Vec3 target = Vec3.ZERO;
    public Vec3 up = Vec3.UP;
    public float fovYRad = (float) Math.toRadians(60);
    public float near = 0.05f, far = 200f;

    public Mat4 view() { return Mat4.lookAt(eye, target, up); }
    public Mat4 projection(float aspect) { return Mat4.perspective(fovYRad, aspect, near, far); }

    /** Orbita wokół celu: yaw/pitch w radianach, odległość. */
    public Camera3D orbit(float yaw, float pitch, float distance) {
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        eye = target.add(new Vec3((float) Math.sin(yaw) * cp * distance, sp * distance, (float) Math.cos(yaw) * cp * distance));
        return this;
    }

    /** Wynik projekcji: pozycja w pikselach widoku oraz głębia NDC (-1..1); null gdy za kamerą. */
    public record Projected(Vec2 screen, float depth) {}

    public Projected project(Vec3 p, Rect viewport) {
        Mat4 vp = projection(viewport.w() / Math.max(1f, viewport.h())).mul(view());
        float[] c = vp.transform(p.x(), p.y(), p.z());
        if (c[3] <= near * 0.5f) return null;                 // za kamerą / zbyt blisko
        float nx = c[0] / c[3], ny = c[1] / c[3], nz = c[2] / c[3];
        return new Projected(new Vec2(viewport.x() + (nx * 0.5f + 0.5f) * viewport.w(),
                                      viewport.y() + (1f - (ny * 0.5f + 0.5f)) * viewport.h()), nz);
    }
}
