package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.input.InputEvent;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;
import dev.mcgui.util.Property;

/** Suwak poziomy związany z {@link Property}{@code <Double>}; opcjonalny krok. */
public class Slider extends Widget {
    private final Property<Double> value;
    public final double min, max;
    public double step = 0;
    private boolean dragging;

    public Slider(Property<Double> value, double min, double max) {
        super("slider");
        this.value = value; this.min = min; this.max = max;
    }

    @Override public boolean isFocusable() { return true; }
    @Override public Size measure(Size avail, TextMeasurer tm) { return applyPref(new Size(80, 12), layoutStyle()); }

    private float fraction() { return (float) ((value.get() - min) / (max - min)); }

    @Override protected void drawSelf(UiGraphics g) {
        long now = g.nowMillis();
        ResolvedStyle st = style(now);
        float th = Math.max(st.minHeight(), 4);
        Rect track = new Rect(bounds.x(), bounds.centerY() - th / 2f, bounds.w(), th);
        g.box(track, st.box());
        float knobD = bounds.h();
        float cx = bounds.x() + knobD / 2f + (bounds.w() - knobD) * fraction();
        ResolvedStyle fill = theme().resolve("slider.fill", states);
        g.box(new Rect(track.x(), track.y(), cx - track.x(), th), fill.box());
        ResolvedStyle ks = theme().resolve("slider.knob", states);
        g.box(new Rect(cx - knobD / 2f, bounds.y(), knobD, knobD), ks.box());
    }

    private void setFromX(float x) {
        float knobD = bounds.h();
        double f = clamp((x - bounds.x() - knobD / 2f) / Math.max(1f, bounds.w() - knobD), 0, 1);
        double v = min + f * (max - min);
        if (step > 0) v = Math.round((v - min) / step) * step + min;
        value.set(Math.max(min, Math.min(max, v)));
    }

    @Override public boolean onEvent(InputEvent e) {
        if (e instanceof InputEvent.MouseDown d && d.button() == InputEvent.BUTTON_LEFT) { dragging = true; setFromX(d.x()); return true; }
        if (e instanceof InputEvent.MouseMove m && dragging) { setFromX(m.x()); return true; }
        if (e instanceof InputEvent.MouseUp) { dragging = false; return true; }
        if (e instanceof InputEvent.KeyDown k) {
            double st = step > 0 ? step : (max - min) / 20.0;
            if (k.key() == InputEvent.KEY_LEFT) { value.set(Math.max(min, value.get() - st)); return true; }
            if (k.key() == InputEvent.KEY_RIGHT) { value.set(Math.min(max, value.get() + st)); return true; }
        }
        return false;
    }
}
