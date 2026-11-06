package dev.mcgui.gfx3d;

import dev.mcgui.gfx.TextStyle;
import dev.mcgui.math.Box3;
import dev.mcgui.math.Vec3;

import java.util.List;

/** Polecenie 3D zapisane na liście. */
public sealed interface Command3 {
    record Line(Vec3 a, Vec3 b, int argb, float width) implements Command3 {}
    record Polyline(List<Vec3> pts, boolean closed, int argb, float width) implements Command3 {}
    record Tri(Vec3 a, Vec3 b, Vec3 c, int argb) implements Command3 {}
    record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) implements Command3 {}
    record BoxCmd(Box3 box, int argb, DrawMode3 mode) implements Command3 {}
    record Sphere(Vec3 c, float r, int argb, DrawMode3 mode, int seg) implements Command3 {}
    record Circle(Vec3 c, Vec3 n, float r, int argb, float width, int seg) implements Command3 {}
    record Arrow(Vec3 from, Vec3 to, int argb, float width) implements Command3 {}
    record Grid(Vec3 origin, float size, float step, int argb) implements Command3 {}
    record Text(CharSequence text, Vec3 pos, TextStyle style, boolean billboard, float scale) implements Command3 {}
    record PushPose() implements Command3 {}
    record PopPose() implements Command3 {}
    record Translate(float x, float y, float z) implements Command3 {}
    record Rotate(float rad, float ax, float ay, float az) implements Command3 {}
    record Scale(float sx, float sy, float sz) implements Command3 {}
    record Depth(boolean enabled) implements Command3 {}

    default void replayOn(Graphics3D g) {
        if (this instanceof Line c) g.line(c.a(), c.b(), c.argb(), c.width());
        else if (this instanceof Polyline c) g.polyline(c.pts(), c.closed(), c.argb(), c.width());
        else if (this instanceof Tri c) g.triangle(c.a(), c.b(), c.c(), c.argb());
        else if (this instanceof Quad c) g.quad(c.a(), c.b(), c.c(), c.d(), c.argb());
        else if (this instanceof BoxCmd c) g.box(c.box(), c.argb(), c.mode());
        else if (this instanceof Sphere c) g.sphere(c.c(), c.r(), c.argb(), c.mode(), c.seg());
        else if (this instanceof Circle c) g.circle(c.c(), c.n(), c.r(), c.argb(), c.width(), c.seg());
        else if (this instanceof Arrow c) g.arrow(c.from(), c.to(), c.argb(), c.width());
        else if (this instanceof Grid c) g.grid(c.origin(), c.size(), c.step(), c.argb());
        else if (this instanceof Text c) g.text(c.text(), c.pos(), c.style(), c.billboard(), c.scale());
        else if (this instanceof PushPose) g.pushPose();
        else if (this instanceof PopPose) g.popPose();
        else if (this instanceof Translate c) g.translate(c.x(), c.y(), c.z());
        else if (this instanceof Rotate c) g.rotate(c.rad(), c.ax(), c.ay(), c.az());
        else if (this instanceof Scale c) g.scale(c.sx(), c.sy(), c.sz());
        else if (this instanceof Depth c) g.depthTest(c.enabled());
    }
}
