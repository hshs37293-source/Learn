# Przewodnik: własny rendering GUI w Minecraft 26.x (ścieżka C, „rysuj co chcesz")

- **Data:** 2026-11-06
- **Cel wersji:** **26.3** (najnowsza linia w kodzie klientów). Dodatek A mówi, co zmieniło się względem 1.21.11.
- **Uzupełnia:** [minecraft-client-gui-1.21.11.md](minecraft-client-gui-1.21.11.md), sekcje 4.3 C, 4.4, 4.5 i 8 (tam jest „co i dlaczego", tutaj „jak to się składa w praktyce").
- **Dlaczego ta wersja:** pierwsza wersja przewodnika powstała na gałęzi `1.21.11` Meteora. To była stara linia: w 26.x zmieniło się całe API renderowania (patrz niżej). Ten dokument jest przepisany na podstawie **najnowszego kodu**.
- **Źródła faktów** (przeczytane w tej sesji):
  - **Meteor Client**, gałąź `master`, commit `e3a2e77` (2026-10-27).
  - **LiquidBounce**, gałąź `nextgen`, commit `2a3762a` (2026-11-06).
  - Pliki wymieniono w sekcji 10.

## Legenda pewności

| Znacznik | Znaczenie |
|---|---|
| **[KOD]** | Przeczytane w kodzie Meteora `e3a2e77` lub LiquidBounce `2a3762a`. Niczego nie uruchamiałem ani nie kompilowałem. |
| **[SZKIC]** | Mój kod/projekt, niekompilowany, oparty na wzorcach z [KOD]. |
| **[WIEDZA]** | Wniosek lub wiedza ogólna, niezweryfikowane. |

---

## 0. Najważniejsze zmiany w 26.x (co musisz wiedzieć na start)

1. **Nowa warstwa GPU: pakiet `com.mojang.renderpearl.api.*`.** `RenderPipeline`, `RenderPass`, `CommandEncoder`, `GpuDevice`, `GpuBuffer(Slice)`, `GpuTextureView`, `GpuSampler`, `VertexFormat`, `GpuFormat`, `IndexType` wszystkie mieszkają tam, a nie w `com.mojang.blaze3d.*`. [KOD]
2. **Gra ma wiele backendów graficznych.** Meteor ma osobne mixiny dla `GlDevice`, `VulkanDevice` i `MetalDevice` (m.in. do globalnego scissora). [KOD] Wniosek: **nie pisz kodu zależnego od OpenGL**; używaj wyłącznie `renderpearl.api`. [WIEDZA co do wniosku]
3. **Nazwy Mojang wszędzie.** 26.x nie ma obfuskacji (`Minecraft`, `GuiGraphicsExtractor`, `Identifier`, `PoseStack`, `RenderTarget`). [KOD]
4. **GUI to faza „extract".** Klasa nazywa się `GuiGraphicsExtractor`, metody `extractRenderState(...)`, a elementy dodajesz przez `guiRenderState.addGuiElement(...)` (w 1.21.11 było `submitGuiElement`). [KOD]
5. **Shadery:** pliki `.vsh` / `.fsh`, na początku `#version 330` oraz `#extension GL_ARB_separate_shader_objects : require`, a **wszystkie** `in`/`out` (także między vertex a fragment) mają jawny `layout(location = N)`. Działa `#include <minecraft:...>`. [KOD]

## 1. Anatomia własnego renderera w 26.x: 8 klocków

