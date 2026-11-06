package dev.mcgui.raster;

import dev.mcgui.gfx.BoxStyle;
import dev.mcgui.gfx.Border;
import dev.mcgui.gfx.Color;
import dev.mcgui.gfx.CornerRadii;
import dev.mcgui.gfx.ImageStyle;
import dev.mcgui.gfx.LayerStyle;
import dev.mcgui.gfx.Paint;
import dev.mcgui.gfx.Path;
import dev.mcgui.gfx.PathStyle;
import dev.mcgui.gfx.ShaderEffect;
import dev.mcgui.gfx.Shadow;
import dev.mcgui.gfx.Tessellator;
import dev.mcgui.gfx.TextStyle;
import dev.mcgui.gfx.TextureRef;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.math.Rect;
import dev.mcgui.math.Size;
import dev.mcgui.math.Vec2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Pełna implementacja {@link UiGraphics} oparta tylko na {@link Surface}.
 * Działa na KAŻDEJ wersji gry bez własnych shaderów. Ograniczenia (patrz README):
 * brak rozmycia tła, gradienty tylko pionowe (inne → kolor średni), cień jako warstwy,
 * przycinanie tylko prostokątne, obrót tylko tam, gdzie pozwala stos macierzy gry.
 * Do pełnej jakości (SDF, rozmycie, dowolne gradienty) służą backendy z shaderami.
 */
public final class SimpleBackend implements UiGraphics {
    private final Surface surface;
    private final float s;                       // piksele fizyczne na jednostkę logiczną
    private final Deque<Float> alphaStack = new ArrayDeque<>();
    private final Map<String, BiConsumer<Rect, ShaderEffect>> effects = new HashMap<>();
    private float alpha = 1f;

    /** Ostry tekst: skala fontu zaokrąglana do wielokrotności 0.5, pozycja do pełnych pikseli (mniej rozmycia). */
    public boolean crispText = true;

    public SimpleBackend(Surface surface) {
        this.surface = surface;
        this.s = surface.guiScale();
    }

    /** Rejestruje obsługę efektu {@code custom(...)} o danym id (dla backendu prostego, np. wersja bez shadera). */
    public SimpleBackend registerEffect(String id, BiConsumer<Rect, ShaderEffect> handler) { effects.put(id, handler); return this; }

    // ---------- pomiar ----------

    private float fontScale(TextStyle st) {
        float f = (st.size() / 9f) * s;
        return crispText ? Math.max(0.5f, Math.round(f * 2f) / 2f) : f;     // krok 0.5: unika rozmycia przy skali ułamkowej
    }

    @Override public Size measure(CharSequence text, TextStyle style) {
        float fs = fontScale(style);
        return new Size(surface.textWidth(text, fs) / s, surface.lineHeight(fs) / s);
    }

    // ---------- prostokąty ----------

    @Override public void box(Rect r, BoxStyle st) {
        float x = r.x() * s, y = r.y() * s, w = r.w() * s, h = r.h() * s;
        if (surface.supportsSdf()) {                          // gładkie krawędzie: shader SDF
            for (SdfElement e : SdfEmitter.emit(new Rect(x, y, w, h), st, s, alpha)) surface.drawSdf(e);
            return;
        }
        CornerRadii radii = st.radii().scale(s).clampTo(w, h);
        for (Shadow sh : st.shadows()) if (!sh.inset() && sh.isVisible()) drawShadow(x, y, w, h, radii, sh);
        if (st.fill().isVisible()) fillShape(x, y, w, h, radii, st.fill());
        Border b = st.border();
        if (b.isVisible()) strokeShape(x, y, w, h, radii, b);
    }

    private void drawShadow(float x, float y, float w, float h, CornerRadii radii, Shadow sh) {
        float blur = sh.blur() * s, spread = sh.spread() * s;
        int steps = Math.max(1, Math.min(10, (int) Math.ceil(blur / 1.5f)));
        // zewnętrzne warstwy coraz słabsze; suma alf ≈ alfa cienia przy środku
        for (int i = steps; i >= 1; i--) {
            float grow = spread + blur * i / steps - blur / 2f;
            float a = (float) Math.pow(1.0 - (i - 1) / (double) steps, 2) / steps * 1.6f;
            float gx = x + sh.dx() * s - grow, gy = y + sh.dy() * s - grow, gw = w + 2 * grow, gh = h + 2 * grow;
            if (gw <= 0 || gh <= 0) continue;
            CornerRadii gr = new CornerRadii(radii.tl() + Math.max(0, grow), radii.tr() + Math.max(0, grow),
                                             radii.br() + Math.max(0, grow), radii.bl() + Math.max(0, grow));
            int col = Color.multiplyAlpha(sh.color(), Math.min(1f, a) * alpha);
            Rasterizer.roundedRect(gx, gy, gw, gh, gr, false, (x0, y0, x1, y1, cov) ->
                surface.fillRect(x0, y0, x1, y1, Color.multiplyAlpha(col, cov)));
        }
    }

