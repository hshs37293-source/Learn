package dev.learn.ui.widget;

import dev.learn.ui.draw.UiBackend;
import dev.learn.ui.theme.Theme;

/** Kontener układający dzieci w pionie lub poziomie z odstępem i paddingiem. */
public final class Stack extends Widget {
    public enum Dir { COLUMN, ROW }
    public final Dir dir;
    public float gap = 6f;

    public Stack(Dir dir) { this.dir = dir; }

    @Override public void layout(UiBackend backend, Theme theme) {
        float pad = theme.padding(), pos = pad, cross = 0;
        for (Widget c : children) {
            c.layout(backend, theme);
            if (!c.visible) continue;
            if (dir == Dir.COLUMN) { c.x = pad; c.y = pos; pos += c.h + gap; cross = Math.max(cross, c.w); }
            else { c.x = pos; c.y = pad; pos += c.w + gap; cross = Math.max(cross, c.h); }
        }
        float main = Math.max(pad, pos - gap + pad);
        if (children.isEmpty()) main = 2 * pad;
        if (dir == Dir.COLUMN) { w = cross + 2 * pad; h = main; }
        else { w = main; h = cross + 2 * pad; }
    }

    @Override protected void drawSelf(dev.learn.ui.draw.DrawList out, Theme t) {
        out.add(new dev.learn.ui.draw.DrawCommand.RoundedRect(absX(), absY(), w, h, t.radius(), t.surface(), 0, 0));
    }
}
