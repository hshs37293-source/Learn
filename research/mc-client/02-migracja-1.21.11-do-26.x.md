# 02. Migracja 1.21.11 → 26.x: kroki i tabele zmian

Ten plik zbiera wszystko, co trzeba zmienić, żeby klient z 1.21.11 zadziałał na 26.x. Źródła: `fabric-docs` (porting 26.1, porting Fabric API 26.1), kod Meteora (`4803071` kontra `e3a2e77`) i LiquidBounce (`0a3d2a25` kontra `2a3762a`). Znaczniki jak w [00-index.md](00-index.md).

> Ważne: dokumentacja Fabric opisuje oficjalnie migrację **1.21.11 → 26.1**. Dalsze zmiany do 26.2 i 26.3 (np. backend Vulkan, nowe API GPU) znam głównie **z kodu klientów**, a nie z przewodnika. Traktuj tabele dla renderowania jako zebrane z kodu, nie z oficjalnej listy.

## 1. Kolejność prac (rekomendowana)

1. **Najpierw mapowania.** Jeśli projekt używa Yarn, zmigruj go na mapowania Mojang, **zanim** ruszysz wersję. [DOK]
2. **Zbuduj działającą bazę 1.21.11** na mapowaniach Mojang (testy zielone).
3. **Zmień build script** (sekcja 2).
4. **Zmień class tweaker** (nagłówek i nazwy klas; sekcja 3).
5. **Napraw Fabric API** (sekcja 4).
6. **Napraw kod GUI i renderowanie** (sekcje 5–7).
7. **Napraw shadery** (sekcja 8).
8. **Uruchom testy ze zrzutami** (plik 07) i porównaj z wzorcami z 1.21.11.

## 2. Build script (10 kroków z dokumentacji) [DOK]

1. Zaktualizuj Gradle: `./gradlew wrapper --gradle-version latest`.
2. Podbij Minecraft, Loader, Loom i Fabric API (najlepiej w `gradle.properties`).
3. W `build.gradle` zmień `id "fabric-loom"` na `id "net.fabricmc.fabric-loom"` (i w `settings.gradle`, jeśli tam deklarujesz Loom). W szablonie 1.21.11 jest `fabric-loom-remap`, w 26.x `fabric-loom`. [KOD]
4. Usuń linię `mappings` z `dependencies`.
5. Zamień `modImplementation` / `modCompileOnly` / `modApi` na `implementation` / `compileOnly` / `api`.
6. Usuń lub podmień mody sprzed 26.1 (żaden mod dla 1.21.11 lub starszego nie działa na 26.1, nawet jako zależność compile-only).
7. W nagłówku class tweakera zamień `named` na `official`.
8. Ustaw zgodność z Javą 25 zamiast 21.
9. Zamień `remapJar` na `jar`.
10. Odśwież Gradle (`./gradlew --refresh-dependencies`, jeśli trzeba).

## 3. Class tweaker: co zmienić

| Co | 1.21.11 | 26.x | Źródło |
|---|---|---|---|
| Nagłówek | `classTweaker v1 named` | `classTweaker v1 official` | [DOK] |
| Klasa stanu GUI | `net/minecraft/client/gui/render/state/GuiRenderState` | `net/minecraft/client/renderer/state/gui/GuiRenderState` | [KOD] |
| Klasa grafiki GUI | `net/minecraft/client/gui/GuiGraphics` | `net/minecraft/client/gui/GuiGraphicsExtractor` | [KOD] |
| Zagnieżdżony scissor | `GuiGraphics$ScissorStack` | `GuiGraphicsExtractor$ScissorStack` | [KOD] |
| `RenderPipeline` | `com/mojang/blaze3d/pipeline/RenderPipeline` | `com/mojang/renderpearl/api/pipeline/RenderPipeline` | [KOD] |

Po migracji uruchom `./gradlew validateAccessWidener`, bo nieistniejące wpisy są domyślnie ignorowane. [DOK]

## 4. Fabric API 26.1: zmiany nazw (wybór istotny dla klienta)

Źródło: `porting/fabric-api` w `fabric-docs`. [DOK] Pełna lista jest długa; poniżej to, co dotyka klienta i GUI.

