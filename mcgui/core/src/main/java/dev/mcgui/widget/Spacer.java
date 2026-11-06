package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.math.Size;

/** Pusty element do rozpychania układu (zwykle z wagą). */
public class Spacer extends Widget {
    public Spacer() { super("spacer"); this.weight = 1f; }
    public Spacer(float w, float h) { super("spacer"); this.prefWidth = w; this.prefHeight = h; }
    @Override public Size measure(Size avail, TextMeasurer tm) {
        return new Size(prefWidth == null ? 0 : prefWidth, prefHeight == null ? 0 : prefHeight);
    }
    @Override protected void drawSelf(UiGraphics g) {}
}
