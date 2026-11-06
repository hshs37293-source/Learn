package dev.mcgui.gfx;

public record Border(float width, int color, Placement placement) {
    public enum Placement { INSIDE, CENTER, OUTSIDE }
    public static final Border NONE = new Border(0, Color.TRANSPARENT, Placement.INSIDE);
    public static Border inside(float width, int color) { return new Border(width, color, Placement.INSIDE); }
    public boolean isVisible() { return width > 0 && Color.a(color) > 0; }
    public Border lerp(Border o, float t) {
        return new Border(width + (o.width - width) * t, Color.mix(color, o.color, t), t < 0.5f ? placement : o.placement);
    }
}
