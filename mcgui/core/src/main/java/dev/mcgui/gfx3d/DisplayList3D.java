package dev.mcgui.gfx3d;

import dev.mcgui.gfx.TextStyle;
import dev.mcgui.math.Box3;
import dev.mcgui.math.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Lista poleceń 3D: nagrywa sceny; odtwarzana na adapterze lub spłaszczana do geometrii. */
public final class DisplayList3D implements Graphics3D {
    private final List<Command3> cmds = new ArrayList<>();
    private int poseDepth;

    public List<Command3> commands() { return List.copyOf(cmds); }
    public int size() { return cmds.size(); }
    public void clear() { cmds.clear(); poseDepth = 0; }
    public boolean isBalanced() { return poseDepth == 0; }
    public void replay(Graphics3D target) { for (Command3 c : cmds) c.replayOn(target); }

    @Override public void line(Vec3 a, Vec3 b, int c, float w) { cmds.add(new Command3.Line(a, b, c, w)); }
    @Override public void polyline(List<Vec3> p, boolean closed, int c, float w) { cmds.add(new Command3.Polyline(List.copyOf(p), closed, c, w)); }
    @Override public void triangle(Vec3 a, Vec3 b, Vec3 c, int argb) { cmds.add(new Command3.Tri(a, b, c, argb)); }
    @Override public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) { cmds.add(new Command3.Quad(a, b, c, d, argb)); }
    @Override public void box(Box3 b, int c, DrawMode3 m) { cmds.add(new Command3.BoxCmd(b, c, m)); }
    @Override public void sphere(Vec3 c, float r, int argb, DrawMode3 m, int seg) { cmds.add(new Command3.Sphere(c, r, argb, m, seg)); }
    @Override public void circle(Vec3 c, Vec3 n, float r, int argb, float w, int seg) { cmds.add(new Command3.Circle(c, n, r, argb, w, seg)); }
    @Override public void arrow(Vec3 f, Vec3 t, int argb, float w) { cmds.add(new Command3.Arrow(f, t, argb, w)); }
    @Override public void grid(Vec3 o, float size, float step, int argb) { cmds.add(new Command3.Grid(o, size, step, argb)); }
    @Override public void text(CharSequence t, Vec3 p, TextStyle s, boolean bb, float sc) { cmds.add(new Command3.Text(t.toString(), p, s, bb, sc)); }
    @Override public void pushPose() { poseDepth++; cmds.add(new Command3.PushPose()); }
    @Override public void popPose() { poseDepth--; cmds.add(new Command3.PopPose()); }
    @Override public void translate(float x, float y, float z) { cmds.add(new Command3.Translate(x, y, z)); }
    @Override public void rotate(float rad, float ax, float ay, float az) { cmds.add(new Command3.Rotate(rad, ax, ay, az)); }
    @Override public void scale(float sx, float sy, float sz) { cmds.add(new Command3.Scale(sx, sy, sz)); }
    @Override public void depthTest(boolean e) { cmds.add(new Command3.Depth(e)); }
}
