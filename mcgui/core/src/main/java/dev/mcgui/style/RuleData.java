package dev.mcgui.style;

import dev.mcgui.gfx.Border;
import dev.mcgui.gfx.CornerRadii;
import dev.mcgui.gfx.Paint;
import dev.mcgui.gfx.Shadow;
import dev.mcgui.math.Insets;

import java.util.List;

/** Częściowy opis stylu: pola null = "nie zmieniaj". Łączone warstwowo (rola ogólna → szczegółowa → stany). */
public final class RuleData {
    public Paint fill;
    public CornerRadii radii;
    public Border border;
    public List<Shadow> shadows;
    public Integer textColor;
    public Float textSize;
    public Boolean textShadow;
    public Insets padding;
    public Float minWidth, minHeight;
    public Integer transitionMs;
    public String easing;
    public Float layerAlpha;
    public Float backdropBlur;

    /** Nakłada niepuste pola {@code o} na ten obiekt (o wygrywa). */
    public RuleData overlay(RuleData o) {
        RuleData r = copy();
        if (o.fill != null) r.fill = o.fill;
        if (o.radii != null) r.radii = o.radii;
        if (o.border != null) r.border = o.border;
        if (o.shadows != null) r.shadows = o.shadows;
        if (o.textColor != null) r.textColor = o.textColor;
        if (o.textSize != null) r.textSize = o.textSize;
        if (o.textShadow != null) r.textShadow = o.textShadow;
        if (o.padding != null) r.padding = o.padding;
        if (o.minWidth != null) r.minWidth = o.minWidth;
        if (o.minHeight != null) r.minHeight = o.minHeight;
        if (o.transitionMs != null) r.transitionMs = o.transitionMs;
        if (o.easing != null) r.easing = o.easing;
        if (o.layerAlpha != null) r.layerAlpha = o.layerAlpha;
        if (o.backdropBlur != null) r.backdropBlur = o.backdropBlur;
        return r;
    }

    public RuleData copy() {
        RuleData r = new RuleData();
        r.fill = fill; r.radii = radii; r.border = border; r.shadows = shadows; r.textColor = textColor;
        r.textSize = textSize; r.textShadow = textShadow; r.padding = padding; r.minWidth = minWidth;
        r.minHeight = minHeight; r.transitionMs = transitionMs; r.easing = easing; r.layerAlpha = layerAlpha;
        r.backdropBlur = backdropBlur;
        return r;
    }
}
