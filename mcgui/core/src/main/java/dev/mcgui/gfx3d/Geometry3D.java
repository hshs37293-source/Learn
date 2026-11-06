package dev.mcgui.gfx3d;

import dev.mcgui.gfx.TextStyle;
import dev.mcgui.math.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Spłaszczona scena 3D: tylko odcinki, trójkąty i teksty, wszystko w jednym układzie. Format dla backendów. */
public final class Geometry3D {
    public record LineSeg(Vec3 a, Vec3 b, int argb, float width, boolean depth) {}
    public record Tri(Vec3 a, Vec3 b, Vec3 c, int argb, boolean depth) {}
    public record TextItem(CharSequence text, Vec3 pos, TextStyle style, boolean billboard, float scale, boolean depth) {}

    public final List<LineSeg> lines = new ArrayList<>();
    public final List<Tri> triangles = new ArrayList<>();
    public final List<TextItem> texts = new ArrayList<>();

    public boolean isEmpty() { return lines.isEmpty() && triangles.isEmpty() && texts.isEmpty(); }
}
