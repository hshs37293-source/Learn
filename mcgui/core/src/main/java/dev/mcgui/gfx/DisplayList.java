package dev.mcgui.gfx;

import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Lista wyświetlania: implementacja {@link UiGraphics}, która tylko zapisuje polecenia.
 * Służy do (a) testów bez GPU, (b) buforowania statycznych paneli, (c) przekazania
 * sceny do backendu po zakończeniu fazy "extract".
 */
public final class DisplayList implements UiGraphics {
    private final List<DrawCommand> cmds = new ArrayList<>();
    private final TextMeasurer measurer;
    private final float scale;
    private final LongSupplier clock;
    private int clipDepth, transformDepth, layerDepth;

    public DisplayList() { this(TextMeasurer.monospace(), 1f, System::currentTimeMillis); }
    public DisplayList(TextMeasurer measurer, float scale, LongSupplier clock) {
        this.measurer = measurer; this.scale = scale; this.clock = clock;
    }

    public List<DrawCommand> commands() { return List.copyOf(cmds); }
    public int size() { return cmds.size(); }
    public void clear() { cmds.clear(); clipDepth = transformDepth = layerDepth = 0; }

    /** Czy push/pop są zbilansowane (po zakończeniu rysowania powinno być true). */
    public boolean isBalanced() { return clipDepth == 0 && transformDepth == 0 && layerDepth == 0; }

    public void replay(UiGraphics target) { for (DrawCommand c : cmds) c.replayOn(target); }

    @Override public void box(Rect r, BoxStyle s) { cmds.add(new DrawCommand.Box(r, s)); }
    @Override public void text(CharSequence t, float x, float y, float w, TextStyle s) { cmds.add(new DrawCommand.Text(t.toString(), x, y, w, s)); }
    @Override public void image(TextureRef t, Rect dst, Rect src, ImageStyle s) { cmds.add(new DrawCommand.Image(t, dst, src, s)); }
    @Override public void path(Path p, PathStyle s) { cmds.add(new DrawCommand.PathCmd(p, s)); }
    @Override public void line(float x0, float y0, float x1, float y1, float w, int c) { cmds.add(new DrawCommand.Line(x0, y0, x1, y1, w, c)); }
    @Override public void pushClip(Rect r, CornerRadii radii) { clipDepth++; cmds.add(new DrawCommand.PushClip(r, radii)); }
    @Override public void popClip() { clipDepth--; cmds.add(new DrawCommand.PopClip()); }
    @Override public void pushTranslate(float dx, float dy) { transformDepth++; cmds.add(new DrawCommand.PushTranslate(dx, dy)); }
    @Override public void pushScale(float sx, float sy, float px, float py) { transformDepth++; cmds.add(new DrawCommand.PushScale(sx, sy, px, py)); }
    @Override public void pushRotate(float rad, float px, float py) { transformDepth++; cmds.add(new DrawCommand.PushRotate(rad, px, py)); }
    @Override public void popTransform() { transformDepth--; cmds.add(new DrawCommand.PopTransform()); }
    @Override public void pushLayer(Rect b, LayerStyle s) { layerDepth++; cmds.add(new DrawCommand.PushLayer(b, s)); }
    @Override public void popLayer() { layerDepth--; cmds.add(new DrawCommand.PopLayer()); }
    @Override public void custom(Rect r, ShaderEffect e) { cmds.add(new DrawCommand.Custom(r, e)); }
    @Override public float scale() { return scale; }
    @Override public long nowMillis() { return clock.getAsLong(); }
    @Override public Size measure(CharSequence text, TextStyle style) { return measurer.measure(text, style); }

    /** Zlicza polecenia danego typu (pomocne w testach). */
    public <T extends DrawCommand> long count(Class<T> type) { return cmds.stream().filter(type::isInstance).count(); }
}
