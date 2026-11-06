package dev.mcgui.gfx;

import dev.mcgui.math.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Zamiana kształtów 2D na trójkąty (wypełnienie) — dla backendów bez shaderów SDF.
 * Wynik: lista trójkątów jako płaska tablica {x0,y0,x1,y1,x2,y2,...}.
 */
public final class Tessellator {
    private Tessellator() {}

    /** Wypełnienie wielokąta prostego (ear clipping). Działa dla wielokątów wypukłych i wklęsłych bez samoprzecięć. */
    public static float[] fillPolygon(List<Vec2> poly) {
        int n = poly.size();
        if (n < 3) return new float[0];
        List<Integer> idx = new ArrayList<>();
        // orientacja: chcemy CCW (dodatni pole)
        double area = 0;
        for (int i = 0; i < n; i++) { Vec2 a = poly.get(i), b = poly.get((i + 1) % n); area += a.cross(b); }
        for (int i = 0; i < n; i++) idx.add(area > 0 ? i : n - 1 - i);

        List<Float> out = new ArrayList<>();
        int guard = 0;
        while (idx.size() > 3 && guard++ < n * n) {
            boolean clipped = false;
            for (int i = 0; i < idx.size(); i++) {
                int ia = idx.get((i + idx.size() - 1) % idx.size()), ib = idx.get(i), ic = idx.get((i + 1) % idx.size());
                Vec2 a = poly.get(ia), b = poly.get(ib), c = poly.get(ic);
                if (b.sub(a).cross(c.sub(b)) <= 1e-9f) continue;   // wklęsły lub zdegenerowany wierzchołek
                boolean inside = false;
                for (int j : idx) {
                    if (j == ia || j == ib || j == ic) continue;
                    if (pointInTriangle(poly.get(j), a, b, c)) { inside = true; break; }
                }
                if (inside) continue;
                emit(out, a, b, c);
                idx.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) break;   // wielokąt zdegenerowany lub samoprzecinający się
        }
        if (idx.size() == 3) emit(out, poly.get(idx.get(0)), poly.get(idx.get(1)), poly.get(idx.get(2)));
        float[] r = new float[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    /** Obrys łamanej jako trójkąty (pas czworokątów o grubości {@code width}); łączenia ucięte (bevel). */
    public static float[] strokePolyline(List<Vec2> pts, boolean closed, float width) {
        List<Float> out = new ArrayList<>();
        int n = pts.size();
        int segs = closed ? n : n - 1;
        float hw = width / 2f;
        for (int i = 0; i < segs; i++) {
            Vec2 a = pts.get(i), b = pts.get((i + 1) % n);
            Vec2 d = b.sub(a);
            if (d.length() < 1e-6f) continue;
            Vec2 nrm = d.normalized().perp().scale(hw);
            Vec2 a0 = a.add(nrm), a1 = a.sub(nrm), b0 = b.add(nrm), b1 = b.sub(nrm);
            emit(out, a0, a1, b1);
            emit(out, a0, b1, b0);
        }
        float[] r = new float[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    /**
     * Zaokrąglony prostokąt jako wielokąt wypukły (CCW w układzie ekranu: y w dół).
     * {@code arcSteps} = liczba odcinków na ćwiartkę.
     */
    public static List<Vec2> roundedRect(float x, float y, float w, float h, CornerRadii r, int arcSteps) {
        CornerRadii c = r.clampTo(w, h);
        List<Vec2> p = new ArrayList<>();
        arc(p, x + c.tl(), y + c.tl(), c.tl(), 180, 270, arcSteps);
        arc(p, x + w - c.tr(), y + c.tr(), c.tr(), 270, 360, arcSteps);
        arc(p, x + w - c.br(), y + h - c.br(), c.br(), 0, 90, arcSteps);
        arc(p, x + c.bl(), y + h - c.bl(), c.bl(), 90, 180, arcSteps);
        return p;
    }

    private static void arc(List<Vec2> out, float cx, float cy, float r, float fromDeg, float toDeg, int steps) {
        if (r <= 0) { out.add(new Vec2(cx, cy)); return; }
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * i / steps);
            out.add(new Vec2(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r));
        }
    }

    private static void emit(List<Float> out, Vec2 a, Vec2 b, Vec2 c) {
        out.add(a.x()); out.add(a.y()); out.add(b.x()); out.add(b.y()); out.add(c.x()); out.add(c.y());
    }

    private static boolean pointInTriangle(Vec2 p, Vec2 a, Vec2 b, Vec2 c) {
        float d1 = b.sub(a).cross(p.sub(a)), d2 = c.sub(b).cross(p.sub(b)), d3 = a.sub(c).cross(p.sub(c));
        boolean neg = d1 < 0 || d2 < 0 || d3 < 0, pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    /** Pole sumy trójkątów (do testów). */
    public static double area(float[] tris) {
        double s = 0;
        for (int i = 0; i + 5 < tris.length; i += 6)
            s += Math.abs((tris[i + 2] - tris[i]) * (tris[i + 5] - tris[i + 1]) - (tris[i + 4] - tris[i]) * (tris[i + 3] - tris[i + 1])) / 2.0;
        return s;
    }
}
