package dev.mcgui.mc;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.mcgui.gfx.Color;
import dev.mcgui.gfx3d.DisplayList3D;
import dev.mcgui.gfx3d.Flattener3D;
import dev.mcgui.gfx3d.Geometry3D;
import dev.mcgui.gfx3d.Graphics3D;
import dev.mcgui.gfx3d.Ribbons;
import dev.mcgui.math.Vec3;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Rysowanie sceny 3D w świecie gry (1.21.11, nazwy Mojang) — wzorzec oparty na kodzie referencyjnym
 * Fabric docs (CustomRenderPipeline, 1.21.11): BufferBuilder + MappableRingBuffer + WorldRenderEvents.
 *
 * Użycie: {@code World3D.show("moj-znacznik", g -> g.box(...));} — scena rysowana co klatkę we współrzędnych świata.
 * Linie są zamieniane na wstęgi (quady), więc potrzebny jest tylko potok wypełnionych czworokątów.
 * Teksty 3D (billboard) nie są tu rysowane — patrz README.
 *
 * NIEKOMPILOWANE w środowisku, w którym powstało. Kroki do weryfikacji są w ../../../../../README.md.
 */
public final class World3D {
    /** Z testem głębi: zwykły potok vanilli. */
    private static final RenderPipeline DEPTH = RenderPipelines.DEBUG_FILLED_BOX;
    /** Bez testu głębi (widoczne przez ściany). */
    private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("mcgui", "pipeline/filled_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build());

    private static final Map<String, Consumer<Graphics3D>> SCENES = new LinkedHashMap<>();
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static MappableRingBuffer vertexBuffer;

    private World3D() {}

    /** Dodaje/zastępuje scenę o danym id. Scena jest wywoływana co klatkę. */
    public static void show(String id, Consumer<Graphics3D> scene) { SCENES.put(id, scene); }
    public static void remove(String id) { SCENES.remove(id); }

    public static void init() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(World3D::onRender);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> close());
    }

    private static void onRender(WorldRenderContext context) {
        if (SCENES.isEmpty()) return;
        DisplayList3D list = new DisplayList3D();
        for (Consumer<Graphics3D> s : List.copyOf(SCENES.values())) s.accept(list);
        Geometry3D geo = Flattener3D.flatten(list);
        if (geo.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = new Vec3((float) context.worldState().cameraRenderState.pos.x,
                            (float) context.worldState().cameraRenderState.pos.y,
                            (float) context.worldState().cameraRenderState.pos.z);
        float fov = (float) Math.toRadians(mc.options.fov().get());
        // sceny w światowych współrzędnych → wstęgi liczone względem kamery
        Geometry3D tris = Ribbons.linesToTriangles(geo, cam, fov, mc.getWindow().getHeight());

        List<Geometry3D.Tri> depth = new ArrayList<>(), through = new ArrayList<>();
        for (Geometry3D.Tri t : tris.triangles) (t.depth() ? depth : through).add(t);

        PoseStack matrices = context.matrices();
        matrices.pushPose();
        matrices.translate(-cam.x(), -cam.y(), -cam.z());
        Matrix4fc pose = matrices.last().pose();
        draw(mc, DEPTH, depth, pose);
        draw(mc, THROUGH_WALLS, through, pose);
        matrices.popPose();
    }

    private static void draw(Minecraft mc, RenderPipeline pipeline, List<Geometry3D.Tri> tris, Matrix4fc pose) {
        if (tris.isEmpty()) return;
        BufferBuilder buffer = new BufferBuilder(ALLOCATOR, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        for (Geometry3D.Tri t : tris) {
            float r = Color.r(t.argb()) / 255f, g = Color.g(t.argb()) / 255f, b = Color.b(t.argb()) / 255f, a = Color.a(t.argb()) / 255f;
            // potok jest QUADS: trójkąt = zdegenerowany czworokąt (c powtórzone)
            vertex(buffer, pose, t.a(), r, g, b, a);
            vertex(buffer, pose, t.b(), r, g, b, a);
            vertex(buffer, pose, t.c(), r, g, b, a);
            vertex(buffer, pose, t.c(), r, g, b, a);
        }
        MeshData built = buffer.buildOrThrow();
        MeshData.DrawState state = built.drawState();
        VertexFormat format = state.format();

        // --- wysyłka wierzchołków (jak w kodzie referencyjnym) ---
        int size = state.vertexCount() * format.getVertexSize();
        if (vertexBuffer == null || vertexBuffer.size() < size) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(() -> "mcgui world3d", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
        }
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = encoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(built.vertexBuffer(), view.data());
        }
        GpuBuffer vertices = vertexBuffer.currentBuffer();

        built.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
        GpuBuffer indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
        VertexFormat.IndexType indexType = built.drawState().indexType();

        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "mcgui world3d", mc.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                mc.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, indexType);
            pass.drawIndexed(0, 0, state.indexCount(), 1);
        }
        built.close();
        vertexBuffer.rotate();
    }

    private static void vertex(BufferBuilder b, Matrix4fc pose, Vec3 p, float r, float g, float bl, float a) {
        b.addVertex(pose, p.x(), p.y(), p.z()).setColor(r, g, bl, a);
    }

    private static void close() {
        ALLOCATOR.close();
        if (vertexBuffer != null) { vertexBuffer.close(); vertexBuffer = null; }
    }
}
