package dev.mcgui.widget;

import dev.mcgui.anim.Easing;
import dev.mcgui.anim.Tween;
import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.input.InputEvent;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;
import dev.mcgui.style.State;
import dev.mcgui.util.Property;

/** Przełącznik on/off związany z {@link Property}{@code <Boolean>}; gałka przesuwa się płynnie. */
public class Toggle extends Widget {
    private final Property<Boolean> value;
    private final Tween knob;

    public Toggle(Property<Boolean> value) {
        super("toggle");
        this.value = value;
        this.knob = new Tween(value.get() ? 1f : 0f);
        value.listen(v -> { /* animacja startuje w draw */ });
    }

    @Override public boolean isFocusable() { return true; }

    @Override public Size measure(Size avail, TextMeasurer tm) { return applyPref(new Size(24, 12), layoutStyle()); }

    @Override protected void drawSelf(UiGraphics g) {
        long now = g.nowMillis();
        setState(State.ACTIVE, value.get());
        float target = value.get() ? 1f : 0f;
        if (knob.target() != target) knob.animateTo(target, now, 160, Easing.EASE_OUT);
        ResolvedStyle st = style(now);
        g.box(bounds, st.box());
        ResolvedStyle ks = theme().resolve("toggle.knob", states);
        float d = bounds.h() - 4;
        float x = bounds.x() + 2 + (bounds.w() - d - 4) * knob.value(now);
        g.box(new Rect(x, bounds.y() + 2, d, d), ks.box());
    }

    @Override public boolean onEvent(InputEvent e) {
        if (e instanceof InputEvent.MouseDown d) return d.button() == InputEvent.BUTTON_LEFT;
        if (e instanceof InputEvent.MouseUp u) {
            if (u.button() == InputEvent.BUTTON_LEFT && bounds.contains(u.x(), u.y())) value.set(!value.get());
            return true;
        }
        if (e instanceof InputEvent.KeyDown k && (k.key() == InputEvent.KEY_ENTER || k.key() == 32)) { value.set(!value.get()); return true; }
        return false;
    }
}
