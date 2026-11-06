package dev.mcgui.mc;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Pipeline SDF dla elementów GUI (26.x, API blaze3d z 26.2): gładkie zaokrąglone prostokąty, obrysy i cienie.
 * Wzorzec: LiquidBounce (nextgen, commit z ery 26.2) — format z DefaultVertexFormat.*_FORMAT, withVertexBinding,
 * withPrimitiveTopology(QUADS) + GUI_SNIPPET. Shadery: assets/mcgui/shaders/core/sdf_box.{vsh,fsh}.
 *
 * 26.3: VertexFormat/RenderPipeline przeniesiono do com.mojang.renderpearl.api.* — zmień importy
 * (patrz ../../../../../README.md, sekcja 26.3). NIEKOMPILOWANE.
 */
public final class SdfPipeline {
    /** Position, UV0 (0..1), Color, UV1 (rozmiar), UV2 (promienie), LineWidth (tryb/grubość/blur). */
    public static final VertexFormat FORMAT = VertexFormat.builder(0)
        .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, DefaultVertexFormat.POSITION_FORMAT)
        .addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, DefaultVertexFormat.UV0_FORMAT)
        .addAttribute(DefaultVertexFormat.COLOR_SEMANTIC_NAME, DefaultVertexFormat.COLOR_FORMAT)
        .addAttribute(DefaultVertexFormat.UV1_SEMANTIC_NAME, DefaultVertexFormat.UV1_FORMAT)
        .addAttribute(DefaultVertexFormat.UV2_SEMANTIC_NAME, DefaultVertexFormat.UV2_FORMAT)
        .addAttribute(DefaultVertexFormat.LINE_WIDTH_SEMANTIC_NAME, DefaultVertexFormat.LINE_WIDTH_FORMAT)
        .build();

    public static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("mcgui", "pipeline/gui_sdf_box"))
            .withVertexShader(Identifier.fromNamespaceAndPath("mcgui", "core/sdf_box"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("mcgui", "core/sdf_box"))
            .withVertexBinding(0, FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build());

    private SdfPipeline() {}
}
