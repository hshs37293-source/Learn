package dev.learn.ui.widget;

import dev.learn.ui.draw.DrawList;
import dev.learn.ui.theme.Theme;
import java.util.ArrayList;
import java.util.List;

/** Węzeł drzewa GUI. Pozycje dzieci są względem rodzica. */
public abstract class Widget {
    public float x, y, w, h;
    public boolean visible = true;
    public Widget parent;
    protected final List<Widget> children = new ArrayList<>();

    public <T extends Widget> T add(T child) { child.parent = this; children.add(child); return child; }
    public List<Widget> children() { return children; }

    public float absX() { return parent == null ? x : parent.absX() + x; }
    public float absY() { return parent == null ? y : parent.absY() + y; }

    public boolean contains(float px, float py) {
        float ax = absX(), ay = absY();
        return px >= ax && px < ax + w && py >= ay && py < ay + h;
    }

    /** Najgłębszy widoczny widget pod kursorem (ostatnie dziecko = na wierzchu). */
    public Widget hitTest(float px, float py) {
        if (!visible || !contains(px, py)) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget hit = children.get(i).hitTest(px, py);
            if (hit != null) return hit;
        }
        return this;
    }

    public void layout(dev.learn.ui.draw.UiBackend backend, Theme theme) {
        for (Widget c : children) c.layout(backend, theme);
    }

    public void draw(DrawList out, Theme theme) {
        if (!visible) return;
        drawSelf(out, theme);
        for (Widget c : children) c.draw(out, theme);
    }

    protected void drawSelf(DrawList out, Theme theme) {}

    // zdarzenia: zwróć true, jeśli obsłużone
    public boolean onMouseDown(float px, float py, int button) { return false; }
    public boolean onMouseUp(float px, float py, int button) { return false; }
    public boolean onMouseMove(float px, float py) { return false; }
    public boolean onScroll(float px, float py, float delta) { return false; }
    public boolean onKey(int key, int mods) { return false; }
    public boolean onChar(char c) { return false; }
}
