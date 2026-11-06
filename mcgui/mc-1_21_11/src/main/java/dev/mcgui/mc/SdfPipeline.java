package dev.mcgui.mc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Pipeline SDF dla elementów GUI (1.21.11): gładkie zaokrąglone prostokąty, obrysy i cienie w dowolnej skali.
 * Wzorzec: LiquidBounce (gałąź 1.21.11, snapshot 0a3d2a25) — własny format wierzchołków + GUI_SNIPPET + QUADS.
 * Shadery: assets/mcgui/shaders/core/sdf_box.{vsh,fsh}. NIEKOMPILOWANE — patrz README ("Do weryfikacji").
 */
public final class SdfPipeline {
    /** Position, UV0 (0..1), Color, UV1 (rozmiar), UV2 (promienie), LineWidth (tryb/grubość/blur). */
    public static final VertexFormat FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV1", VertexFormatElement.UV1)
        .add("UV2", VertexFormatElement.UV2)
        .add("LineWidth", VertexFormatElement.LINE_WIDTH)
        .build();

    public static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("mcgui", "pipeline/gui_sdf_box"))
            .withVertexShader(Identifier.fromNamespaceAndPath("mcgui", "core/sdf_box"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("mcgui", "core/sdf_box"))
            .withVertexFormat(FORMAT, VertexFormat.Mode.QUADS)
            .build());

    private SdfPipeline() {}
}
