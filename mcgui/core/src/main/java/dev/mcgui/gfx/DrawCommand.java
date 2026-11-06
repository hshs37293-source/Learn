package dev.mcgui.gfx;

import dev.mcgui.math.Rect;

/** Polecenie rysowania 2D zapisane na liście wyświetlania. */
public sealed interface DrawCommand {
    record Box(Rect rect, BoxStyle style) implements DrawCommand {}
    record Text(CharSequence text, float x, float y, float width, TextStyle style) implements DrawCommand {}
    record Image(TextureRef tex, Rect dst, Rect src, ImageStyle style) implements DrawCommand {}
    record PathCmd(Path path, PathStyle style) implements DrawCommand {}
    record Line(float x0, float y0, float x1, float y1, float width, int argb) implements DrawCommand {}
    record PushClip(Rect rect, CornerRadii radii) implements DrawCommand {}
    record PopClip() implements DrawCommand {}
    record PushTranslate(float dx, float dy) implements DrawCommand {}
    record PushScale(float sx, float sy, float px, float py) implements DrawCommand {}
    record PushRotate(float radians, float px, float py) implements DrawCommand {}
    record PopTransform() implements DrawCommand {}
    record PushLayer(Rect bounds, LayerStyle style) implements DrawCommand {}
    record PopLayer() implements DrawCommand {}
    record Custom(Rect rect, ShaderEffect effect) implements DrawCommand {}

    /** Odtwarza polecenie na innym {@link UiGraphics}. */
    default void replayOn(UiGraphics g) {
        if (this instanceof Box c) g.box(c.rect(), c.style());
        else if (this instanceof Text c) g.text(c.text(), c.x(), c.y(), c.width(), c.style());
        else if (this instanceof Image c) g.image(c.tex(), c.dst(), c.src(), c.style());
        else if (this instanceof PathCmd c) g.path(c.path(), c.style());
        else if (this instanceof Line c) g.line(c.x0(), c.y0(), c.x1(), c.y1(), c.width(), c.argb());
        else if (this instanceof PushClip c) g.pushClip(c.rect(), c.radii());
        else if (this instanceof PopClip) g.popClip();
        else if (this instanceof PushTranslate c) g.pushTranslate(c.dx(), c.dy());
        else if (this instanceof PushScale c) g.pushScale(c.sx(), c.sy(), c.px(), c.py());
        else if (this instanceof PushRotate c) g.pushRotate(c.radians(), c.px(), c.py());
        else if (this instanceof PopTransform) g.popTransform();
        else if (this instanceof PushLayer c) g.pushLayer(c.bounds(), c.style());
        else if (this instanceof PopLayer) g.popLayer();
        else if (this instanceof Custom c) g.custom(c.rect(), c.effect());
    }
}
