package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.gfx3d.Camera3D;
import dev.mcgui.gfx3d.DisplayList3D;
import dev.mcgui.gfx3d.Flattener3D;
import dev.mcgui.gfx3d.Geometry3D;
import dev.mcgui.gfx3d.Graphics3D;
import dev.mcgui.gfx3d.Projector2D;
import dev.mcgui.input.InputEvent;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;

import java.util.function.Consumer;

/**
 * Podgląd sceny 3D w GUI (model, wykres, minimapa). Scenę opisujesz przez {@link Graphics3D};
 * rysowana jest programową projekcją przez {@link UiGraphics}, więc działa na każdym backendzie.
 * Przeciągnięcie myszą obraca kamerę (orbita), kółko zmienia odległość.
 */
public class Viewport3D extends Widget {
    private final Consumer<Graphics3D> scene;
    private final Camera3D camera = new Camera3D();
    private float yaw = 0.6f, pitch = 0.45f, distance = 6f;
    private boolean dragging;
    private float lastX, lastY;
    public boolean autoRotate = false;

    public Viewport3D(Consumer<Graphics3D> scene) {
        super("viewport");
        this.scene = scene;
        this.prefWidth = 160f;
        this.prefHeight = 120f;
    }

    public Camera3D camera() { return camera; }
    public void setOrbit(float yaw, float pitch, float distance) { this.yaw = yaw; this.pitch = pitch; this.distance = distance; }

    @Override public Size measure(Size avail, TextMeasurer tm) { return applyPref(new Size(160, 120), layoutStyle()); }

    @Override protected void drawSelf(UiGraphics g) {
        long now = g.nowMillis();
        ResolvedStyle st = style(now);
        g.box(bounds, st.box());
        float y = autoRotate ? yaw + (now % 20000) / 20000f * (float) (2 * Math.PI) : yaw;
        camera.orbit(y, pitch, distance);
        DisplayList3D list = new DisplayList3D();
        scene.accept(list);
        Geometry3D geo = Flattener3D.flatten(list);
        Projector2D.draw(geo, camera, bounds.inset(1), g);
    }

    @Override public boolean onEvent(InputEvent e) {
        if (e instanceof InputEvent.MouseDown d && d.button() == InputEvent.BUTTON_LEFT) { dragging = true; lastX = d.x(); lastY = d.y(); return true; }
        if (e instanceof InputEvent.MouseMove m && dragging) {
            yaw -= (m.x() - lastX) * 0.01f;
            pitch = clamp(pitch + (m.y() - lastY) * 0.01f, -1.5f, 1.5f);
            lastX = m.x(); lastY = m.y();
            return true;
        }
        if (e instanceof InputEvent.MouseUp) { dragging = false; return true; }
        if (e instanceof InputEvent.Scroll s && bounds.contains(s.x(), s.y())) {
            distance = clamp(distance - s.dy() * 0.5f, 1.5f, 40f);
            return true;
        }
        return false;
    }
}
