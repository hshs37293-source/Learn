package dev.mcgui.gfx3d;

import dev.mcgui.math.Vec3;

/**
 * Zamiana odcinków 3D na wstęgi (quady zwrócone do kamery). Pozwala rysować grube linie
 * w świecie gry używając wyłącznie potoku wypełnionych trójkątów — bez osobnego pipeline'u linii.
 */
public final class Ribbons {
    private Ribbons() {}

    /** Szerokość w jednostkach świata, która daje {@code pixels} pikseli na ekranie w danej odległości. */
    public static float worldWidth(float pixels, float distance, float fovYRad, float screenHeightPx) {
        return pixels * 2f * (float) Math.tan(fovYRad / 2f) * Math.max(distance, 0.01f) / Math.max(1f, screenHeightPx);
    }

    /** Cztery narożniki wstęgi (a-, a+, b+, b-) dla odcinka a→b widzianego z kamery {@code cam}. */
    public static Vec3[] lineQuad(Vec3 a, Vec3 b, Vec3 cam, float widthWorld) {
        Vec3 dir = b.sub(a);
        Vec3 mid = a.lerp(b, 0.5f);
        Vec3 toCam = cam.sub(mid);
        Vec3 side = dir.cross(toCam);
        if (side.length() < 1e-9f) side = dir.cross(Vec3.UP.length() > 0 ? Vec3.UP : new Vec3(1, 0, 0));
        if (side.length() < 1e-9f) side = dir.cross(new Vec3(1, 0, 0));
        side = side.normalized().scale(widthWorld / 2f);
        return new Vec3[] { a.sub(side), a.add(side), b.add(side), b.sub(side) };
    }

    /** Zwraca nową geometrię, w której odcinki zastąpiono wstęgami (trójkąty); teksty i trójkąty przenosi bez zmian. */
    public static Geometry3D linesToTriangles(Geometry3D geo, Vec3 cam, float fovYRad, float screenHeightPx) {
        Geometry3D out = new Geometry3D();
        out.triangles.addAll(geo.triangles);
        out.texts.addAll(geo.texts);
        for (Geometry3D.LineSeg l : geo.lines) {
            float dist = l.a().lerp(l.b(), 0.5f).sub(cam).length();
            Vec3[] q = lineQuad(l.a(), l.b(), cam, worldWidth(l.width(), dist, fovYRad, screenHeightPx));
            out.triangles.add(new Geometry3D.Tri(q[0], q[1], q[2], l.argb(), l.depth()));
            out.triangles.add(new Geometry3D.Tri(q[0], q[2], q[3], l.argb(), l.depth()));
        }
        return out;
    }
}
