package dev.mcgui.mc;

import dev.mcgui.input.InputEvent;
import dev.mcgui.raster.SimpleBackend;
import dev.mcgui.widget.UiRoot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Ekran gry hostujący drzewo widgetów {@link UiRoot} (1.21.11, nazwy Mojang).
 * Sygnatury zdarzeń wejścia (MouseButtonEvent/KeyEvent/CharacterEvent) potwierdzone w kodzie Meteora
 * (gałąź 1.21.11, tam pod nazwami Yarn Click/KeyInput/CharInput); pakiet net.minecraft.client.input — do sprawdzenia.
 */
public class UiScreen extends Screen {
    private final UiRoot ui;

    public UiScreen(Component title, UiRoot ui) {
        super(title);
        this.ui = ui;
    }

    public UiRoot ui() { return ui; }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);                    // tło ekranu
        ui.setSize(width, height);
        ui.dispatch(new InputEvent.MouseMove(mouseX, mouseY));    // aktualizacja hover co klatkę
        try (McSurface surface = new McSurface(g)) {
            ui.render(new SimpleBackend(surface));
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        return ui.dispatch(new InputEvent.MouseDown((float) e.x(), (float) e.y(), e.button())) || super.mouseClicked(e, doubled);
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) {
        return ui.dispatch(new InputEvent.MouseUp((float) e.x(), (float) e.y(), e.button())) || super.mouseReleased(e);
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        return ui.dispatch(new InputEvent.MouseMove((float) e.x(), (float) e.y())) || super.mouseDragged(e, dx, dy);
    }

    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        return ui.dispatch(new InputEvent.Scroll((float) x, (float) y, (float) sx, (float) sy)) || super.mouseScrolled(x, y, sx, sy);
    }

    @Override public boolean keyPressed(KeyEvent e) {
        // modyfikatory GLFW (shift=1, ctrl=2, alt=4) pokrywają się z InputEvent.MOD_*
        return ui.dispatch(new InputEvent.KeyDown(e.key(), e.modifiers())) || super.keyPressed(e);
    }

    @Override public boolean charTyped(CharacterEvent e) {
        return ui.dispatch(new InputEvent.CharTyped(e.codepoint())) || super.charTyped(e);
    }

    @Override public boolean isPauseScreen() { return false; }
}