### 4.1 Komendy, zdarzenia, klawisze
| Było | Jest |
|---|---|
| `client.command.v2.ClientCommandManager` | `ClientCommands` |
| `FabricClientCommandSource.getWorld` | `getLevel` |
| `ClientTickEvents.START_WORLD_TICK` / `END_WORLD_TICK` | `START_LEVEL_TICK` / `END_LEVEL_TICK` |
| `ClientTickEvents$StartWorldTick` / `EndWorldTick` | `StartLevelTick` / `EndLevelTick` |
| `ClientWorldEvents` | `ClientLevelEvents` |
| `ClientWorldEvents$AfterClientWorldChange` | `ClientLevelEvents$AfterClientLevelChange` (`afterWorldChange` → `afterLevelChange`) |
| `client.keybinding.v1.KeyBindingHelper` | `client.keymapping.v1.KeyMappingHelper` (`registerKeyBinding` → `registerKeyMapping`) |
| `client.screen.v1.Screens.getButtons` | `getWidgets` |
| `Screens.getTextRenderer` | `getFont` |
| `Screens.getClient` | `getMinecraft` |

### 4.2 Renderowanie
| Było | Jest |
|---|---|
| `rendering.v1.world.WorldRenderEvents` | `rendering.v1.level.LevelRenderEvents` |
| `…world.WorldRenderContext` | `…level.LevelRenderContext` |
| `…world.AbstractWorldRenderContext` | `…level.AbstractLevelRenderContext` (`worldRenderer` → `levelRenderer`) |
| `…world.WorldExtractionContext` | `…level.LevelExtractionContext` |
| `…world.WorldTerrainRenderContext` | `…level.LevelTerrainRenderContext` (`sectionState` → `sectionsToRender`) |
| `WorldRenderEvents$BeforeEntities`, `AfterEntities`, `BeforeTranslucent`, `StartMain`, `EndMain`, `EndExtraction`, `DebugRender`… | te same nazwy pod `LevelRenderEvents$…` |
| `SpecialGuiElementRegistry` (+ `$Context`, `$Factory`) | `PictureInPictureRendererRegistry` (`createSpecialRenderer` → `createRenderer`; `orderedRenderCommandQueue` → `submitNodeCollector`) |
| `DrawItemStackOverlayCallback` | `RenderItemDecorationsCallback` (`onDrawItemStackOverlay` → `onRenderItemDecorations`) |
| `TooltipComponentCallback` | `ClientTooltipComponentCallback` |
| `BlockRenderLayerMap` | `ChunkSectionLayerMap` |
| `AtlasSourceRegistry` | `SpriteSourceRegistry` |
| `ColorProviderRegistry.BLOCK` | `BlockColorRegistry` |
| `EntityModelLayerRegistry` | `ModelLayerRegistry` |
| `LivingEntityFeatureRendererRegistrationCallback` | `LivingEntityRenderLayerRegistrationCallback` (`registerRenderers` → `registerLayers`) |
| `ParticleRendererRegistry` | `ParticleGroupRegistry` (`getParticleTextureSheet` → `getParticleRenderType`) |
| Renderer API: `renderLayer` w `MutableQuadView`/`QuadEmitter` | `chunkLayer` |

Uwaga dla klienta z własnym GUI: najczęściej dotkniesz `SpecialGuiElementRegistry` → `PictureInPictureRendererRegistry` (jeśli renderujesz modele 3D w GUI), `Screens.*` i `LevelRenderEvents`. `HudElementRegistry` nie ma w tej liście zmian nazwy. [DOK]

## 5. Klasy i metody GUI: 1.21.11 → 26.x

Źródło: `develop/rendering/*.md` w obu wersjach oraz kod klientów. [DOK / KOD]

| Pojęcie | 1.21.11 | 26.x |
|---|---|---|
| Główna klasa rysowania GUI | `GuiGraphics` | `GuiGraphicsExtractor` |
| Rysowanie tekstu | `drawString(...)` | `text(...)` |
| Obrys | `renderOutline(...)` | `outline(...)` |
| Linie | `hLine(...)`, `vLine(...)` | `horizontalLine(...)`, `verticalLine(...)` |
| Wypełnienie, scissor, `blit` | `fill`, `enableScissor`, `disableScissor`, `blit` | te same nazwy |
| Stos macierzy GUI | `Matrix3x2fStack` (od 1.21.8), `pose()` | bez zmian |
| Dodanie własnego elementu do stanu | `guiRenderState.submitGuiElement(...)` | `guiRenderState.addGuiElement(...)` |
| Faza w ekranie | `render(...)` | `extractRenderState(...)` (metody ekranów i komponentów) |
| HUD | `HudElementRegistry`, element dostaje `GuiGraphics` + `DeltaTracker` | to samo, ale z `GuiGraphicsExtractor` |
| Kolor tekstu | ARGB (od 1.21.6) | ARGB |
| Pomocnik koloru | `ARGB.opaque(...)` | `ARGB.opaque(...)` |
| Animacja czasowa | `Util.getMillis()` | `Util.getMillis()` |
| Animacja tickowa | `DeltaTracker.getGameTimeDeltaPartialTick(false)` | to samo |

