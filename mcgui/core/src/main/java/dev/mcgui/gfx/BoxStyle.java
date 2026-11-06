package dev.mcgui.gfx;

import java.util.ArrayList;
import java.util.List;

/** Pełny opis wyglądu prostokąta: wypełnienie, rogi, obramowanie, cienie. Niemutowalny. */
public record BoxStyle(Paint fill, CornerRadii radii, Border border, List<Shadow> shadows) {
    public static final BoxStyle EMPTY = new BoxStyle(Paint.NONE, CornerRadii.NONE, Border.NONE, List.of());

    public BoxStyle {
        fill = fill == null ? Paint.NONE : fill;
        radii = radii == null ? CornerRadii.NONE : radii;
        border = border == null ? Border.NONE : border;
        shadows = shadows == null ? List.of() : List.copyOf(shadows);
    }

    public static Builder builder() { return new Builder(); }

    public BoxStyle withFill(Paint p) { return new BoxStyle(p, radii, border, shadows); }
    public BoxStyle withRadii(CornerRadii r) { return new BoxStyle(fill, r, border, shadows); }
    public BoxStyle withBorder(Border b) { return new BoxStyle(fill, radii, b, shadows); }
    public BoxStyle withShadows(List<Shadow> s) { return new BoxStyle(fill, radii, border, s); }
    public BoxStyle withAlphaFactor(float f) {
        List<Shadow> sh = new ArrayList<>();
        for (Shadow s : shadows) sh.add(new Shadow(s.dx(), s.dy(), s.blur(), s.spread(), Color.multiplyAlpha(s.color(), f), s.inset()));
        return new BoxStyle(fill.withAlphaFactor(f), radii,
            new Border(border.width(), Color.multiplyAlpha(border.color(), f), border.placement()), sh);
    }

    public static final class Builder {
        private Paint fill = Paint.NONE;
        private CornerRadii radii = CornerRadii.NONE;
        private Border border = Border.NONE;
        private final List<Shadow> shadows = new ArrayList<>();
        public Builder fill(int argb) { this.fill = Paint.solid(argb); return this; }
        public Builder fill(Paint p) { this.fill = p; return this; }
        public Builder radius(float r) { this.radii = CornerRadii.all(r); return this; }
        public Builder radii(CornerRadii r) { this.radii = r; return this; }
        public Builder border(float width, int color) { this.border = Border.inside(width, color); return this; }
        public Builder border(Border b) { this.border = b; return this; }
        public Builder shadow(Shadow s) { this.shadows.add(s); return this; }
        public BoxStyle build() { return new BoxStyle(fill, radii, border, shadows); }
    }
}
