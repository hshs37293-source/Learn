package dev.learn.ui.widget;

import dev.learn.ui.draw.DrawList;
import dev.learn.ui.draw.UiBackend;
import dev.learn.ui.theme.Theme;

/** Korzeń GUI: łączy wejście, layout i rysowanie. Jedyny punkt styku z backendem. */
public final class Screen {
    public final Widget root = new Stack(Stack.Dir.COLUMN);
    public Theme theme = Theme.dark();
    private Widget focus;

    public Screen() { root.x = 0; root.y = 0; }

    /** Jedna klatka: layout + zapis poleceń + wykonanie przez backend. */
    public void frame(UiBackend backend, DrawList scratch) {
        scratch.clear();
        root.layout(backend, theme);
        root.draw(scratch, theme);
        backend.execute(scratch.commands());
    }

    public boolean mouseDown(float x, float y, int b) {
        focus = root.hitTest(x, y);
        for (Widget w = focus; w != null; w = w.parent) if (w.onMouseDown(x, y, b)) return true;
        return false;
    }
    public boolean mouseUp(float x, float y, int b) {
        Widget f = focus;
        for (Widget w = f; w != null; w = w.parent) if (w.onMouseUp(x, y, b)) return true;
        return false;
    }
    public void mouseMove(float x, float y) { moveRec(root, x, y); }
    private void moveRec(Widget w, float x, float y) {
        w.onMouseMove(x, y);
        for (Widget c : w.children()) moveRec(c, x, y);
    }
}
