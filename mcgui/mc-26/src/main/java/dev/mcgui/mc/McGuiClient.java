package dev.mcgui.mc;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mcgui.demo.DemoUi;
import dev.mcgui.gfx.Color;
import dev.mcgui.gfx3d.DrawMode3;
import dev.mcgui.math.Box3;
import dev.mcgui.math.Vec3;
import dev.mcgui.raster.SimpleBackend;
import dev.mcgui.style.Themes;
import dev.mcgui.util.Property;
import dev.mcgui.widget.Label;
import dev.mcgui.widget.Panel;
import dev.mcgui.widget.UiRoot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Punkt wejścia demo (26.x): klawisz RIGHT SHIFT otwiera ekran z pełnym GUI (2D + podgląd 3D),
 * mały HUD z FPS (też zbudowany z widgetów) i znacznik 3D w świecie.
 */
public class McGuiClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("mcgui", "main"));
    private KeyMapping openKey;

    @Override public void onInitializeClient() {
        openKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.mcgui.open", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                mc.gui.setScreen(new UiScreen(Component.literal("mcgui"), DemoUi.build(Themes.builtin(Themes.FLAT), new DemoUi.Model())));
            }
        });

        // HUD zbudowany z widgetów (bez wejścia): panel z licznikiem FPS
        Property<String> fps = new Property<>("FPS: -");
        Panel hudPanel = new Panel();
        hudPanel.add(new Label(fps));
        UiRoot hud = new UiRoot(Themes.builtin(Themes.GLASS), hudPanel);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("mcgui", "hud"), (graphics, tracker) -> {
            Minecraft mc = Minecraft.getInstance();
            fps.set("FPS: " + mc.getFps());
            hud.setSize(70, 24);
            graphics.pose().pushMatrix();
            graphics.pose().translate(4, 4);                      // w jednostkach GUI (przed skalowaniem McSurface)
            try (McSurface surface = new McSurface(graphics)) {
                hud.render(new SimpleBackend(surface));
            }
            graphics.pose().popMatrix();
        });

        // Znacznik 3D w świecie (współrzędne świata) — widoczny przez ściany dzięki depthTest(false)
        World3D.init();
        World3D.show("mcgui-demo", g -> {
            g.depthTest(false);
            g.box(Box3.of(0, 100, 0, 1, 101, 1), Color.argb(110, 70, 119, 255), DrawMode3.BOTH);
            g.arrow(new Vec3(0.5f, 101, 0.5f), new Vec3(0.5f, 103, 0.5f), Color.rgb(80, 255, 120), 3f);
        });
    }
}