    private void fillShape(float x, float y, float w, float h, CornerRadii radii, Paint p) {
        if (p instanceof Paint.Linear l && l.x0() == l.x1() && l.y1() > l.y0() && radii.isZero()) {
            // pionowy gradient na zwykłym prostokącie: natywny fillGradient (najszybszy)
            surface.fillGradientV(x, y, x + w, y + h, Color.multiplyAlpha(l.c0(), alpha), Color.multiplyAlpha(l.c1(), alpha));
            return;
        }
        if (p instanceof Paint.Linear l && l.x0() == l.x1() && l.y1() > l.y0()) {
            Rasterizer.roundedRect(x, y, w, h, radii, true, (x0, y0, x1, y1, cov) -> {
                float t = ((y0 + y1) / 2f - y) / h;
                int c = Color.mix(l.c0(), l.c1(), (t - l.y0()) / (l.y1() - l.y0()));
                surface.fillRect(x0, y0, x1, y1, Color.multiplyAlpha(c, cov * alpha));
            });
            return;
        }
        int col = Color.multiplyAlpha(p.representative(), alpha);
        Rasterizer.roundedRect(x, y, w, h, radii, false, (x0, y0, x1, y1, cov) ->
            surface.fillRect(x0, y0, x1, y1, Color.multiplyAlpha(col, cov)));
    }

    private void strokeShape(float x, float y, float w, float h, CornerRadii radii, Border b) {
        float t = b.width() * s;
        float off = switch (b.placement()) { case INSIDE -> 0; case CENTER -> t / 2f; case OUTSIDE -> t; };
        float ox = x - off, oy = y - off, ow = w + 2 * off, oh = h + 2 * off;
        CornerRadii or = new CornerRadii(radii.tl() + off, radii.tr() + off, radii.br() + off, radii.bl() + off);
        int col = Color.multiplyAlpha(b.color(), alpha);
        Rasterizer.ring(ox, oy, ow, oh, or, t, (x0, y0, x1, y1, cov) ->
            surface.fillRect(x0, y0, x1, y1, Color.multiplyAlpha(col, cov)));
    }

    // ---------- tekst ----------

    @Override public void text(CharSequence text, float x, float y, float width, TextStyle st) {
        float fs = fontScale(st);
        float tw = surface.textWidth(text, fs);
        float px = x * s;
        if (width > 0) {
            float wpx = width * s;
            if (st.align() == TextStyle.Align.CENTER) px += (wpx - tw) / 2f;
            else if (st.align() == TextStyle.Align.RIGHT) px += wpx - tw;
        }
        float py = y * s;
        if (crispText) { px = Math.round(px); py = Math.round(py); }          // pozycja w pełnych pikselach fizycznych
        surface.drawText(text, px, py, Color.multiplyAlpha(st.color(), alpha), st.shadow(), fs);
    }

    // ---------- obrazy ----------

    @Override public void image(TextureRef tex, Rect dst, Rect src, ImageStyle st) {
        Rect d = new Rect(dst.x() * s, dst.y() * s, dst.w() * s, dst.h() * s);
        int tint = Color.multiplyAlpha(st.tint(), alpha);
        if (!st.isNineSlice()) {
            float tw = st.textureWidth() > 0 ? st.textureWidth() : 0, th = st.textureHeight() > 0 ? st.textureHeight() : 0;
            surface.drawImage(tex, d, src, st, tint, tw, th);
            return;
        }
        // 9-slice: 4 rogi stałe, 4 krawędzie i środek rozciągane
        var sl = st.nineSlice();
        float tw = st.textureWidth(), th = st.textureHeight();
        float l = sl.left() * s, r = sl.right() * s, t = sl.top() * s, b = sl.bottom() * s;
        float[] dx = { d.x(), d.x() + l, d.right() - r, d.right() };
        float[] dy = { d.y(), d.y() + t, d.bottom() - b, d.bottom() };
        float[] sx = { 0, sl.left(), tw - sl.right(), tw };
        float[] sy = { 0, sl.top(), th - sl.bottom(), th };
        for (int j = 0; j < 3; j++)
            for (int i = 0; i < 3; i++) {
                Rect dr = new Rect(dx[i], dy[j], dx[i + 1] - dx[i], dy[j + 1] - dy[j]);
                Rect sr = new Rect(sx[i], sy[j], sx[i + 1] - sx[i], sy[j + 1] - sy[j]);
                if (dr.w() > 0 && dr.h() > 0 && sr.w() > 0 && sr.h() > 0) surface.drawImage(tex, dr, sr, st, tint, tw, th);
            }
    }

