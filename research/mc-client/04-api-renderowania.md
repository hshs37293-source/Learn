# 04. API renderowania: 1.21.11 (Blaze3D) kontra 26.x (renderpearl)

Plik zbiera wszystko, co trzeba wiedzieć o niskopoziomowym API rysowania w obu liniach. Stan na 2026-11-06. Znaczniki jak w [00-index.md](00-index.md). Głębszy, oparty na kodzie przewodnik po własnym silniku 26.x: [../custom-rendering-gui-guide.md](../custom-rendering-gui-guide.md). Tu jest wersja porównawcza i hooki.

## 1. Model „extract → draw"

Od 1.21.6 renderowanie dzieli się na **fazę ekstrakcji** (dane trafiają do obiektów `RenderState`) i **fazę rysowania** (inny wątek może rysować klatkę N, gdy N+1 jest ekstrahowana). W 1.21.8 na ten model przeszło GUI: metody grafiki tylko dodają elementy do stanu, a zapis do `BufferBuilder` dzieje się na końcu fazy przygotowania (`GuiRenderer#prepare`). [DOK]

Konsekwencje:
1. Wywołania `fill`, `text`, `blit` i własne `addGuiElement` **nie rysują natychmiast**.
2. Stan, który zapisujesz, musi przeżyć wywołanie (kopiuj macierze: `new Matrix3x2f(pose)`).
3. Nie wolno trzymać referencji do mutowalnych obiektów gry w elemencie stanu, bo rysowanie może być na innym wątku. [WIEDZA co do wątków; model opisany w DOK]
4. Własny kod rysujący GUI wykonuj w fazie ekstrakcji (`extractRenderState` w 26.x, `render` w 1.21.11).

## 2. Co wolno, a czego nie

| Zasada | 1.21.11 | 26.x |
|---|---|---|
| Surowy OpenGL | niezalecany, nieprzewidywalny | **niewspierany** (od 26.2 jest opcjonalny Vulkan) [DOK] |
| Warstwa pośrednia | Blaze3D (`com.mojang.blaze3d.*`) | `com.mojang.renderpearl.api.*` + `blaze3d` [KOD] |
| Backendy | OpenGL | OpenGL, Vulkan, Metal [KOD: mixiny Meteora dla `GlDevice`, `VulkanDevice`, `MetalDevice`] |
| Własne pipeline'y | tak | tak |
| Własne tekstury offscreen | tak | tak |
| Własne shadery | tak | tak |

## 3. Podstawowe pojęcia (wspólne dla obu linii)

### 3.1 `VertexFormat`
Definiuje, jakie atrybuty ma wierzchołek i w jakiej kolejności są pakowane. Domyślne formaty vanilli (`DefaultVertexFormat`): [DOK]

| Nazwa | Zawartość |
|---|---|
| `POSITION` | pozycja |
| `POSITION_COLOR` | pozycja, kolor |
| `POSITION_TEX` | pozycja, uv |
| `POSITION_TEX_COLOR` | pozycja, uv, kolor |
| `POSITION_COLOR_NORMAL` | pozycja, kolor, normalna |
| `POSITION_COLOR_LIGHTMAP` | pozycja, kolor, światło |
| `POSITION_TEX_COLOR_NORMAL` | pozycja, uv, kolor, normalna |
| `BLOCK`, `NEW_ENTITY`, `PARTICLE` | formaty specjalne gry |

### 3.2 Tryb rysowania (topologia)
| Wartość | Opis |
|---|---|
| `LINES` | każda linia z 2 wierzchołków |
| `LINE_STRIP` | ciągła linia |
| `DEBUG_LINES` | linie zawsze 1 piksel szerokości |
| `DEBUG_LINE_STRIP` | j.w. dla pasa |
| `TRIANGLES` | trójkąty (po 3 wierzchołki) |
| `TRIANGLE_STRIP` | pas trójkątów |
| `TRIANGLE_FAN` | wachlarz |
| `QUADS` | czworokąty (po 4 wierzchołki) |

