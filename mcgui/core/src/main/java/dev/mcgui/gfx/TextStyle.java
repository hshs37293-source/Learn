package dev.mcgui.gfx;

public record TextStyle(FontRef font, float size, int color, Align align, boolean shadow, boolean bold) {
    public enum Align { LEFT, CENTER, RIGHT }
    public static final TextStyle DEFAULT = new TextStyle(FontRef.DEFAULT, 9f, Color.WHITE, Align.LEFT, false, false);

    public TextStyle withColor(int c) { return new TextStyle(font, size, c, align, shadow, bold); }
    public TextStyle withSize(float s) { return new TextStyle(font, s, color, align, shadow, bold); }
    public TextStyle withAlign(Align a) { return new TextStyle(font, size, color, a, shadow, bold); }
    public TextStyle withShadow(boolean s) { return new TextStyle(font, size, color, align, s, bold); }
    public TextStyle lerp(TextStyle o, float t) {
        return new TextStyle(t < 0.5f ? font : o.font, size + (o.size - size) * t, Color.mix(color, o.color, t),
            t < 0.5f ? align : o.align, t < 0.5f ? shadow : o.shadow, t < 0.5f ? bold : o.bold);
    }
}
