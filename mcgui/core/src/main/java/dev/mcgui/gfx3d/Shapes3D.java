package dev.mcgui.gfx3d;

import dev.mcgui.math.Box3;
import dev.mcgui.math.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Generatory geometrii (krawędzie i trójkąty) dla podstawowych brył. Czyste funkcje, testowalne. */
public final class Shapes3D {
    private Shapes3D() {}

    /** 12 krawędzi AABB jako pary punktów. */
    public static List<Vec3[]> boxEdges(Box3 b) {
        int[][] e = { {0,1},{2,3},{4,5},{6,7},  {0,2},{1,3},{4,6},{5,7},  {0,4},{1,5},{2,6},{3,7} };
        List<Vec3[]> out = new ArrayList<>(12);
        for (int[] p : e) out.add(new Vec3[] { b.corner(p[0]), b.corner(p[1]) });
        return out;
    }

    /** 6 ścian AABB jako czworokąty (wierzchołki CCW patrząc z zewnątrz). */
    public static List<Vec3[]> boxFaces(Box3 b) {
        Vec3 c0 = b.corner(0), c1 = b.corner(1), c2 = b.corner(2), c3 = b.corner(3),
             c4 = b.corner(4), c5 = b.corner(5), c6 = b.corner(6), c7 = b.corner(7);
        List<Vec3[]> f = new ArrayList<>(6);
        f.add(new Vec3[] { c0, c2, c3, c1 });   // -Z
        f.add(new Vec3[] { c4, c5, c7, c6 });   // +Z
        f.add(new Vec3[] { c0, c4, c6, c2 });   // -X
        f.add(new Vec3[] { c1, c3, c7, c5 });   // +X
        f.add(new Vec3[] { c0, c1, c5, c4 });   // -Y
        f.add(new Vec3[] { c2, c6, c7, c3 });   // +Y
        return f;
    }

    /** Punkty okręgu w płaszczyźnie o normalnej {@code n}. */
    public static List<Vec3> circlePoints(Vec3 center, Vec3 n, float r, int seg) {
        Vec3 nn = n.normalized();
        Vec3 ref = Math.abs(nn.y()) < 0.99f ? Vec3.UP : new Vec3(1, 0, 0);
        Vec3 u = nn.cross(ref).normalized(), v = nn.cross(u).normalized();
        List<Vec3> pts = new ArrayList<>(seg);
        for (int i = 0; i < seg; i++) {
            double a = 2 * Math.PI * i / seg;
            pts.add(center.add(u.scale((float) Math.cos(a) * r)).add(v.scale((float) Math.sin(a) * r)));
        }
        return pts;
    }

    /** Sfera UV jako trójkąty: {@code seg} długości geograficznych, {@code seg/2} szerokości. */
    public static List<Vec3[]> sphereTriangles(Vec3 c, float r, int seg) {
        int lon = Math.max(3, seg), lat = Math.max(2, seg / 2);
        List<Vec3[]> tris = new ArrayList<>();
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = 2 * Math.PI * j / lon, p1 = 2 * Math.PI * (j + 1) / lon;
                Vec3 a = sph(c, r, t0, p0), b = sph(c, r, t1, p0), d = sph(c, r, t0, p1), e = sph(c, r, t1, p1);
                if (i != 0) tris.add(new Vec3[] { a, b, d });
                if (i != lat - 1) tris.add(new Vec3[] { d, b, e });
            }
        }
        return tris;
    }

    /** Siatka linii na płaszczyźnie XZ o boku {@code size}, co {@code step}. */
    public static List<Vec3[]> gridLines(Vec3 origin, float size, float step) {
        List<Vec3[]> l = new ArrayList<>();
        float h = size / 2;
        int n = Math.max(1, Math.round(size / step));
        for (int i = 0; i <= n; i++) {
            float t = -h + i * (size / n);
            l.add(new Vec3[] { origin.add(new Vec3(t, 0, -h)), origin.add(new Vec3(t, 0, h)) });
            l.add(new Vec3[] { origin.add(new Vec3(-h, 0, t)), origin.add(new Vec3(h, 0, t)) });
        }
        return l;
    }

    /** Strzałka: trzon + grot z dwóch odcinków (płaszczyzna wybierana automatycznie). */
    public static List<Vec3[]> arrowLines(Vec3 from, Vec3 to) {
        Vec3 d = to.sub(from);
        float len = d.length();
        List<Vec3[]> l = new ArrayList<>();
        l.add(new Vec3[] { from, to });
        if (len < 1e-6f) return l;
        Vec3 dir = d.scale(1f / len);
        Vec3 ref = Math.abs(dir.y()) < 0.99f ? Vec3.UP : new Vec3(1, 0, 0);
        Vec3 side = dir.cross(ref).normalized();
        float head = Math.min(len * 0.25f, 0.3f * Math.max(len, 1f));
        Vec3 back = to.sub(dir.scale(head));
        l.add(new Vec3[] { to, back.add(side.scale(head * 0.5f)) });
        l.add(new Vec3[] { to, back.sub(side.scale(head * 0.5f)) });
        return l;
    }

    private static Vec3 sph(Vec3 c, float r, double theta, double phi) {
        return new Vec3(c.x() + r * (float) (Math.sin(theta) * Math.cos(phi)),
                        c.y() + r * (float) Math.cos(theta),
                        c.z() + r * (float) (Math.sin(theta) * Math.sin(phi)));
    }
}
