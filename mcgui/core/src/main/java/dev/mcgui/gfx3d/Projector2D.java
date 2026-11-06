package dev.mcgui.gfx3d;

import dev.mcgui.gfx.Color;
import dev.mcgui.gfx.Path;
import dev.mcgui.gfx.PathStyle;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Vec2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Programowa projekcja sceny 3D na 2D: rysuje {@link Geometry3D} przez {@link UiGraphics}.
 * Działa na każdym backendzie (bez własnych pipeline'ów 3D), więc nadaje się do podglądów
 * w GUI (model, wykres 3D, minimapa). Sortowanie "painter's algorithm" po głębi — bez z-buffera.
 */
public final class Projector2D {
    private Projector2D() {}

    public static void draw(Geometry3D geo, Camera3D cam, Rect viewport, UiGraphics g) {
        record Item(float depth, Runnable draw) {}
        List<Item> items = new ArrayList<>();

        for (Geometry3D.Tri t : geo.triangles) {
            Camera3D.Projected a = cam.project(t.a(), viewport), b = cam.project(t.b(), viewport), c = cam.project(t.c(), viewport);
            if (a == null || b == null || c == null) continue;
            Vec2 pa = a.screen(), pb = b.screen(), pc = c.screen();
            float cross = pb.sub(pa).cross(pc.sub(pa));
            if (Math.abs(cross) < 1e-4f) continue;                // zdegenerowany
            float depth = (a.depth() + b.depth() + c.depth()) / 3f;
            int col = t.argb();
            items.add(new Item(depth, () -> g.path(
                new Path().moveTo(pa.x(), pa.y()).lineTo(pb.x(), pb.y()).lineTo(pc.x(), pc.y()).close(), PathStyle.fill(col))));
        }
        for (Geometry3D.LineSeg l : geo.lines) {
            Camera3D.Projected a = cam.project(l.a(), viewport), b = cam.project(l.b(), viewport);
            if (a == null || b == null) continue;
            float depth = Math.min(a.depth(), b.depth()) - 1e-3f; // linie lekko "przed" wypełnieniem
            items.add(new Item(depth, () -> g.line(a.screen().x(), a.screen().y(), b.screen().x(), b.screen().y(), l.width(), l.argb())));
        }
        // dalekie (duża głębia) najpierw
        items.sort(Comparator.comparingDouble((Item i) -> i.depth()).reversed());
        g.pushClip(viewport, dev.mcgui.gfx.CornerRadii.NONE);
        for (Item i : items) i.draw().run();
        for (Geometry3D.TextItem t : geo.texts) {
            Camera3D.Projected p = cam.project(t.pos(), viewport);
            if (p == null) continue;
            g.text(t.text(), p.screen().x(), p.screen().y(), 0, t.style());
        }
        g.popClip();
    }

    /** Tło/obrys podglądu (pomocnicze). */
    public static void frame(UiGraphics g, Rect viewport, int borderArgb) {
        g.line(viewport.x(), viewport.y(), viewport.right(), viewport.y(), 1, borderArgb);
        g.line(viewport.right(), viewport.y(), viewport.right(), viewport.bottom(), 1, borderArgb);
        g.line(viewport.right(), viewport.bottom(), viewport.x(), viewport.bottom(), 1, borderArgb);
        g.line(viewport.x(), viewport.bottom(), viewport.x(), viewport.y(), 1, borderArgb);
    }

    static int dim(int argb, float f) { return Color.multiplyAlpha(argb, f); }
}
