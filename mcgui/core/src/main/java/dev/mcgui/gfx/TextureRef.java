package dev.mcgui.gfx;

/** Odwołanie do tekstury po identyfikatorze, np. "mymod:textures/gui/icon.png". Bez typów Minecrafta. */
public record TextureRef(String id) {
    public static TextureRef of(String id) { return new TextureRef(id); }
}
