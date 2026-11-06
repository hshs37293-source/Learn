package dev.mcgui.gfx3d;

import dev.mcgui.gfx.TextStyle;
import dev.mcgui.math.Box3;
import dev.mcgui.math.Vec3;

import java.util.List;

/**
 * Kontrakt rysowania 3D (świat gry lub podgląd w GUI). Tak jak {@link dev.mcgui.gfx.UiGraphics}
 * nie zawiera typów Minecrafta. Współrzędne: w świecie gry zwykle względem kamery lub bezwzględne
 * (decyduje adapter). Polecenia, nie natychmiastowe rysowanie.
 */
public interface Graphics3D {
    void line(Vec3 a, Vec3 b, int argb, float width);
    void polyline(List<Vec3> points, boolean closed, int argb, float width);
    void triangle(Vec3 a, Vec3 b, Vec3 c, int argb);
    void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb);
    void box(Box3 box, int argb, DrawMode3 mode);
    void sphere(Vec3 center, float radius, int argb, DrawMode3 mode, int segments);
    void circle(Vec3 center, Vec3 normal, float radius, int argb, float width, int segments);
    void arrow(Vec3 from, Vec3 to, int argb, float width);
    void grid(Vec3 origin, float size, float step, int argb);
    /** Tekst w świecie; {@code billboard} = zawsze zwrócony do kamery; {@code scale} = rozmiar w jednostkach świata na jednostkę fontu. */
    void text(CharSequence text, Vec3 pos, TextStyle style, boolean billboard, float scale);

    // --- poza (transformacja) ---
    void pushPose();
    void popPose();
    void translate(float x, float y, float z);
    void rotate(float radians, float axisX, float axisY, float axisZ);
    void scale(float sx, float sy, float sz);

    /** Czy kolejne elementy są testowane względem głębi (false = widoczne przez ściany). */
    void depthTest(boolean enabled);
}
