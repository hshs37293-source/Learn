package dev.mcgui.mc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.mcgui.raster.SdfElement;
import dev.mcgui.raster.SdfPacking;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Stan elementu GUI rysowanego shaderem SDF. Dane z {@link SdfElement} trafiają w atrybuty wierzchołków
 * (uniformów per element nie wolno użyć: GUI jest batchowane po (pipeline, tekstura)).
 */
public record SdfGuiElement(SdfElement e, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
        implements GuiElementRenderState {

    @Override public void buildVertices(VertexConsumer v) {
        float x0 = e.x(), y0 = e.y(), x1 = e.x() + e.w(), y1 = e.y() + e.h();
        int sw = SdfPacking.encodeSize(e.w()), sh = SdfPacking.encodeSize(e.h());
        int rx = SdfPacking.radiiX(e.radii()), ry = SdfPacking.radiiY(e.radii());
        float line = switch (e.kind()) { case FILL -> 0f; case OUTLINE -> Math.max(e.param(), 0.01f); case SHADOW -> -Math.max(e.param(), 0.01f); };
        // kolejność jak w LiquidBounce: lewy-górny, lewy-dolny, prawy-dolny, prawy-górny
        v.addVertexWith2DPose(pose, x0, y0).setUv(0f, 0f).setColor(e.cTL()).setUv1(sw, sh).setUv2(rx, ry).setLineWidth(line);
        v.addVertexWith2DPose(pose, x0, y1).setUv(0f, 1f).setColor(e.cBL()).setUv1(sw, sh).setUv2(rx, ry).setLineWidth(line);
        v.addVertexWith2DPose(pose, x1, y1).setUv(1f, 1f).setColor(e.cBR()).setUv1(sw, sh).setUv2(rx, ry).setLineWidth(line);
        v.addVertexWith2DPose(pose, x1, y0).setUv(1f, 0f).setColor(e.cTR()).setUv1(sw, sh).setUv2(rx, ry).setLineWidth(line);
    }

    @Override public RenderPipeline pipeline() { return SdfPipeline.PIPELINE; }
    @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
}
