package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.TextStyle;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.input.InputEvent;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;

/** Przycisk. Klik = mysz wciśnięta i puszczona nad przyciskiem; z klawiatury Enter/Spacja przy fokusie. */
public class Button extends Widget {
    private String text;
    private final Runnable onClick;

    public Button(String text, Runnable onClick) { this(text, "button", onClick); }
    public Button(String text, String role, Runnable onClick) {
        super(role);
        this.text = text;
        this.onClick = onClick;
    }

    public String text() { return text; }
    public void setText(String t) { text = t; invalidateLayout(); }
    @Override public boolean isFocusable() { return true; }

    @Override public Size measure(Size avail, TextMeasurer tm) {
        ResolvedStyle st = layoutStyle();
        Size s = tm.measure(text, st.text());
        return applyPref(new Size(s.w() + st.padding().horizontal(), s.h() + st.padding().vertical()), st);
    }

    @Override protected void drawSelf(UiGraphics g) {
        ResolvedStyle st = style(g.nowMillis());
        g.box(bounds, st.box());
        TextStyle ts = st.text().withAlign(TextStyle.Align.CENTER);
        Size s = g.measure(text, ts);
        g.text(text, bounds.x(), bounds.y() + (bounds.h() - s.h()) / 2f, bounds.w(), ts);
    }

    @Override public boolean onEvent(InputEvent e) {
        if (e instanceof InputEvent.MouseDown d) return d.button() == InputEvent.BUTTON_LEFT;
        if (e instanceof InputEvent.MouseUp u) {
            if (u.button() == InputEvent.BUTTON_LEFT && bounds.contains(u.x(), u.y()) && isEnabled() && onClick != null) onClick.run();
            return true;
        }
        if (e instanceof InputEvent.KeyDown k && (k.key() == InputEvent.KEY_ENTER || k.key() == 32)) {
            if (onClick != null) onClick.run();
            return true;
        }
        return false;
    }
}
