package dev.learn.ui.math;

/** Promień do klikania w elementy 3D (picking) i w panele w świecie. */
public record Ray(Vec3 origin, Vec3 dir) {

    /** Promień spod kursora (piksele okna) przez odwróconą macierz viewProjection. */
    public static Ray fromScreen(float px, float py, float w, float h, Mat4 viewProj) {
        Mat4 inv = viewProj.inverse();
        float nx = 2 * px / w - 1, ny = 1 - 2 * py / h;
        Vec3 near = inv.transformPoint(new Vec3(nx, ny, -1));
        Vec3 far = inv.transformPoint(new Vec3(nx, ny, 1));
        return new Ray(near, far.sub(near).normalize());
    }

    /**
     * Przecięcie z prostokątnym panelem w przestrzeni: środek, osie right/up (jednostkowe),
     * rozmiar w jednostkach świata. Zwraca współrzędne panelu 0..1 (u,v; v w dół) lub null.
     */
    public float[] hitPanel(Vec3 center, Vec3 right, Vec3 up, float width, float height) {
        Vec3 n = right.cross(up);
        float denom = n.dot(dir);
        if (Math.abs(denom) < 1e-6f) return null;
        float t = center.sub(origin).dot(n) / denom;
        if (t < 0) return null;
        Vec3 d = origin.add(dir.mul(t)).sub(center);
        float u = d.dot(right) / width + 0.5f;
        float v = 0.5f - d.dot(up) / height;
        return (u < 0 || u > 1 || v < 0 || v > 1) ? null : new float[]{u, v};
    }
}
