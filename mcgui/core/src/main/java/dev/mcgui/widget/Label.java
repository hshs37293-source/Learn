package dev.mcgui.widget;

import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.TextStyle;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.math.Size;
import dev.mcgui.style.ResolvedStyle;
import dev.mcgui.util.Property;

/** Tekst. Może być związany z {@link Property}. */
public class Label extends Widget {
    private String text;
    public TextStyle.Align align = TextStyle.Align.LEFT;

    public Label(String text) { super("label"); this.text = text; }
    @SuppressWarnings("this-escape")
    public Label(Property<String> bound) {
        super("label");
        this.text = bound.get();
        bound.listen(v -> { this.text = v; invalidateLayout(); });
    }

    public String text() { return text; }
    public void setText(String t) { if (!t.equals(text)) { text = t; invalidateLayout(); } }

    @Override public Size measure(Size avail, TextMeasurer tm) {
        ResolvedStyle st = layoutStyle();
        Size s = tm.measure(text, st.text());
        return applyPref(new Size(s.w() + st.padding().horizontal(), s.h() + st.padding().vertical()), st);
    }

    @Override protected void drawSelf(UiGraphics g) {
        ResolvedStyle st = style(g.nowMillis());
        TextStyle ts = st.text().withAlign(align);
        Size s = g.measure(text, ts);
        g.text(text, bounds.x() + st.padding().left(), bounds.y() + (bounds.h() - s.h()) / 2f,
            bounds.w() - st.padding().horizontal(), ts);
    }
}
