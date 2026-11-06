package dev.learn.ui.widget;

import dev.learn.ui.draw.DrawCommand;
import dev.learn.ui.draw.DrawList;
import dev.learn.ui.theme.Theme;


public final class Slider extends Widget {
    @FunctionalInterface public interface FloatConsumer { void accept(float v); }
    public float min, max, value;
    public Slider.FloatConsumer onChange = v -> {};
    private boolean dragging;

    public Slider(float min, float max, float value, float width) {
        this.min = min; this.max = max; this.value = value; this.w = width; this.h = 14;
    }

    private void setFromX(float px) {
        float t = Math.max(0f, Math.min(1f, (px - absX()) / w));
        value = min + t * (max - min);
        onChange.accept(value);
    }

    @Override protected void drawSelf(DrawList out, Theme t) {
        float frac = (value - min) / (max - min);
        out.add(new DrawCommand.RoundedRect(absX(), absY() + h / 2 - 2, w, 4, 2, t.surfaceHover(), 0, 0));
        out.add(new DrawCommand.RoundedRect(absX(), absY() + h / 2 - 2, w * frac, 4, 2, t.accent(), 0, 0));
        out.add(new DrawCommand.RoundedRect(absX() + w * frac - 6, absY() + 1, 12, 12, 6, t.text(), 0, 0));
    }

    @Override public boolean onMouseDown(float px, float py, int b) {
        if (b == 0 && contains(px, py)) { dragging = true; setFromX(px); return true; }
        return false;
    }
    @Override public boolean onMouseMove(float px, float py) { if (dragging) { setFromX(px); return true; } return false; }
    @Override public boolean onMouseUp(float px, float py, int b) { boolean was = dragging; dragging = false; return was; }
}