W 1.21.11 to `VertexFormat.Mode` (Mojang) lub `VertexFormat.DrawMode` (Yarn); w 26.x `PrimitiveTopology` (`TRIANGLES`, `QUADS`, `DEBUG_LINES` widziane w kodzie klientów). [DOK / KOD]

### 3.3 Culling
Kolejność wierzchołków decyduje o tym, która strona trójkąta jest „przednia". Zła kolejność = element niewidoczny (jeśli `cull = true`). W pipeline'ach UI Meteor używa `withCull(true)` dla 2D. [DOK / KOD]

### 3.4 Macierze
W GUI od 1.21.8: `Matrix3x2fStack` (bez parametru z), dostępny przez `graphics.pose()`; push/pop jak wcześniej. W świecie 3D: `PoseStack` z `pushPose/popPose/last/translate/scale/rotateAround`. [DOK]

## 4. Siedem klocków (obie linie)

Poniższa tabela mapuje funkcję → API w obu liniach. Linia 1.21.11 pochodzi z Meteora `4803071` (nazwy **Yarn** wskazane, gdzie różnią się od Mojang) i LiquidBounce `0a3d2a25`, linia 26.x z `e3a2e77` i `2a3762a`. [KOD]

| Klocek | 1.21.11 | 26.x |
|---|---|---|
| Format wierzchołków | `VertexFormat.builder().add("Nazwa", VertexFormatElement.X)` | `VertexFormat.builder(stepRate).addAttribute("Nazwa", GpuFormat.X)` |
| Własny element | `VertexFormatElement.register(id, 0, Type.FLOAT, Usage.POSITION, 2)`; id szukane pętlą, limit **32** w całej grze | nie potrzebny |
| Pipeline | `RenderPipeline.builder(snippets...)…build()` | `RenderPipeline.builder()…build()` |
| Uniformy | `withUniform("Nazwa", UniformType.UNIFORM_BUFFER)` | `BindGroupLayout.builder().withUniform(...)` + `withBindGroupLayout` |
| Sampler | `withSampler("u_Texture")` | `withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)` |
| Głębia | `withDepthTestFunction`, `withDepthWrite` | `withDepthStencilState(new DepthStencilState(CompareOp, bool))` |
| Blend | `withBlend(BlendFunction.TRANSLUCENT)` | `withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))` |
| Rejestracja | `RenderPipelines.register(...)` (LiquidBounce GUI) lub `precompilePipeline` po `ShaderLoader.apply` (Meteor) | `RenderPipelines.register(...)` (LiquidBounce) lub `compilePipeline` + `cache.insert` w `ShaderManager.apply` (Meteor) |
| Bufory | `createBuffer(...)`, `MeshBuilder.getVertexBuffer()` | + `encoder.transientMemory().uploadGpu(...)` → `GpuBufferSlice` |
| Uniformy dynamiczne | `DynamicUniformStorage` | `DynamicGpuDataStorageMapped` |
| Render pass | `createRenderPass(supplier, color, OptionalInt, ...)` | `createRenderPass(supplier, color, Optional<Vector4fc>, ...)` |
| Draw | `drawIndexed(0, 0, count, 1)` | `drawIndexed(count, 1, firstIndex, 0, 0)` |

## 5. Dwa sposoby dodania własnego rysowania do GUI

### 5.1 Własny element stanu GUI (preferowane dla HUD i widgetów)

Jest to wzorzec LiquidBounce w obu liniach [KOD]:

