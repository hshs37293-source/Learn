package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.layout.Align;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;

/** Warstwy jedna na drugiej (np. ikona na przycisku, odznaka). Domyślnie dzieci wypełniają obszar. */
public class Stack extends Container {
    public Align horizontal = Align.STRETCH, vertical = Align.STRETCH;

    public Stack() { super("stack"); }

    @Override protected Size measureContent(Size avail, TextMeasurer tm) {
        ResolvedStyle st = layoutStyle();
        float w = 0, h = 0;
        for (Widget c : children()) {
            if (!c.visible) continue;
            Size s = c.layoutMeasure(avail, tm);
            w = Math.max(w, s.w() + c.margin.horizontal());
            h = Math.max(h, s.h() + c.margin.vertical());
        }
        return applyPref(new Size(w + st.padding().horizontal(), h + st.padding().vertical()), st);
    }

    @Override public void arrange(Rect finalBounds) {
        super.arrange(finalBounds);
        Rect box = finalBounds.inset(layoutStyle().padding());
        for (Widget c : children()) {
            if (!c.visible) continue;
            Size s = c.desired();
            float availW = box.w() - c.margin.horizontal(), availH = box.h() - c.margin.vertical();
            float w = horizontal == Align.STRETCH ? availW : Math.min(s.w(), availW);
            float h = vertical == Align.STRETCH ? availH : Math.min(s.h(), availH);
            float x = box.x() + c.margin.left() + off(horizontal, availW, w);
            float y = box.y() + c.margin.top() + off(vertical, availH, h);
            c.arrange(new Rect(x, y, w, h));
        }
    }

    private static float off(Align a, float avail, float size) {
        return switch (a) { case START, STRETCH -> 0; case CENTER -> (avail - size) / 2f; case END -> avail - size; };
    }
}