| # | Klocek | Jak jest zrobiony w kodzie [KOD] |
|---|---|---|
| 1 | **Format wierzchołków** | `VertexFormat.builder(stepRate).addAttribute("Nazwa", GpuFormat.X)…build()`. Przykłady: `RG32_FLOAT` (2 floaty), `RGBA8_UNORM` (kolor 4 bajty). Komentarz Meteora: „Step rate = 0 means per-vertex (not instanced)", co sugeruje, że niezerowy krok oznacza dane **per instancja**. LiquidBounce składa własne formaty z semantycznych nazw vanilli (`UV0/UV1/UV2`, `LINE_WIDTH`). **Nie ma już rejestracji `VertexFormatElement` ani limitu 32.** |
| 2 | **Pipeline** | `RenderPipeline.Builder`: `withLocation(id)`, `withVertexBinding(0, format)`, `withPrimitiveTopology(PrimitiveTopology.TRIANGLES / QUADS / DEBUG_LINES)`, `withVertexShader(id)`, `withFragmentShader(id)`, `withBindGroupLayout(...)`, `withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))`, `withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))`, `withCull(bool)`, `withShaderDefine("UI")`. |
| 3 | **Bind groups (uniformy i tekstury)** | `BindGroupLayout.builder().withUniform("MeshData", UniformType.UNIFORM_BUFFER).withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER).build()`; dołączane do pipeline'u (też jako część snippetu). Vanilla ma gotowe układy: `BindGroupLayouts.DYNAMIC_TRANSFORMS`, `PROJECTION`, `SAMPLER0`. |
| 4 | **Shadery** | `assets/<modid>/shaders/*.vsh|fsh`. Przykład z Meteora: ten sam shader obsługuje 2D i 3D przez `#ifdef UI` (definiowane `withShaderDefine("UI")`). |
| 5 | **Kompilacja pipeline'ów** | Dwie sprawdzone drogi, patrz sekcja 2. |
| 6 | **Bufory** | `device.createBuffer(() -> "nazwa", GpuBuffer.USAGE_VERTEX, dane)` albo przejściowo: `encoder.transientMemory().uploadGpu(dane, wyrównanie, USAGE_VERTEX)` → `GpuBufferSlice` (offset+długość). Uniformy dynamiczne: `DynamicGpuDataStorageMapped<>(nazwa, rozmiar, USAGE_UNIFORM, 16)`, zapis przez `Std140Builder`, czyszczenie raz na klatkę (`endFrame()`). |
| 7 | **Tekstury** | `device.createTexture(label, flagi, GpuFormat.RGBA8_UNORM, w, h, 1, 1)` + `createTextureView`; sampler z `RenderSystem.getSamplerCache()` (np. `getClampToEdge(FilterMode.NEAREST)`). |
| 8 | **Render pass** | `try (RenderPass pass = encoder.createRenderPass(() -> "nazwa", colorView, Optional<Vector4fc> clear[, depthView, OptionalDouble]))` → `pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline))` → `setUniform(nazwa, bufferSlice)` → `setUniform(nazwa, textureView, sampler)` → `setVertexBuffer(0, slice)` → `setIndexBuffer(buffer, IndexType.INT)` → `drawIndexed(indexCount, instanceCount, firstIndex, baseVertex, firstInstance)`. `RenderPass` jest `AutoCloseable`. |

Wniosek: **rysowanie nie jest ograniczone do GUI vanilli.** Masz zwykły nowoczesny potok GPU (bufory, bind groupy, wiele przebiegów, instancing w sygnaturze `drawIndexed`).

## 2. Dwie sprawdzone drogi rejestracji pipeline'u

### Droga 1: „niech vanilla skompiluje" (LiquidBounce) [KOD]
```
RenderPipeline.Builder().withLocation(id).…build()  →  RenderPipelines.register(pipeline)
```
Pipeline trafia do rejestru vanilli i jest kompilowany razem z resztą. Do elementów GUI dołącza się gotowy `RenderPipelines.GUI_SNIPPET` (lub `GUI_TEXTURED_SNIPPET`), który niesie macierze i układy bind group. **Najprostsza droga dla elementów dodawanych do GUI przez `GuiElementRenderState`.**

### Droga 2: „kompiluj sam" (Meteor) [KOD]
Mixin w `ShaderManager.apply` wstrzyknięty tuż przed `RenderSystem.setCurrentPipelineCache(...)` i przekazujący `PipelineCache` z `@Local`. W środku dla każdego pipeline'u:
1. Zbuduj `ShaderSource` (metody `getShader(id, type)` czytająca z `ResourceManager`, `getInclude(id)` oraz `close()`); includes z `ShaderManager.listAllIncludes(resources)`.
2. `device.compilePipeline(pipeline, shaderSource, executor)` → `CompletableFuture<CompiledRenderPipeline.Pending>`.
3. `.join().finishCompile()` → `cache.insert(pipeline, compiled)`.

