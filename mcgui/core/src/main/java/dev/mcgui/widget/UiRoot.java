package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.TextStyle;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.input.InputEvent;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.State;
import dev.mcgui.style.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Korzeń drzewa widgetów: trzyma motyw, rozmiar, fokus i stan myszy.
 * Adapter gry woła {@link #render} w fazie rysowania i {@link #dispatch} dla zdarzeń wejścia.
 */
public final class UiRoot {
    private Theme theme;
    private Widget content;
    private Size size = Size.ZERO;
    private boolean layoutDirty = true;
    private Size laidOutFor = Size.ZERO;

    private Widget hovered, focused, captured;
    private float mouseX, mouseY;

    public UiRoot(Theme theme, Widget content) {
        this.theme = theme;
        setContent(content);
    }

    public Theme theme() { return theme; }
    public void setTheme(Theme t) { this.theme = t; invalidateLayout(); }
    public Widget content() { return content; }
    public Size size() { return size; }
    public Widget focused() { return focused; }
    public Widget hovered() { return hovered; }

    public void setContent(Widget w) {
        if (content != null) content.attach(null);
        content = w;
        content.attach(this);
        invalidateLayout();
    }

    public void setSize(float w, float h) {
        if (size.w() != w || size.h() != h) { size = new Size(w, h); invalidateLayout(); }
    }

    public void invalidateLayout() { layoutDirty = true; }

    /** Wymusza układ teraz (np. w testach). */
    public void layout(TextMeasurer tm) {
        content.layoutMeasure(size, tm);
        content.arrange(new Rect(0, 0, size.w(), size.h()));
        layoutDirty = false;
        laidOutFor = size;
    }

    /** Faza rysowania: układ (jeśli trzeba) i polecenia rysowania całego drzewa. */
    public void render(UiGraphics g) {
        if (layoutDirty || !size.equals(laidOutFor)) layout(g);
        content.draw(g);
        drawTooltip(g);
    }

    private void drawTooltip(UiGraphics g) {
        Widget w = hovered;
        while (w != null && w.tooltip == null) w = w.parent();
        if (w == null || w.tooltip == null) return;
        TextStyle ts = TextStyle.DEFAULT.withShadow(false);
        Size s = g.measure(w.tooltip, ts);
        float x = Math.min(mouseX + 8, size.w() - s.w() - 8), y = Math.min(mouseY + 12, size.h() - s.h() - 6);
        g.box(new Rect(x - 3, y - 2, s.w() + 6, s.h() + 4),
            dev.mcgui.gfx.BoxStyle.builder().fill(0xE6101018).radius(3).border(1, 0x55FFFFFF).build());
        g.text(w.tooltip, x, y, ts);
    }

    // ---------- wejście ----------

    /** Przekazuje zdarzenie do drzewa; zwraca true, jeśli zostało zużyte. */
    public boolean dispatch(InputEvent e) {
        if (e instanceof InputEvent.MouseMove m) return onMove(m);
        if (e instanceof InputEvent.MouseDown d) return onDown(d);
        if (e instanceof InputEvent.MouseUp u) return onUp(u);
        if (e instanceof InputEvent.Scroll s) return bubble(content.hitTest(s.x(), s.y()), s);
        if (e instanceof InputEvent.KeyDown k) return onKey(k);
        if (e instanceof InputEvent.CharTyped c) return focused != null && focused.isEnabled() && focused.onEvent(c);
        return false;
    }

    private boolean onMove(InputEvent.MouseMove m) {
        mouseX = m.x(); mouseY = m.y();
        Widget hit = content.hitTest(m.x(), m.y());
        setHover(hit);
        if (captured != null) return captured.onEvent(m);
        return hit != null && hit.isEnabled() && bubble(hit, m);
    }

    private boolean onDown(InputEvent.MouseDown d) {
        Widget hit = content.hitTest(d.x(), d.y());
        // fokus: najbliższy fokusowalny przodek
        Widget f = hit;
        while (f != null && !f.isFocusable()) f = f.parent();
        setFocus(f != null && f.isEnabled() ? f : null);
        for (Widget w = hit; w != null; w = w.parent()) {
            if (!w.isEnabled()) return true;               // wyłączony blokuje kliknięcie
            if (w.onEvent(d)) {
                captured = w;
                w.setState(State.PRESSED, true);
                return true;
            }
        }
        return false;
    }

    private boolean onUp(InputEvent.MouseUp u) {
        Widget c = captured;
        captured = null;
        if (c == null) return false;
        c.setState(State.PRESSED, false);
        c.onEvent(u);
        return true;
    }

    private boolean onKey(InputEvent.KeyDown k) {
        if (focused != null && focused.isEnabled() && focused.onEvent(k)) return true;
        if (k.key() == InputEvent.KEY_TAB) { moveFocus((k.mods() & InputEvent.MOD_SHIFT) == 0 ? 1 : -1); return true; }
        return false;
    }

    private boolean bubble(Widget from, InputEvent e) {
        for (Widget w = from; w != null; w = w.parent()) if (w.isEnabled() && w.onEvent(e)) return true;
        return false;
    }

    private void setHover(Widget hit) {
        if (hit == hovered) return;
        for (Widget w = hovered; w != null; w = w.parent()) w.setState(State.HOVER, false);
        hovered = hit;
        for (Widget w = hovered; w != null; w = w.parent()) w.setState(State.HOVER, true);
    }

    public void setFocus(Widget w) {
        if (w == focused) return;
        if (focused != null) { focused.setState(State.FOCUSED, false); focused.onFocusChanged(false); }
        focused = w;
        if (focused != null) { focused.setState(State.FOCUSED, true); focused.onFocusChanged(true); }
    }

    private void moveFocus(int dir) {
        List<Widget> order = new ArrayList<>();
        collectFocusable(content, order);
        if (order.isEmpty()) return;
        int i = order.indexOf(focused);
        setFocus(order.get(((i < 0 ? (dir > 0 ? -1 : 0) : i) + dir + order.size()) % order.size()));
    }

    private void collectFocusable(Widget w, List<Widget> out) {
        if (!w.visible) return;
        if (w.isFocusable() && w.isEnabled()) out.add(w);
        for (Widget c : w.children()) collectFocusable(c, out);
    }
}