1. Zdefiniuj rekord implementujący `GuiElementRenderState` (np. z polami `x0, y0, x1, y1, radius, kolory, Matrix3x2f pose, ScreenRectangle scissorArea, ScreenRectangle bounds`).
2. W `buildVertices(VertexConsumer)` wyemituj wierzchołki w kolejności: lewy-górny, lewy-dolny, prawy-dolny, prawy-górny (dla `QUADS`) i wołaj `addVertexWith2DPose(pose, x, y)` a potem atrybuty (`setUv`, `setColor`, `setUv1`, `setUv2`, `setLineWidth`).
3. `pipeline()` zwraca Twój pipeline, `textureSetup()` daje `TextureSetup.noTexture()` lub konkretną teksturę.
4. Wyślij: `graphics.guiRenderState.submitGuiElement(state)` (1.21.11) / `.addGuiElement(state)` (26.x).
5. `bounds` policz jako `transformMaxBounds(pose)` przeciętą ze scissorem z `scissorStack.peek()` (wymaga class tweakera).

Zalety: wchodzisz w sortowanie warstw, scissor i batching gry.
Wady: dane elementu muszą się zmieścić w atrybutach wierzchołka.

LiquidBounce koduje rozmiar i promień jako dwie liczby całkowite w kanałach `UV1`/`UV2` (zakres do 32767) i obrys jako `LINE_WIDTH`. Dzięki temu jeden pipeline obsługuje wypełnienie i obrys. [KOD]

### 5.2 Własny renderer po GUI vanilli (Meteor)
Wzorzec: pozwól vanilli narysować swoje GUI, potem uruchom własny przebieg.

| | 1.21.11 | 26.x |
|---|---|---|
| Miejsce hooka | `GameRenderer.render`, `@Inject … INVOKE GuiRenderer.render(...) shift AFTER` | `GuiRenderer.render`, `@Inject … @At("TAIL")` |
| Czyszczenie stanu | `guiState.clear()` | druga instancja GuiRenderer (`MeteorMcGuiRenderer`) z własnym stanem |
| Nowy kontekst | `new DrawContext(client, guiState, mouseX, mouseY)` | `new GuiGraphicsExtractor(mc, renderState, mouseX, mouseY)` |
| Głębia | `clearDepthTexture(framebuffer.getDepthAttachment(), 1.0)` | `clearDepthTexture(mainRenderTarget().getDepthTexture(), 1.0)` |
| Spłukanie | `guiRenderer.render(fogRenderer.getFogBuffer(FogType.NONE))` + `incrementFrame()` | `guiRenderer.render()` + `guiRenderer.endFrame()` |

[KOD: `GameRendererMixin` w `4803071`, `GuiRendererMixin` w `e3a2e77`]

Obie wersje pracują w **pikselach fizycznych**: `Utils.unscaledProjection()` (26.x) lub skalowanie `1/getScaleFactor()` (1.21.11), dzięki czemu linie są ostre przy każdym GUI scale. [KOD]

## 6. Hooki renderowania świata (3D) [KOD]

- 1.21.11: `@Inject` w `GameRenderer.renderWorld` przed `profiler.swap("hand")`; w środku Meteor odkłada macierz widoku na `RenderSystem.getModelViewStack()`, buduje `Render3DEvent`, wysyła na magistralę i rysuje zebrane prymitywy własnymi pipeline'ami.
- 26.x: `@Inject` w `GameRenderer.renderLevel` przed `render3dHud(...)`; zdarzenie po świecie (`RenderAfterWorldEvent`) w `renderLevel` na `TAIL`.
- Alternatywa oficjalna: `WorldRenderEvents` (1.21.11) / `LevelRenderEvents` (26.x) z Fabric API (`BeforeEntities`, `AfterEntities`, `BeforeTranslucent`, `StartMain`, `EndMain`, `DebugRender`). [DOK]

Rada: dla rysowania w świecie najpierw spróbuj zdarzeń Fabric API; mixin dopiero, gdy nie dają potrzebnego punktu.

## 7. Przebiegi offscreen

Oba klienty tworzą własne cele renderowania. Wspólny schemat: [KOD]

