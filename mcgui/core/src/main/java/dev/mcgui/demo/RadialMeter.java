package dev.mcgui.demo;

import dev.mcgui.gfx.Color;
import dev.mcgui.gfx.Path;
import dev.mcgui.gfx.PathStyle;
import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.TextStyle;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.math.Size;
import dev.mcgui.util.Property;
import dev.mcgui.widget.Widget;

/**
 * Przykład WŁASNEGO widgetu: okrągły miernik narysowany ścieżkami (łuk + tekst).
 * Pokazuje, że do rysowania wystarczy {@link UiGraphics}: żadnych typów Minecrafta,
 * a widget działa na każdej linii gry i z każdym backendem.
 */
public final class RadialMeter extends Widget {
    private final Property<Double> value;
    public int trackColor = Color.argb(70, 255, 255, 255);
    public int arcColor = Color.parse("#4677ff");

    public RadialMeter(Property<Double> value) {
        super("meter");
        this.value = value;
        this.prefWidth = 48f;
        this.prefHeight = 48f;
    }

    @Override public Size measure(Size avail, TextMeasurer tm) { return applyPref(new Size(48, 48), layoutStyle()); }

    @Override protected void drawSelf(UiGraphics g) {
        float r = Math.min(bounds.w(), bounds.h()) / 2f - 3f;
        float cx = bounds.centerX(), cy = bounds.centerY();
        g.path(Path.circle(cx, cy, r), PathStyle.stroke(trackColor, 4f));

        // łuk od góry (−90°) zgodnie z ruchem wskazówek zegara
        double f = Math.max(0, Math.min(1, value.get()));
        int steps = Math.max(2, (int) (64 * f));
        Path arc = new Path();
        for (int i = 0; i <= steps; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * f * i / steps;
            float x = cx + (float) Math.cos(a) * r, y = cy + (float) Math.sin(a) * r;
            if (i == 0) arc.moveTo(x, y); else arc.lineTo(x, y);
        }
        if (f > 0) g.path(arc, PathStyle.stroke(arcColor, 4f));

        String label = Math.round(f * 100) + "%";
        TextStyle ts = TextStyle.DEFAULT.withAlign(TextStyle.Align.CENTER);
        g.text(label, cx - r, cy - g.measure(label, ts).h() / 2f, r * 2, ts);
    }
}
