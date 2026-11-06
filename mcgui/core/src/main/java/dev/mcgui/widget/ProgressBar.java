package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;
import dev.mcgui.util.Property;

/** Pasek postępu 0..1. */
public class ProgressBar extends Widget {
    private final Property<Double> value;

    public ProgressBar(Property<Double> value) { super("progress"); this.value = value; }

    @Override public Size measure(Size avail, TextMeasurer tm) { return applyPref(new Size(80, 6), layoutStyle()); }

    @Override protected void drawSelf(UiGraphics g) {
        ResolvedStyle st = style(g.nowMillis());
        g.box(bounds, st.box());
        float f = (float) Math.max(0, Math.min(1, value.get()));
        if (f > 0) g.box(new Rect(bounds.x(), bounds.y(), bounds.w() * f, bounds.h()), theme().resolve("progress.fill", states).box());
    }
}