Potem rysujesz przez `RenderSystem.getCompiledPipeline(pipeline)`. **Ta droga daje pełną kontrolę nad własnymi przebiegami** (offscreen, post-process) niezależnie od rejestru vanilli.

## 3. Jak rysować własne GUI na wierzchu

### Metoda A: element w pipeline'ie GUI vanilli (LiquidBounce) [KOD]
- Własny rekord implementujący `GuiElementRenderState` (np. `RoundedRectGuiElementRenderState`), w `buildVertices(VertexConsumer)` dla każdego wierzchołka: `addVertexWith2DPose(pose, x, y).setUv(...).setColor(...).setUv1(...).setUv2(...).setLineWidth(...)`.
- Metody `pipeline()` i `textureSetup()` zwracają Twój pipeline i ustawienie tekstur (`TextureSetup.noTexture()`).
- Wysyłka: `graphics.guiRenderState.addGuiElement(state)`. Dostęp do pól `guiRenderState` i `scissorStack` w `GuiGraphicsExtractor` przez access widener. [KOD: `liquidbounce.accesswidener`]
- Zalety: sortowanie warstw, scissor, `pose()` i batching działają same.
- Ograniczenie: dane elementu muszą wejść w atrybuty wierzchołków (LiquidBounce koduje rozmiar i promień jako `ivec2` w `UV1`/`UV2`, zakres do 32767).

### Metoda B: osobny renderer GUI po vanilli (Meteor) [KOD]
Mixin `GuiRendererMixin`, `@Inject` w metodzie `render` na `@At("TAIL")`:
1. Pominąć własną instancję (`MeteorMcGuiRenderer`), żeby się nie zapętlić.
2. `clearDepthTexture(mainRenderTarget().getDepthTexture(), 1.0)`.
3. Dla swojego ekranu: `new GuiGraphicsExtractor(mc, renderState, mouseX, mouseY)`, wywołanie własnego `renderCustom(...)` / zdarzenia `Render2DEvent`, potem `guiRenderer.render()`.
4. Na końcu `guiRenderer.endFrame()`.

Meteor ma **własną, drugą instancję GuiRenderer** i własne pipeline'y (`UI_COLORED`, `UI_TEXTURED`, `UI_TEXT`), a w `WidgetScreen.renderCustom` używa rzutowania „bez skalowania" (`Utils.unscaledProjection()`), czyli pracuje w pikselach fizycznych. Scissor idzie przez `graphics.enableScissor/disableScissor` z własnym stosem. [KOD]

**Którą wybrać?** Metoda A dla elementów mieszanych z GUI gry (HUD, własne widgety w vanilli). Metoda B dla w pełni własnego ekranu z własnymi pipeline'ami i efektami. Można łączyć. [WIEDZA co do rekomendacji]

## 4. Pętla rysowania (Metoda B) krok po kroku [KOD]

Z `MeshRenderer.end()` w Meteorze `master`, własnymi słowami:

1. Zbuduj siatkę w `MeshBuilder` albo użyj gotowych slice'ów.
2. Wyślij dane na GPU **przez encoder**: `mesh.uploadVertexBuffer(encoder)` i `uploadIndexBuffer(encoder)` (pamięć przejściowa, `transientMemory().uploadGpu`). Offset indeksów **musi być wyrównany** do rozmiaru typu indeksu, inaczej wyjątek.
3. Zapisz uniform `MeshData` (projekcja + model-view) do dynamicznego bufora.
4. Otwórz render pass na wskazanym celu (główny `RenderTarget` lub własna tekstura); głębię dołączaj tylko, gdy `pipeline.wantsDepthTexture()`.
5. Ustaw pipeline, uniformy, tekstury, bufory; `drawIndexed(indexCount, 1, firstIndex, 0, 0)` gdzie `firstIndex = offsetSlice / bajtyIndeksu`.
6. Zamknij pass (try-with-resources) i zdejmij macierz.

