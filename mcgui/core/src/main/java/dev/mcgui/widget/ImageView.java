package dev.mcgui.widget;

import dev.mcgui.gfx.ImageStyle;
import dev.mcgui.gfx.TextMeasurer;
import dev.mcgui.gfx.TextureRef;
import dev.mcgui.gfx.UiGraphics;
import dev.mcgui.math.Size;

/** Obraz (tekstura) wpisany w prostokąt widgetu. */
public class ImageView extends Widget {
    public TextureRef texture;
    public ImageStyle style = ImageStyle.DEFAULT;
    private final Size natural;

    public ImageView(TextureRef texture, float naturalW, float naturalH) {
        super("image");
        this.texture = texture;
        this.natural = new Size(naturalW, naturalH);
    }

    @Override public Size measure(Size avail, TextMeasurer tm) { return applyPref(natural, layoutStyle()); }
    @Override protected void drawSelf(UiGraphics g) { g.image(texture, bounds, null, style); }
}
