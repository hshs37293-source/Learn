package dev.mcgui.gfx3d;

import dev.mcgui.gfx.Color;
import dev.mcgui.gfx.TextStyle;
import dev.mcgui.math.Box3;
import dev.mcgui.math.Mat4;
import dev.mcgui.math.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Spłaszcza {@link DisplayList3D} do {@link Geometry3D}, stosując stos transformacji.
 * Dzięki temu backend dostaje tylko odcinki, trójkąty i teksty już w docelowym układzie.
 * Implementuje {@link Graphics3D}, więc można nagrywać wprost bez listy pośredniej.
 */
public final class Flattener3D implements Graphics3D {
    private final Geometry3D out = new Geometry3D();
    private final Deque<Mat4> stack = new ArrayDeque<>();
    private Mat4 pose = Mat4.identity();
    private boolean depth = true;
    private final float boxOutlineWidth;

    public Flattener3D() { this(2f); }
    public Flattener3D(float defaultOutlineWidth) { this.boxOutlineWidth = defaultOutlineWidth; }

    public static Geometry3D flatten(DisplayList3D list) {
        Flattener3D f = new Flattener3D();
        list.replay(f);
        return f.result();
    }

    public Geometry3D result() { return out; }

    private Vec3 p(Vec3 v) { return pose.transformPoint(v); }

    @Override public void line(Vec3 a, Vec3 b, int c, float w) { out.lines.add(new Geometry3D.LineSeg(p(a), p(b), c, w, depth)); }

    @Override public void polyline(List<Vec3> pts, boolean closed, int c, float w) {
        int n = pts.size();
        for (int i = 0; i + 1 < n; i++) line(pts.get(i), pts.get(i + 1), c, w);
        if (closed && n > 2) line(pts.get(n - 1), pts.get(0), c, w);
    }

    @Override public void triangle(Vec3 a, Vec3 b, Vec3 c, int argb) { out.triangles.add(new Geometry3D.Tri(p(a), p(b), p(c), argb, depth)); }

    @Override public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) { triangle(a, b, c, argb); triangle(a, c, d, argb); }

    @Override public void box(Box3 box, int argb, DrawMode3 mode) {
        if (mode == DrawMode3.FILL || mode == DrawMode3.BOTH)
            for (Vec3[] f : Shapes3D.boxFaces(box)) quad(f[0], f[1], f[2], f[3], argb);
        if (mode == DrawMode3.OUTLINE || mode == DrawMode3.BOTH) {
            int lc = mode == DrawMode3.BOTH ? Color.withAlpha(argb, 255) : argb;
            for (Vec3[] e : Shapes3D.boxEdges(box)) line(e[0], e[1], lc, boxOutlineWidth);
        }
    }

    @Override public void sphere(Vec3 center, float r, int argb, DrawMode3 mode, int seg) {
        List<Vec3[]> tris = Shapes3D.sphereTriangles(center, r, seg);
        if (mode == DrawMode3.FILL || mode == DrawMode3.BOTH)
            for (Vec3[] t : tris) triangle(t[0], t[1], t[2], argb);
        if (mode == DrawMode3.OUTLINE || mode == DrawMode3.BOTH)
            for (Vec3[] t : tris) { line(t[0], t[1], argb, 1f); line(t[1], t[2], argb, 1f); }
    }

    @Override public void circle(Vec3 center, Vec3 normal, float r, int argb, float w, int seg) {
        polyline(Shapes3D.circlePoints(center, normal, r, seg), true, argb, w);
    }

    @Override public void arrow(Vec3 from, Vec3 to, int argb, float w) {
        for (Vec3[] l : Shapes3D.arrowLines(from, to)) line(l[0], l[1], argb, w);
    }

    @Override public void grid(Vec3 origin, float size, float step, int argb) {
        for (Vec3[] l : Shapes3D.gridLines(origin, size, step)) line(l[0], l[1], argb, 1f);
    }

    @Override public void text(CharSequence t, Vec3 pos, TextStyle s, boolean bb, float sc) {
        out.texts.add(new Geometry3D.TextItem(t, p(pos), s, bb, sc, depth));
    }

    @Override public void pushPose() { stack.push(pose); }
    @Override public void popPose() {
        if (stack.isEmpty()) throw new IllegalStateException("popPose bez pushPose");
        pose = stack.pop();
    }
    @Override public void translate(float x, float y, float z) { pose = pose.mul(Mat4.translation(x, y, z)); }
    @Override public void rotate(float rad, float ax, float ay, float az) { pose = pose.mul(Mat4.rotation(rad, ax, ay, az)); }
    @Override public void scale(float sx, float sy, float sz) { pose = pose.mul(Mat4.scaling(sx, sy, sz)); }
    @Override public void depthTest(boolean e) { depth = e; }
}