Klienty używają teraz sufiksu **`extract`** w wielu miejscach (`extractRenderState`, `extractBackground`, `extractPortalOverlay` w mixinach Meteora 26.3). Oznacza to, że rysowanie ekranów jest w pełni fazą „zbierz stan", a nie „rysuj". [KOD]

## 6. API GPU: 1.21.11 → 26.x (tabela zbiorcza)

Źródło: porównanie `MeshRenderer`, `MeteorRenderPipelines`, `MeteorVertexFormats` w Meteorze `4803071` i `e3a2e77` oraz `ClientRenderPipelines.kt`, `ClientVertexFormats.kt` w LiquidBounce `2a3762a`. [KOD]

| Obszar | 1.21.11 | 26.x |
|---|---|---|
| Pakiet | `com.mojang.blaze3d.*` | `com.mojang.renderpearl.api.*` (+ `blaze3d` dla `RenderSystem`, `RenderTarget`, `DefaultVertexFormat`) |
| Urządzenie | `RenderSystem.getDevice()` → `GpuDevice` | `RenderSystem.getDevice()` → `GpuDevice` (api.device); backendy GL / Vulkan / Metal |
| Pipeline: format | `.withVertexFormat(format, VertexFormat.DrawMode.X)` | `.withVertexBinding(0, format).withPrimitiveTopology(PrimitiveTopology.X)` |
| Format wierzchołków | `VertexFormat.builder().add("Nazwa", VertexFormatElement)`; własne elementy: `VertexFormatElement.register(id, ...)`, limit 32 | `VertexFormat.builder(stepRate).addAttribute("Nazwa", GpuFormat.X)`; bez limitu |
| Uniformy | `.withUniform("Nazwa", UniformType.UNIFORM_BUFFER)` | `BindGroupLayout.builder().withUniform("Nazwa", UniformType.UNIFORM_BUFFER)` + `.withBindGroupLayout(...)` |
| Samplery | `.withSampler("u_Texture")` | `withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)` w bind group |
| Głębia | `.withDepthTestFunction(...)`, `.withDepthWrite(...)` | `.withDepthStencilState(new DepthStencilState(CompareOp.X, writeDepth))` |
| Blend | `.withBlend(BlendFunction.TRANSLUCENT)` | `.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))` |
| Define do shaderów | brak w widzianym kodzie | `.withShaderDefine("UI")` |
| Kompilacja | `device.precompilePipeline(pipeline, (id, type) -> źródło)` po `ShaderLoader.apply` | `device.compilePipeline(pipeline, shaderSource, executor)` + `finishCompile()` + `cache.insert(...)` w `ShaderManager.apply`, albo `RenderPipelines.register(...)` |
| Użycie pipeline'u | `pass.setPipeline(pipeline)` | `pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline))` |
| Tekstury w passie | `pass.bindTexture(name, view, sampler)` | `pass.setUniform(name, view, sampler)` |
| Draw | `pass.drawIndexed(0, 0, count, 1)` | `pass.drawIndexed(count, 1, firstIndex, 0, 0)` |
| Indeksy | `pass.setIndexBuffer(buffer, VertexFormat.IndexType.INT)` | `pass.setIndexBuffer(slice.buffer, IndexType.INT)` (`IndexType` w `pipeline`) |
| Bufory | `GpuBuffer` | `GpuBuffer` + `GpuBufferSlice`; `encoder.transientMemory().uploadGpu(...)` |
| Uniformy dynamiczne | `DynamicUniformStorage<T>` (`clear()`) | `DynamicGpuDataStorageMapped<T>` (`endFrame()`) |
| Tekstura | `createTexture(label, flagi, TextureFormat.RGBA8, w, h, 1, 1)` | `createTexture(label, flagi, GpuFormat.RGBA8_UNORM, w, h, 1, 1)` |
| Render pass | `createRenderPass(supplier, color, OptionalInt clear[, depth, OptionalDouble])`, `close()` | `createRenderPass(supplier, color, Optional<Vector4fc> clear[, depth, OptionalDouble])`, `AutoCloseable` |

