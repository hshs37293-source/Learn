package dev.mcgui.widget;

import dev.mcgui.gfx.Color;
import dev.mcgui.gfx.CornerRadii;
import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.TextStyle;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.input.InputEvent;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;
import dev.mcgui.util.Property;

/** Jednowierszowe pole tekstowe z kursorem, edycją i miganiem karetki. */
public class TextField extends Widget {
    private final Property<String> value;
    private int caret;
    public String placeholder = "";
    public int maxLength = 256;

    public TextField(Property<String> value) {
        super("textfield");
        this.value = value;
        this.caret = value.get().length();
    }

    @Override public boolean isFocusable() { return true; }
    public int caret() { return caret; }

    @Override public Size measure(Size avail, TextMeasurer tm) {
        ResolvedStyle st = layoutStyle();
        Size s = tm.measure("M", st.text());
        return applyPref(new Size(100, s.h() + st.padding().vertical()), st);
    }

    @Override protected void drawSelf(UiGraphics g) {
        long now = g.nowMillis();
        ResolvedStyle st = style(now);
        g.box(bounds, st.box());
        Rect inner = bounds.inset(st.padding());
        TextStyle ts = st.text();
        String txt = value.get();
        Size line = g.measure("M", ts);
        float ty = inner.y() + (inner.h() - line.h()) / 2f;
        g.pushClip(inner, CornerRadii.NONE);
        if (txt.isEmpty() && !hasState(dev.mcgui.style.State.FOCUSED)) {
            g.text(placeholder, inner.x(), ty, ts.withColor(Color.multiplyAlpha(ts.color(), 0.5f)));
        } else {
            // przewiń tak, by karetka była widoczna
            float caretX = g.measure(txt.substring(0, caret), ts).w();
            float scroll = Math.max(0, caretX - inner.w() + 2);
            g.text(txt, inner.x() - scroll, ty, ts);
            if (hasState(dev.mcgui.style.State.FOCUSED) && (now / 500) % 2 == 0)
                g.line(inner.x() + caretX - scroll, ty, inner.x() + caretX - scroll, ty + line.h(), 1, ts.color());
        }
        g.popClip();
    }

    @Override public boolean onEvent(InputEvent e) {
        if (e instanceof InputEvent.MouseDown d) return d.button() == InputEvent.BUTTON_LEFT;
        if (e instanceof InputEvent.CharTyped c) {
            if (c.codePoint() < 32 || value.get().length() >= maxLength) return true;
            String t = value.get();
            value.set(t.substring(0, caret) + new String(Character.toChars(c.codePoint())) + t.substring(caret));
            caret += Character.charCount(c.codePoint());
            return true;
        }
        if (e instanceof InputEvent.KeyDown k) {
            String t = value.get();
            switch (k.key()) {
                case InputEvent.KEY_BACKSPACE -> { if (caret > 0) { value.set(t.substring(0, caret - 1) + t.substring(caret)); caret--; } return true; }
                case InputEvent.KEY_DELETE -> { if (caret < t.length()) value.set(t.substring(0, caret) + t.substring(caret + 1)); return true; }
                case InputEvent.KEY_LEFT -> { caret = Math.max(0, caret - 1); return true; }
                case InputEvent.KEY_RIGHT -> { caret = Math.min(t.length(), caret + 1); return true; }
                case InputEvent.KEY_HOME -> { caret = 0; return true; }
                case InputEvent.KEY_END -> { caret = t.length(); return true; }
                default -> { return false; }
            }
        }
        return false;
    }
}