1. Utwórz teksturę kolorową (`createTexture(label, flagi, format, w, h, 1, 1)`) i jej widok (`createTextureView`).
2. Przy zmianie rozdzielczości okna/framebuffera zamknij i odtwórz.
3. Rysuj do niej: `createRenderPass(..., colorView, clear)` z pipeline'em i pełnoekranowym quadem.
4. Skomponuj wynik na cel główny kolejnym przebiegiem z samplerem tej tekstury.

LiquidBounce opakowuje to w `LazyRenderTargetHolder` (tworzy cel dopiero przy pierwszym użyciu, z głębią lub bez). Meteor trzyma sześć tekstur o skali `1/2^i` do blura. [KOD]

## 8. Tekstura i sampler

- 1.21.11: `TextureFormat.RGBA8` / `RED8`, sampler z `RenderSystem.getSamplerCache().get(AddressMode, AddressMode, FilterMode, FilterMode, bool)`. [KOD]
- 26.x: `GpuFormat.RGBA8_UNORM`, a sampler np. `getClampToEdge(FilterMode.NEAREST)`. [KOD]
- Tekst: atlas jednokanałowy + shader mnożący alfę z tekstury przez kolor (Meteor); LiquidBounce ma własny `FontManager`, atlasy i `font_mask`. [KOD]
- Przeglądarka (CEF) w LiquidBounce: tekstury BGRA i **kolor premultiplikowany**, stąd `BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA` i osobny shader `bgra_position_tex_color.frag`. [KOD]

## 9. Scissor

- Stos scissorów w `GuiGraphics(Extractor)`: `enableScissor` / `disableScissor`, zagnieżdżalny (liczba wywołań musi się zgadzać). [DOK]
- Meteor ma własny stos (`Scissor` z puli), przycinający prostokąt dziecka do prostokąta rodzica (`x`, `width`, `y`, `height` ograniczone do wnętrza rodzica). [KOD]
- Na poziomie render passu: `pass.enableScissor(...)`. Meteor musiał w 26.x dodać mixiny na trzy backendy (`Gl/Vulkan/MetalDevice`), by wymusić globalny scissor dla swoich przebiegów (dozwolony tylko jeden naraz). [KOD]

## 10. Jak wybrać poziom API [SZKIC]

| Potrzeba | Poziom |
|---|---|
| Prostokąty, tekst, tekstury, gradient | `GuiGraphics(Extractor)` (poziom 1) |
| Zaokrąglone rogi, obrys, własny kształt w GUI | własny `GuiElementRenderState` + pipeline (poziom 2) |
| Własny ekran z efektami (blur, glow, kompozycja) | własny renderer po GUI vanilli + pipeline'y i offscreen (poziom 3) |
| Wygląd jak aplikacja webowa | UI w przeglądarce (poziom 4) |

Poziomy można mieszać; zasada: **im niżej, tym więcej własnego kodu i tym większe ryzyko, że kolejna wersja gry go złamie**.

## 11. Pułapki wspólne dla obu linii

1. Nie dotykaj surowego GL.
2. Nie trzymaj referencji do mutowalnego stanu w elemencie GUI (kopiuj `pose`).
3. Wszystkie tekstury offscreen odtwarzaj po zmianie rozmiaru okna.
4. Zerowa alfa + brak `discard` = „prostokątna poświata" wokół zaokrąglonych rogów.
5. Wsparcie `LINE_SMOOTH` nie istnieje w vanilli; Meteor dodaje własny mixin na `RenderPipeline`. [KOD]
6. Indeksy: offset bufora musi być wyrównany do rozmiaru typu indeksu (26.x). [KOD]
7. Nie zagnieżdżaj singletonowego `MeshRenderer`-a (jeśli piszesz podobny, nie rób z niego singletona).
8. Ścieżki, rozszerzenia i `#version` shaderów różnią się między liniami (plik 02, sekcja 8).