## 7. Kod własnego silnika: co przepisać

Kolejność prac nad warstwą `UiGraphics` / silnikiem (plik 08):

1. **Wydziel wszystko, co dotyka `com.mojang.blaze3d.*`/`renderpearl` do jednego pakietu** (np. `gfx.backend`). Reszta kodu zna tylko Twój interfejs.
2. Dla każdej linii zrób osobną implementację tego pakietu.
3. Format wierzchołków, pipeline'y i kompilacja to najbardziej zmienione miejsca (sekcja 6).
4. Hook „rysuj na wierzchu" jest inny (plik 04, sekcja o hookach).
5. Class tweaker osobno dla każdej linii.

Wzorzec LiquidBounce [KOD]: ten sam rekord stanu elementu GUI (`RoundedRectGuiElementRenderState`) istnieje w obu liniach; różnią się importy (`com.mojang.blaze3d.pipeline.RenderPipeline` kontra `com.mojang.renderpearl.api.pipeline.RenderPipeline`), nazwa metody wysyłki (`submitGuiElement` kontra `addGuiElement`) i klasa grafiki. To dowodzi, że przy dobrej separacji **logika jest wspólna**, a zmienia się cienka warstwa.

## 8. Shadery: co zmienić

| Aspekt | 1.21.11 | 26.x | Źródło |
|---|---|---|---|
| Rozszerzenia plików | `.vert` / `.frag` (Meteor), `.vsh` / `.fsh` (LiquidBounce GUI) | `.vsh` / `.fsh` (Meteor), `.vsh/.fsh` i `.vert/.frag` (LiquidBounce) | [KOD] |
| Nagłówek | `#version 330 core` (Meteor) / `#version 330` | `#version 330` + `#extension GL_ARB_separate_shader_objects : require` | [KOD] |
| `layout(location)` | wejścia wierzchołka | **wszystkie** `in` i `out`, także między etapami | [KOD] |
| Uniformy vanilli | bloki `DynamicTransforms`, `Projection` deklarowane ręcznie (LiquidBounce 1.21.11) | `#include <minecraft:dynamictransforms.glsl>` i `<minecraft:projection.glsl>` | [KOD] |
| Dopasowanie atrybutów | `in vec3 Position; in vec2 UV0;…` | `layout(location = 0) in vec3 Position;…` z numerami zgodnymi z kolejnością w `VertexFormat` | [KOD] |
| Warianty 2D/3D jednego shadera | osobne pliki | jeden plik z `#ifdef UI` i `withShaderDefine("UI")` | [KOD] |

Przykład (Meteor 26.3, struktura `pos_color.vsh`) [KOD, opis własnymi słowami]: ten sam vertex shader obsługuje pozycję 2D lub 3D; makro `UI` decyduje, czy `Position` jest `vec2` czy `vec3`, a wynik to `gl_Position = u_Proj * u_ModelView * pozycja`.

## 9. Lista kontrolna po migracji

- [ ] Build przechodzi na 26.x bez `mappings` i z Javą 25.
- [ ] `validateAccessWidener` czyste.
- [ ] Wszystkie `GuiGraphics` zamienione na `GuiGraphicsExtractor`; `drawString` → `text`.
- [ ] `submitGuiElement` → `addGuiElement`.
- [ ] Pipeline'y zbudowane nowym builderem; shadery z `layout(location)`.
- [ ] Ekrany i komponenty używają `extractRenderState`.
- [ ] Zdarzenia Fabric API zamienione (tabela 4.1).
- [ ] Zrzuty ekranu z testów 1.21.11 i 26.x wyglądają tak samo (lub różnice są zrozumiane).
- [ ] Sprawdzone na backendzie OpenGL **i** Vulkan (od 26.2).

## 10. Czego tu nie ma

- Pełnej listy zmian 26.1 → 26.3 (np. dokładnej chwili zmiany API GPU). Wiem tylko, że `1.21.11` i `26.3` różnią się tak, jak pokazano. Kiedy dokładnie, między 26.1 a 26.3, nie sprawdzałem. [LUKA]
- Zmian w grze niezwiązanych z GUI (bloki, encje, sieć). Te opisuje oficjalny przewodnik i primery NeoForge, do których linkuje dokumentacja Fabric. [DOK]
