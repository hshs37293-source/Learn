package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.input.InputEvent;
import dev.mcgui.layout.Align;
import dev.mcgui.math.Insets;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;
import dev.mcgui.style.State;
import dev.mcgui.style.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Bazowy widget. Zna tylko swoją ROLĘ (np. "button.primary"); wygląd pochodzi z motywu.
 * Cykl: {@link #measure} → {@link #arrange} → {@link #draw}. Współrzędne po {@link #arrange} są bezwzględne.
 */
public abstract class Widget {
    public String role;
    public Insets margin = Insets.NONE;
    public Float prefWidth, prefHeight;
    public float weight = 0f;                 // udział w wolnej przestrzeni w kontenerze liniowym
    public Align align = null;                // null = domyślne wyrównanie kontenera
    public boolean visible = true;
    public String tooltip;

    protected Rect bounds = Rect.ZERO;
    protected Widget parent;
    protected UiRoot root;
    protected final EnumSet<State> states = EnumSet.noneOf(State.class);
    private final List<Widget> children = new ArrayList<>();

    // przejścia stylu
    private ResolvedStyle styleFrom, styleTarget;
    private long styleStart;
    private Theme styleTheme;

    protected Widget(String role) { this.role = role; }

    // ---------- drzewo ----------
    public List<Widget> children() { return Collections.unmodifiableList(children); }
    public Widget parent() { return parent; }
    public Rect bounds() { return bounds; }
    public boolean isFocusable() { return false; }

    protected void addChild(Widget w) {
        w.parent = this;
        w.attach(root);
        children.add(w);
        invalidateLayout();
    }
    protected void removeChild(Widget w) {
        if (children.remove(w)) { w.parent = null; w.attach(null); invalidateLayout(); }
    }
    protected void clearChildren() {
        for (Widget c : children) { c.parent = null; c.attach(null); }
        children.clear();
        invalidateLayout();
    }
    void attach(UiRoot r) {
        this.root = r;
        for (Widget c : children) c.attach(r);
    }
    public void invalidateLayout() { if (root != null) root.invalidateLayout(); }

    // ---------- stany ----------
    public Set<State> states() { return states; }
    public void setState(State s, boolean on) { if (on) states.add(s); else states.remove(s); }
    public boolean hasState(State s) { return states.contains(s); }
    private boolean enabled = true;
    public boolean isEnabled() { return enabled && (parent == null || parent.isEnabled()); }
    public void setEnabled(boolean e) { this.enabled = e; setState(State.DISABLED, !e); }

    // ---------- styl ----------
    protected Theme theme() { return root == null ? null : root.theme(); }

    /** Bieżący styl wizualny (z płynnym przejściem między stanami). */
    protected ResolvedStyle style(long now) {
        Theme th = theme();
        if (th == null) throw new IllegalStateException("Widget nie jest podłączony do UiRoot");
        ResolvedStyle target = th.resolve(role, states);
        if (styleTarget == null || th != styleTheme) {
            // pierwszy rysunek lub zmiana motywu: przełącz od razu, bez animacji ze starego stylu
            styleTheme = th;
            styleFrom = styleTarget = target;
            styleStart = now;
        }
        if (target != styleTarget) {
            styleFrom = visual(now);
            styleTarget = target;
            styleStart = now;
        }
        return visual(now);
    }

    private ResolvedStyle visual(long now) {
        int dur = styleTarget.transitionMs();
        if (dur <= 0 || styleFrom == styleTarget) return styleTarget;
        float t = (now - styleStart) / (float) dur;
        return styleFrom.lerp(styleTarget, styleTarget.easing().apply(t));
    }

    // ---------- layout ----------
    /** Preferowany rozmiar (z marginesami NIE, ale z paddingiem stylu tak). */
    public abstract Size measure(Size available, TextMeasurer tm);

    private Size desired = Size.ZERO;

    /** Mierzy widget i zapamiętuje wynik; kontenery używają tego w {@link #arrange}. */
    public final Size layoutMeasure(Size available, TextMeasurer tm) {
        desired = measure(available, tm);
        return desired;
    }

    /** Rozmiar z ostatniego {@link #layoutMeasure}. */
    public final Size desired() { return desired; }

    protected Size applyPref(Size content, ResolvedStyle st) {
        float w = prefWidth != null ? prefWidth : Math.max(content.w(), st.minWidth());
        float h = prefHeight != null ? prefHeight : Math.max(content.h(), st.minHeight());
        return new Size(w, h);
    }

    public void arrange(Rect finalBounds) { this.bounds = finalBounds; }

    protected ResolvedStyle layoutStyle() {
        Theme th = theme();
        return th == null ? null : th.resolve(role, states);
    }

    // ---------- rysowanie ----------
    public final void draw(UiGraphics g) {
        if (!visible) return;
        drawSelf(g);
        drawChildren(g);
        drawOverlay(g);
    }
    protected abstract void drawSelf(UiGraphics g);
    protected void drawChildren(UiGraphics g) { for (Widget c : children) c.draw(g); }
    protected void drawOverlay(UiGraphics g) {}

    // ---------- wejście ----------
    /** Zwraca true, jeśli zdarzenie zostało zużyte. */
    public boolean onEvent(InputEvent e) { return false; }
    public void onFocusChanged(boolean focused) {}

    /** Najgłębszy widżet pod punktem (od góry: ostatnie dziecko wygrywa). */
    public Widget hitTest(float x, float y) {
        if (!visible || !bounds.contains(x, y)) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget h = children.get(i).hitTest(x, y);
            if (h != null) return h;
        }
        return this;
    }

    // ---------- pomocnicze ----------
    protected static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