`MeshRenderer` jest nadal **singletonem z flagą `taken`**: nie zagnieżdżaj rysowań. [KOD] Własny renderer rób bez singletona, jeśli chcesz warstwy w warstwach.

## 5. Przebiegi offscreen i efekty

### Blur Meteora [KOD]
Wieloprzebiegowy: łańcuch tekstur o skali `1 / 2^i`, down-sampling potem up-sampling (`blur_down.fsh`, `blur_up.fsh`), parametry (`u_HalfTexelSize`, `u_Offset`) w uniform buffer `BlurData`, wynik kompozycją `passthrough`.

### Blur LiquidBounce [KOD]
`BlurEffectRenderer` używa `LazyRenderTargetHolder` (cel tworzony leniwie, z głębią lub bez), osobnych shaderów `ui_blur_h` i `ui_blur_v` (rozmycie rozdzielone na poziome i pionowe), samplera `clampToEdge(NEAREST)` i uniformu z min/max alfa; ma też efekty `OutlineShaderRenderer` i `OverlayShaderRenderer`.

Oba podejścia potwierdzają wzorzec: **cel offscreen → seria przebiegów z pełnoekranowym quadem → kompozycja**. Na nim zbudujesz szkło, glow, cienie, przejścia i dowolny post-process.

## 6. Tekst i tekstury

- Atlas tekstu jako tekstura jednokanałowa i shader mnożący alfę z tekstury przez kolor (`text.fsh`). [KOD]
- LiquidBounce ma własny `FontManager`, `FontFace`, atlasy (katalog `render/atlas`) oraz fragment shader `font_mask`. [KOD]
- W 26.x format tekstury to `GpuFormat.RGBA8_UNORM` itd. (zamiast `TextureFormat.RGBA8`). [KOD]

## 7. Przepis: dowolny styl, krok po kroku [SZKIC]

> Mój projekt na bazie wzorców z sekcji 1–5. Niczego nie skompilowałem.

**Krok 1: jeden pipeline „uber-box".** W 26.x nie ma już limitu rejestracji elementów, a `VertexFormat.builder(stepRate)` pozwala na dowolne atrybuty z `GpuFormat`. Format: pozycja 2D, lokalne UV, kolor wypełnienia (gradient góra/dół), kolor obramowania, atrybut z rozmiarem, atrybut z czterema promieniami rogów, atrybut z grubością obramowania i parametrem cienia. To usuwa największy problem z 1.21.11 (za mało miejsca na parametry w wierzchołku).

**Opcja zaawansowana: instancing** [WIEDZA, niezweryfikowane]. Skoro `VertexFormat.builder` ma „step rate" a `drawIndexed` przyjmuje liczbę instancji, możliwe jest: binding 0 = wspólny quad, binding 1 = parametry boxa na instancję. Nie widziałem tego w kodzie klientów, więc sprawdź, zanim polegasz.

**Krok 2: shader fragmentów (SDF, pisany od zera).**

```glsl
#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2  v_Uv;           // 0..1 w obrębie prostokąta
layout(location = 1) in vec4  v_Fill;
layout(location = 2) in vec4  v_Border;
layout(location = 3) flat in vec2  v_Size;    // piksele
layout(location = 4) flat in vec4  v_Radii;   // x=prawy-górny, y=prawy-dolny, z=lewy-dolny, w=lewy-górny
layout(location = 5) flat in float v_BorderWidth;
layout(location = 0) out vec4 color;

float sdBox(vec2 p, vec2 b, vec4 r) {
    // zakładamy p.y rosnące ku górze (przy UV rosnącym w dół zamień role góra/dół)
    r.xy = (p.x > 0.0) ? r.xy : r.wz;          // prawa strona : lewa strona
    float rr = (p.y > 0.0) ? r.x : r.y;        // góra : dół
    vec2 q = abs(p) - b + rr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rr;
}

void main() {
    vec2 hs = 0.5 * v_Size;                    // "half" jest słowem zarezerwowanym w GLSL
    float d = sdBox((v_Uv - 0.5) * v_Size, hs, v_Radii);
    float aa = max(fwidth(d), 1e-4);
    float shape = 1.0 - smoothstep(-aa, 0.0, d);
    float inner = smoothstep(-v_BorderWidth - aa, -v_BorderWidth + aa, d);
    vec4 c = mix(v_Fill, v_Border, inner);
    c.a *= shape;
    if (c.a <= 0.001) discard;
    color = c;
}
```

