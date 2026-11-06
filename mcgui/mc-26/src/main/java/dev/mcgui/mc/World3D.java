package dev.mcgui.mc;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.mcgui.gfx.Color;
import dev.mcgui.gfx3d.DisplayList3D;
import dev.mcgui.gfx3d.Flattener3D;
import dev.mcgui.gfx3d.Geometry3D;
import dev.mcgui.gfx3d.Graphics3D;
import dev.mcgui.gfx3d.Ribbons;
import dev.mcgui.math.Vec3;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/**
 * Rysowanie sceny 3D w świecie gry (26.x, nazwy Mojang) — wzorzec oparty na kodzie referencyjnym
 * Fabric docs "latest" (CustomRenderPipeline, 26.2): LevelExtractionEvents (faza extract) + LevelRenderEvents
 * (faza draw) + StagedVertexBuffer. Scena jest migawką tworzoną w fazie extract (immutable), a rysowana w draw.
 *
 * Użycie: {@code World3D.show("moj-znacznik", g -> g.box(...));}. Linie → wstęgi (quady); teksty 3D pomijane.
 *
 * UWAGA 26.3: w 26.3 pakiety GPU zmieniły się na com.mojang.renderpearl.api.* (RenderPass, RenderPipeline, ...),
 * więc importy i sygnatury tej klasy trzeba dostosować — zob. ../../../../../README.md ("26.3").
 * NIEKOMPILOWANE w środowisku, w którym powstało.
 */
public final class World3D {
    /** Z testem głębi: zwykły potok vanilli. */
    private static final RenderPipeline DEPTH = RenderPipelines.DEBUG_FILLED_BOX;
    /** Bez testu głębi (widoczne przez ściany). */
    private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("mcgui", "pipeline/filled_through_walls"))
            .withDepthStencilState(Optional.empty())
            .build());

    private static final Map<String, Consumer<Graphics3D>> SCENES = new LinkedHashMap<>();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED = new StagedVertexBuffer(() -> "mcgui world3d", RenderType.SMALL_BUFFER_SIZE);

    /** Migawka z fazy extract: niemutowalna lista trójkątów + pozycja kamery. */
    private record Snapshot(List<Geometry3D.Tri> depth, List<Geometry3D.Tri> through) {}
    private static Snapshot snapshot = new Snapshot(List.of(), List.of());

    private World3D() {}

    public static void show(String id, Consumer<Graphics3D> scene) { SCENES.put(id, scene); }
    public static void remove(String id) { SCENES.remove(id); }

    public static void init() {
        LevelExtractionEvents.END_EXTRACTION.register(World3D::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(World3D::draw);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> STAGED.close());
    }

    // ---------- faza extract: zbieramy dane (bez rysowania) ----------
    private static void extract(net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext context) {
        if (SCENES.isEmpty()) { snapshot = new Snapshot(List.of(), List.of()); return; }
        DisplayList3D list = new DisplayList3D();
        for (Consumer<Graphics3D> s : List.copyOf(SCENES.values())) s.accept(list);
        Geometry3D geo = Flattener3D.flatten(list);
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.phys.Vec3 c = context.levelState().cameraRenderState.pos;
        Vec3 cam = new Vec3((float) c.x, (float) c.y, (float) c.z);
        float fov = (float) Math.toRadians(mc.options.fov().get());
        Geometry3D tris = Ribbons.linesToTriangles(geo, cam, fov, mc.getWindow().getHeight());
        List<Geometry3D.Tri> depth = new ArrayList<>(), through = new ArrayList<>();
        for (Geometry3D.Tri t : tris.triangles) (t.depth() ? depth : through).add(t);
        snapshot = new Snapshot(List.copyOf(depth), List.copyOf(through));
    }

    // ---------- faza draw ----------
    private static void draw(LevelRenderContext context) {
        Snapshot snap = snapshot;
        if (snap.depth().isEmpty() && snap.through().isEmpty()) return;
        PoseStack matrices = context.poseStack();
        net.minecraft.world.phys.Vec3 cam = context.levelState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4fc pose = matrices.last().pose();
        drawBatch(DEPTH, snap.depth(), pose);
        drawBatch(THROUGH_WALLS, snap.through(), pose);
        matrices.popPose();
        STAGED.endFrame();
    }

    private static void drawBatch(RenderPipeline pipeline, List<Geometry3D.Tri> tris, Matrix4fc pose) {
        if (tris.isEmpty()) return;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = STAGED.appendDraw(format, primitive,
            primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);
        VertexConsumer b = STAGED.getVertexBuilder(draw);
        for (Geometry3D.Tri t : tris) {
            float r = Color.r(t.argb()) / 255f, g = Color.g(t.argb()) / 255f, bl = Color.b(t.argb()) / 255f, a = Color.a(t.argb()) / 255f;
            // potok jest QUADS: trójkąt = zdegenerowany czworokąt (c powtórzone)
            b.addVertex(pose, t.a().x(), t.a().y(), t.a().z()).setColor(r, g, bl, a);
            b.addVertex(pose, t.b().x(), t.b().y(), t.b().z()).setColor(r, g, bl, a);
            b.addVertex(pose, t.c().x(), t.c().y(), t.c().z()).setColor(r, g, bl, a);
            b.addVertex(pose, t.c().x(), t.c().y(), t.c().z()).setColor(r, g, bl, a);
        }
        STAGED.upload();
        StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(draw);
        if (info == null) return;

        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "mcgui world3d", color, Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }
}