    // ---------- ścieżki i linie ----------

    @Override public void path(Path p, PathStyle st) {
        for (Path.Contour c : p.flatten(0.25f / s)) {
            List<Vec2> pts = scalePts(c.points());
            if (st.fill().isVisible() && c.closed()) {
                float[] tris = Tessellator.fillPolygon(pts);
                int col = Color.multiplyAlpha(st.fill().representative(), alpha);
                for (int i = 0; i + 5 < tris.length; i += 6)
                    Rasterizer.triangle(tris[i], tris[i + 1], tris[i + 2], tris[i + 3], tris[i + 4], tris[i + 5],
                        (x0, y0, x1, y1, cov) -> surface.fillRect(x0, y0, x1, y1, Color.multiplyAlpha(col, cov)));
            }
            if (st.hasStroke()) {
                float[] tris = Tessellator.strokePolyline(pts, c.closed(), st.strokeWidth() * s);
                int col = Color.multiplyAlpha(st.strokeColor(), alpha);
                for (int i = 0; i + 5 < tris.length; i += 6)
                    Rasterizer.triangle(tris[i], tris[i + 1], tris[i + 2], tris[i + 3], tris[i + 4], tris[i + 5],
                        (x0, y0, x1, y1, cov) -> surface.fillRect(x0, y0, x1, y1, Color.multiplyAlpha(col, cov)));
            }
        }
    }

    private List<Vec2> scalePts(List<Vec2> in) {
        List<Vec2> out = new ArrayList<>(in.size());
        for (Vec2 v : in) out.add(new Vec2(v.x() * s, v.y() * s));
        return out;
    }

    @Override public void line(float x0, float y0, float x1, float y1, float width, int argb) {
        int col = Color.multiplyAlpha(argb, alpha);
        // poziome/pionowe odcinki: jeden prostokąt (najtaniej)
        float w = Math.max(width * s, 1f);
        if (y0 == y1) { surface.fillRect(Math.min(x0, x1) * s, y0 * s - w / 2f, Math.max(x0, x1) * s, y0 * s + w / 2f, col); return; }
        if (x0 == x1) { surface.fillRect(x0 * s - w / 2f, Math.min(y0, y1) * s, x0 * s + w / 2f, Math.max(y0, y1) * s, col); return; }
        Rasterizer.line(x0 * s, y0 * s, x1 * s, y1 * s, w, (a, b, c, d, cov) ->
            surface.fillRect(a, b, c, d, Color.multiplyAlpha(col, cov)));
    }

    // ---------- przycinanie, transformacje, warstwy ----------

    @Override public void pushClip(Rect r, CornerRadii radii) { surface.pushClip(r.x() * s, r.y() * s, r.right() * s, r.bottom() * s); }
    @Override public void popClip() { surface.popClip(); }
    @Override public void pushTranslate(float dx, float dy) { surface.pushMatrix(); surface.translate(dx * s, dy * s); }
    @Override public void pushScale(float sx, float sy, float px, float py) {
        surface.pushMatrix(); surface.translate(px * s, py * s); surface.scale(sx, sy); surface.translate(-px * s, -py * s);
    }
    @Override public void pushRotate(float rad, float px, float py) { surface.pushMatrix(); surface.rotateAbout(rad, px * s, py * s); }
    @Override public void popTransform() { surface.popMatrix(); }

    @Override public void pushLayer(Rect bounds, LayerStyle st) {
        alphaStack.push(alpha);
        alpha *= st.alpha();
        // st.backdropBlur() ignorowane w prostym backendzie
    }
    @Override public void popLayer() { alpha = alphaStack.pop(); }

    @Override public void custom(Rect r, ShaderEffect e) {
        BiConsumer<Rect, ShaderEffect> h = effects.get(e.id());
        if (h != null) h.accept(r, e);
    }

    @Override public float scale() { return s; }
    @Override public long nowMillis() { return surface.nowMillis(); }
}