Wzór na wybór promienia po ćwiartce to standardowe rozszerzenie SDF. [WIEDZA] Vertex shader musi dostarczać wartości z tymi samymi `location`; dla GUI dołącz `#include <minecraft:dynamictransforms.glsl>` i `<minecraft:projection.glsl>` jak w shaderze zaokrąglonego prostokąta LiquidBounce (macierze `ModelViewMat`, `ProjMat`). [KOD]

**Krok 3: wyjście awaryjne „własny shader".** Dla jednego widgetu: osobny pipeline z własnym `BindGroupLayout` (własne uniformy i tekstury) i render pass z sekcji 4. Tylko ten widget przerywa batch.

**Krok 4: efekty.** Blur tła, cień, poświata i przejścia: schemat z sekcji 5.

**Krok 5: styl jako dane.** Motyw (JSON) + warstwa `UiGraphics` z sekcji 4.5 głównego raportu.

## 8. Pułapki znalezione w najnowszym kodzie [KOD]

1. **Tylko `renderpearl.api`.** Backendy (GL/Vulkan/Metal) różnią się; Meteor musi dublować mixiny przy każdym dotknięciu wnętrz urządzenia. Dla scissora używaj `graphics.enableScissor`/`pass.enableScissor`.
2. **`layout(location = N)` na każdym `in`/`out`**, razem z `GL_ARB_separate_shader_objects`.
3. **Kompilacja pipeline'u:** albo `RenderPipelines.register`, albo własny `compilePipeline` + `cache.insert` w hooku `ShaderManager.apply`. Bez tego pipeline nie zadziała.
4. **`MeshRenderer` jest singletonem.** Nie zagnieżdżaj rysowań.
5. **Wyrównanie bufora indeksów** do rozmiaru `IndexType` (offset slice'a), inaczej wyjątek.
6. **Pamięć przejściowa vs trwałe bufory:** `transientMemory().uploadGpu` dla danych na jedną klatkę, `createBuffer` dla trwałych (np. quad pełnoekranowy).
7. **Dynamiczne uniformy** trzeba zamykać raz na klatkę (`endFrame()`), inaczej rosną.
8. **Kolor premultiplikowany:** przeglądarkowe tekstury (CEF) wymagają `BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA` (tak robi LiquidBounce dla pipeline'ów `browser/*`).
9. **Kompatybilność z modami renderującymi** (Sodium, Iris): Meteor ma osobne mixiny, np. dla Sodium.

## 9. Co zweryfikować przy pierwszym uruchomieniu

- [ ] Dokładne sygnatury `RenderPipeline.Builder`, `VertexFormat.builder`, `BindGroupLayout` w Twoim buildzie 26.x.
- [ ] Czy niezerowy „step rate" daje instancing (sekcja 7) i czy `withVertexBinding` przyjmuje kolejne indeksy.
- [ ] Które wartości `GpuFormat` są dozwolone dla atrybutów wierzchołków (np. `RGBA32_FLOAT`).
- [ ] Znaczenie liczby `15` (flagi) w `createTexture(label, 15, …)` w kodzie Meteora.
- [ ] Wydajność uber-shadera i pasów blur na Vulkanie/Metalu, nie tylko GL.
- [ ] Czy compute shadery są dostępne w `renderpearl.api`.

## 10. Licencja

Meteor Client i LiquidBounce to **GPL-3.0**. Ten dokument opisuje wzorce i API oraz zawiera wyłącznie mój własny kod. Nie kopiuj kodu klientów do własnego projektu, jeśli nie chcesz objąć go GPL-em.

## 11. Źródła

| Plik | Co z niego |
|---|---|
| Meteor `e3a2e77`: `renderer/MeshRenderer.java`, `MeshBuilder.java`, `MeshUniforms.java` | pętla render passu, upload przez encoder, dynamiczne uniformy |
| Meteor: `renderer/MeteorRenderPipelines.java`, `MeteorVertexFormats.java` | definicje pipeline'ów, bind groupy, `compilePipeline`/`cache.insert` |
| Meteor: `mixin/ShaderManagerMixin.java`, `GuiRendererMixin.java`, `GlDeviceMixin.java`, `VulkanDeviceMixin.java` | hook kompilacji, hook GUI po vanilli, backendy |
| Meteor: `gui/WidgetScreen.java`, `assets/meteor-client/shaders/*.vsh|fsh` | ekran z `renderCustom`, konwencja shaderów |
| LiquidBounce `2a3762a`: `render/ClientRenderPipelines.kt`, `ClientVertexFormats.kt`, `RenderPassExtensions.kt` | rejestracja pipeline'ów, formaty, rozszerzenia render passu |
| LiquidBounce: `render/gui/element/RoundedRectGuiElementRenderState.java`, `Render2D.kt`, `liquidbounce.accesswidener` | element GUI z własnym pipeline'em, `addGuiElement`, dostęp do pól |
| LiquidBounce: `render/engine/BlurEffectRenderer.kt`, `shaders/gui/rounded_rect.vsh`, `shaders/blur/*` | blur wielotargetowy, shader GUI |

---

## Dodatek A: różnice 1.21.11 → 26.3 (dla porównania)

| Obszar | 1.21.11 (gałąź `1.21.11` Meteora) | 26.3 (`master`) |
|---|---|---|
| Pakiet GPU | `com.mojang.blaze3d.*` (`RenderPipeline`, `RenderPass`, `GpuBuffer`…) | `com.mojang.renderpearl.api.*` |
| Nazwy | Yarn (`MinecraftClient`, `DrawContext`, `Framebuffer`) | Mojang (`Minecraft`, `GuiGraphicsExtractor`, `RenderTarget`) |
| Format wierzchołków | `VertexFormat.builder().add("X", VertexFormatElement…)`, własne elementy rejestrowane z limitem 32 | `VertexFormat.builder(stepRate).addAttribute("X", GpuFormat.…)`, bez limitu |
| Uniformy i tekstury | `withUniform(…)`, `withSampler("u_Texture")` | `BindGroupLayout` z `UniformType.UNIFORM_BUFFER` / `COMBINED_IMAGE_SAMPLER` |
| Topologia/głębia/blend | `DrawMode`, `withDepthTestFunction`, `withDepthWrite`, `withBlend` | `PrimitiveTopology`, `DepthStencilState`, `ColorTargetState` |
| Kompilacja | `device.precompilePipeline(pipeline, (id, type) -> źródło)` po `ShaderLoader.apply` | `device.compilePipeline(…)` + `cache.insert(…)` w `ShaderManager.apply`, lub `RenderPipelines.register` |
| Rysowanie | `pass.setPipeline(pipeline)`, `bindTexture`, `drawIndexed(0, 0, count, 1)` | `setPipeline(getCompiledPipeline(…))`, `setUniform(name, view, sampler)`, `drawIndexed(count, 1, firstIndex, 0, 0)` |
| Wysyłka danych | `GpuBuffer` przez `createBuffer` | dodatkowo `transientMemory().uploadGpu` → `GpuBufferSlice` |
| Shadery | `.vert` / `.frag`, `layout(location)` tylko na atrybutach wejściowych | `.vsh` / `.fsh`, `layout(location)` na wszystkim, `#include <minecraft:…>` |
| GUI | `GuiGraphics`, `submitGuiElement` | `GuiGraphicsExtractor`, `addGuiElement` |
| Hook „na wierzchu" | `GameRenderer.render` po `GuiRenderer.render(...)`, ręczny flush stanu | `GuiRenderer.render` na `TAIL`, druga instancja GuiRenderer |
| Backendy | OpenGL | OpenGL, Vulkan, Metal |
