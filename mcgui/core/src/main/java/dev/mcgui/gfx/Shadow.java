package dev.mcgui.gfx;

/** Cień prostokąta (jak box-shadow w CSS). {@code inset} = cień wewnętrzny. */
public record Shadow(float dx, float dy, float blur, float spread, int color, boolean inset) {
    public static Shadow drop(float dx, float dy, float blur, int color) { return new Shadow(dx, dy, blur, 0, color, false); }
    public static Shadow glow(float blur, int color) { return new Shadow(0, 0, blur, 0, color, false); }
    public boolean isVisible() { return Color.a(color) > 0; }
    public Shadow lerp(Shadow o, float t) {
        return new Shadow(dx + (o.dx - dx) * t, dy + (o.dy - dy) * t, blur + (o.blur - blur) * t,
                          spread + (o.spread - spread) * t, Color.mix(color, o.color, t), t < 0.5f ? inset : o.inset);
    }
}
