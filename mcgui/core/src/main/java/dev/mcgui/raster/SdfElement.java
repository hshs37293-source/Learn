package dev.mcgui.raster;

import dev.mcgui.gfx.CornerRadii;

/**
 * Element do narysowania shaderem SDF (gładkie krawędzie w dowolnej skali, bez "pikselozy").
 * Współrzędne w pikselach fizycznych. Kolory narożników (góra-lewy, góra-prawy, dół-prawy, dół-lewy)
 * interpolowane bilinearnie — daje gradienty poziome, pionowe i ukośne bez dodatkowych atrybutów.
 *
 * @param quad   prostokąt geometrii, który realnie jest rysowany
 * @param radii  promienie rogów KSZTAŁTU (dla cienia: kształtu rzucającego cień)
 * @param param  FILL: nieużywany; OUTLINE: grubość obrysu; SHADOW: promień rozmycia
 */
public record SdfElement(Kind kind, float x, float y, float w, float h, CornerRadii radii,
                         int cTL, int cTR, int cBR, int cBL, float param) {
    public enum Kind { FILL, OUTLINE, SHADOW }

    /** Rozmiar kształtu SDF: dla cienia quad jest powiększony o blur z każdej strony. */
    public float shapeW() { return kind == Kind.SHADOW ? Math.max(0, w - 2 * param) : w; }
    public float shapeH() { return kind == Kind.SHADOW ? Math.max(0, h - 2 * param) : h; }
}
