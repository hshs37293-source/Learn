package dev.mcgui.mc;

import dev.mcgui.gfx.ImageStyle;
import dev.mcgui.gfx.TextureRef;
import dev.mcgui.math.Rect;
import dev.mcgui.raster.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Util;

/**
 * {@link Surface} dla Minecrafta 1.21.11 (nazwy Mojang) oparta o {@link GuiGraphics}.
 * Pracuje w pikselach fizycznych: konstruktor skaluje macierz o 1/guiScale, {@link #close()} ją zdejmuje.
 * Używane przez {@link dev.mcgui.raster.SimpleBackend}.
 */
public final class McSurface implements Surface, AutoCloseable {
    private final GuiGraphics g;
    private final Minecraft mc = Minecraft.getInstance();
    private final Font font = mc.font;
    private final float gui = (float) mc.getWindow().getGuiScale();

    public McSurface(GuiGraphics g) {
        this.g = g;
        g.pose().pushMatrix();
        g.pose().scale(1f / gui, 1f / gui);
    }

    @Override public void close() { g.pose().popMatrix(); }

    private static int r(float v) { return Math.round(v); }

    @Override public void fillRect(float x0, float y0, float x1, float y1, int argb) {
        int a = r(x0), b = r(y0), c = r(x1), d = r(y1);
        if (c > a && d > b) g.fill(a, b, c, d, argb);
    }

    @Override public void fillGradientV(float x0, float y0, float x1, float y1, int top, int bottom) {
        int a = r(x0), b = r(y0), c = r(x1), d = r(y1);
        if (c > a && d > b) g.fillGradient(a, b, c, d, top, bottom);
    }

    @Override public void drawText(CharSequence text, float x, float y, int argb, boolean shadow, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.drawString(font, text.toString(), 0, 0, argb, shadow);
        g.pose().popMatrix();
    }

    @Override public float textWidth(CharSequence text, float scale) { return font.width(text.toString()) * scale; }
    @Override public float lineHeight(float scale) { return font.lineHeight * scale; }

    @Override public void drawImage(TextureRef tex, Rect dst, Rect src, ImageStyle style, int tint, float texW, float texH) {
        McTextures.Entry e = McTextures.get(tex);
        if (e == null) return;
        int tw = e.width(), th = e.height();
        float u = src == null ? 0 : src.x(), v = src == null ? 0 : src.y();
        int rw = src == null ? tw : r(src.w()), rh = src == null ? th : r(src.h());
        // Uwaga: przeciążenie z kolorem (tint) jest założeniem — sprawdź w IDE (patrz README, sekcja "Do weryfikacji").
        g.blit(RenderPipelines.GUI_TEXTURED, e.id(), r(dst.x()), r(dst.y()), u, v, r(dst.w()), r(dst.h()), rw, rh, tw, th, tint);
    }

    @Override public void pushClip(float x0, float y0, float x1, float y1) { g.enableScissor(r(x0), r(y0), r(x1), r(y1)); }
    @Override public void popClip() { g.disableScissor(); }

    @Override public void pushMatrix() { g.pose().pushMatrix(); }
    @Override public void popMatrix() { g.pose().popMatrix(); }
    @Override public void translate(float dx, float dy) { g.pose().translate(dx, dy); }
    @Override public void scale(float sx, float sy) { g.pose().scale(sx, sy); }
    @Override public void rotateAbout(float radians, float px, float py) { g.pose().rotateAbout(radians, px, py); }

    @Override public float guiScale() { return gui; }
    @Override public long nowMillis() { return Util.getMillis(); }
}
