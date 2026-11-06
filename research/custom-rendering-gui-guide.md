# Przewodnik: własny rendering GUI w Minecraft 1.21.11 (ścieżka C, „rysuj co chcesz")

- **Data:** 2026-11-06
- **Uzupełnia:** [minecraft-client-gui-1.21.11.md](minecraft-client-gui-1.21.11.md), sekcje 4.3 C, 4.4 i 4.5 (tam jest „co i dlaczego", tutaj „jak to się składa w praktyce").
- **Cel:** mieć własny silnik rysowania, który nie jest ograniczony przez `GuiGraphics`: własne shadery, formaty wierzchołków, tekstury, przebiegi offscreen i efekty pełnoekranowe.
- **Źródło faktów:** kod Meteor Client, gałąź `1.21.11`, commit `4803071` (2026-08-10), pliki w `renderer/`, `mixin/GameRendererMixin.java`, `mixin/ShaderLoaderMixin.java`, `systems/modules/render/Blur.java`, shadery w `assets/meteor-client/shaders/`. Przeczytałem je w tej sesji.

## Legenda pewności

| Znacznik | Znaczenie |
|---|---|
| **[KOD]** | Przeczytane w kodzie Meteora @`4803071`. Nic nie uruchamiałem ani nie kompilowałem. |
| **[SZKIC]** | Mój kod/projekt, niekompilowany, oparty na wzorcach z [KOD]. |
| **[WIEDZA]** | Z ogólnej wiedzy, niezweryfikowane. |

## Uwaga o nazwach (mapowania)

Gałąź `1.21.11` Meteora używa **nazw Yarn**, a w głównym raporcie zalecam **Mojang mappings**. Te same klasy mają różne nazwy. Wzorce są takie same, nazwy trzeba przetłumaczyć. Przykłady z tego, co widziałem [KOD] i co znam: [WIEDZA co do odpowiedników]

| Yarn (kod Meteora) | Mojang (zalecane w raporcie) |
|---|---|
| `MinecraftClient` | `Minecraft` |
| `DrawContext` | `GuiGraphics` |
| `VertexFormat.DrawMode` | `VertexFormat.Mode` |
| `net.minecraft.client.gl.Framebuffer` | `com.mojang.blaze3d.pipeline.RenderTarget` |
| `GameRenderer.renderWorld` / `render` | te same nazwy metod, inne klasy pomocnicze |

Klasy z pakietu `com.mojang.blaze3d.*` (`RenderPipeline`, `RenderPass`, `GpuBuffer`, `GpuTextureView`, `VertexFormat`, `RenderSystem`) mają w obu mapowaniach te same nazwy. [KOD]

---

## 1. Anatomia własnego renderera: 7 klocków

Wszystko, co Meteor rysuje poza vanillą, składa się z tych samych siedmiu elementów. Rozumiejąc je, możesz narysować cokolwiek.

| # | Klocek | Jak jest zrobiony w Meteorze [KOD] |
|---|---|---|
| 1 | **Format wierzchołków** | `VertexFormat.builder().add("Position", POS2).add("Color", VertexFormatElement.COLOR)…build()`. Własny element `POS2` (2 floaty) rejestrowany przez `VertexFormatElement.register(id, 0, Type.FLOAT, Usage.POSITION, 2)`. Wolny `id` szukany pętlą od 0; **limit 32 elementów w całej grze** (przy przekroczeniu wyjątek „Too many mods registering VertexFormatElements"). |
| 2 | **Pipeline** | Builder oparty o `RenderPipeline.Builder`: `withLocation`, `withVertexFormat(format, DrawMode)`, `withVertexShader`, `withFragmentShader`, `withSampler("u_Texture")`, `withUniform("Nazwa", UniformType.UNIFORM_BUFFER)`, `withDepthTestFunction`, `withDepthWrite`, `withBlend(BlendFunction.TRANSLUCENT)`, `withCull`. Wspólne uniformy trafiają do **snippetu** (`RenderPipeline.builder().withUniform(...).buildSnippet()`). |
| 3 | **Shadery** | Pliki `.vert` / `.frag` w `assets/<modid>/shaders/`, `#version 330 core`, atrybuty przez `layout (location = N) in …`, uniformy w blokach `layout (std140) uniform Nazwa { … }`. |
| 4 | **Prekompilacja** | Gra sama nie zna Twoich pipeline'ów. Mixin na `ShaderLoader.apply(…)` (`@At("TAIL")`) woła `device.precompilePipeline(pipeline, (identifier, shaderType) -> źródło-tekstowe)`, gdzie źródło czytane jest z `ResourceManager`. Dzięki hookowi pod koniec `apply` działa to też po przeładowaniu zasobów (F3+T). |
| 5 | **Bufory** | `MeshBuilder` buduje wierzchołki i indeksy do `GpuBuffer` (indeksy typu `INT`). Dla pełnoekranowego trójkąta/quada jest jeden współdzielony vbo+ibo (`FullScreenRenderer`). |
| 6 | **Uniformy** | Blok `MeshData { mat4 u_Proj; mat4 u_ModelView; }` zapisywany co rysowanie przez `DynamicUniformStorage` + `Std140Builder`; storage czyszczony raz na klatkę (`flipFrame`). Zwraca `GpuBufferSlice`, który wiążesz przez `pass.setUniform("MeshData", slice)`. |
| 7 | **Render pass** | `RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "nazwa", colorView, clearColor[, depthView, clearDepth])` → `setPipeline` → `setUniform(...)` → `bindTexture(nazwa, view, sampler)` → `setVertexBuffer(0, vbo)` → `setIndexBuffer(ibo, IndexType.INT)` → `drawIndexed(0, 0, liczbaIndeksów, 1)` → `close()`. |

Wniosek: **rysowanie nie jest ograniczone do GUI vanilli.** Masz zwykły potok GPU: bufory, shadery, tekstury, wiele przebiegów. Ograniczeniem jest tylko to, że robisz to przez abstrakcję Blaze3D, a nie surowy OpenGL.

## 2. Jak zrobić, żeby własne GUI rysowało się na wierzchu

Meteor rozwiązuje to w `GameRendererMixin` [KOD]:

1. `@Inject` w `GameRenderer.render`, w miejscu **bezpośrednio po** wywołaniu `GuiRenderer.render(...)` (`At.Shift.AFTER`), więc po całym GUI vanilli.
2. Gdy otwarty jest ekran Meteora (`WidgetScreen`): `guiState.clear()`, nowy `DrawContext`, wywołanie własnej metody `renderCustom(...)` ekranu.
3. Wyczyszczenie głębi: `createCommandEncoder().clearDepthTexture(framebuffer.getDepthAttachment(), 1.0)`.
4. Ręczne „spłukanie" stanu GUI: `guiRenderer.render(fogRenderer.getFogBuffer(FogType.NONE))` i `guiRenderer.incrementFrame()` (metoda `meteor$flushGuiState`).

Rysowanie w pikselach fizycznych: w `GuiRenderer.begin` macierz jest skalowana o `1 / window.getScaleFactor()`, więc linie są ostre niezależnie od GUI scale. [KOD]

Dwa ważne szczegóły [KOD]:
- Hook woła `guiState.clear()` **przed** własnym rysowaniem. Moja interpretacja: żeby przy ponownym „spłukaniu" nie narysować drugi raz tego, co vanilla już wyświetliła. [WIEDZA co do przyczyny]
- Pipeline'y Meteora do świata (3D) używają osobnego mixinu w `renderWorld` (przed `"hand"`), z własnymi macierzami `RenderSystem.getModelViewStack()`.

## 3. Pętla rysowania krok po kroku

Kolejność w `MeshRenderer.end()` [KOD], opisana własnymi słowami:

1. Zbuduj siatkę (wierzchołki + indeksy) w `MeshBuilder` albo użyj gotowych `GpuBuffer`.
2. Jeśli podano macierz, odłóż kopię na `RenderSystem.getModelViewStack()` (`pushMatrix`, `mul`), a po rysowaniu zdejmij (`popMatrix`).
3. Zapisz `MeshData` (projekcja + model-view) do dynamicznego bufora uniformów, weź slice.
4. Otwórz render pass na wskazanym celu (główny framebuffer lub własna tekstura). Głębię dołączaj **tylko** gdy `pipeline.wantsDepthTexture()`.
5. Ustaw pipeline, uniformy i samplery, bufory, zrób `drawIndexed`, zamknij pass.
6. Wyczyść stan renderera (singleton).

Konsekwencja [KOD]: `MeshRenderer` jest **singletonem z flagą `taken`**; `begin()` rzuca wyjątek, jeśli poprzednie rysowanie nie zostało zakończone `end()`. Nie zagnieżdżaj rysowań. Własny renderer zrób bez singletona, jeśli chcesz warstwy w warstwach.

## 4. Przebiegi offscreen i efekty (blur jako wzorzec)

Moduł `Blur` Meteora [KOD] pokazuje cały schemat „efekt pełnoekranowy":

- Tworzy **6 tekstur** (`device.createTexture("Blur - i", 15, TextureFormat.RGBA8, w, h, 1, 1)` + `createTextureView`), każda w skali `1 / 2^i` rozdzielczości okna.
- Przy zmianie rozdzielczości zamyka i tworzy je od nowa (zdarzenie `ResolutionChangedEvent`).
- Rysuje w pętli: **down-sampling** do coraz mniejszych tekstur, potem **up-sampling** z powrotem (shadery `blur_down.frag`, `blur_up.frag`), każdy przebieg to osobny `MeshRenderer` z `attachments(target, null)` i `fullscreen()`.
- Parametry (`u_HalfTexelSize`, `u_Offset`) idą w uniform buffer `BlurData`.
- Wynik jest komponowany na ekran shaderem `passthrough`.

Magiczna stała `15` w `createTexture(…)` to prawdopodobnie maska flag użycia tekstury; w kodzie nie jest nazwana. [WIEDZA co do znaczenia, niezweryfikowane]

Ten sam wzorzec obsługuje: rozmycie tła (frosted glass), poświatę (blur kształtu), cienie z miękką krawędzią, kompozycję warstw z przezroczystością i przejścia między ekranami.

## 5. Tekstury i tekst

- Klasa `Texture` [KOD] tworzy teksturę przez `createTexture`, widok przez `createTextureView`, sampler z `RenderSystem.getSamplerCache().get(AddressMode…, FilterMode min, FilterMode mag, false)`, a dane wgrywa przez `writeToTexture` z `NativeImage`. Obsługuje formaty `RGBA8` i `RED8`.
- Tekst: font TTF → atlas jednokanałowy `RED8` (filtr LINEAR) → pipeline `UI_TEXT`, w którym fragment shader mnoży kolor przez wartość z kanału czerwonego tekstury jako alfę. [KOD]
- Ikony: pakowane do jednego atlasu (`TexturePacker`) i rysowane jednym pipeline'em teksturowanym. [KOD]

## 6. Przepis: dowolny styl, krok po kroku [SZKIC]

> Poniższe to mój projekt na bazie wzorców z sekcji 1–5. Niczego nie skompilowałem.

**Krok 1: jeden pipeline „uber-box" zamiast wielu pipeline'ów.** Format wierzchołków: pozycja 2D, lokalne UV (0..1), kolor wypełnienia, kolor obramowania, oraz kilka kanałów parametrów (rozmiar, promienie czterech rogów, grubość obramowania, miękkość cienia). W jednym shaderze liczysz SDF prostokąta z promieniami per róg, obramowanie, cień i gradient; to pokrywa większość stylów z sekcji 4.5 głównego raportu.

Problem do rozwiązania na początku: Meteor rejestruje własny element tylko typu `POSITION` (`POS2`) i korzysta z gotowych `COLOR`/`UV`. Czy można zarejestrować kilka elementów o `Usage.GENERIC` z większą liczbą floatów, tego nie sprawdziłem. [WIEDZA, niezweryfikowane] Dwie drogi awaryjne:
- **Tekstura danych:** parametry elementu zapisujesz do małej tekstury `RGBA32F`/`RGBA8`, a w wierzchołku przekazujesz tylko indeks. Pewna i w pełni elastyczna, ale wymaga obsługi dużej liczby elementów.
- **Tablica struktur w UBO:** zapisujesz parametry wielu elementów w uniform buffer, a z wierzchołka przekazujesz tylko indeks (limit rozmiaru UBO ogranicza liczbę elementów na jedno rysowanie). [WIEDZA]

**Krok 2: szkic shadera fragmentów (SDF, pisany od zera).**

```glsl
#version 330 core
in vec2 v_Uv;            // 0..1 w obrębie prostokąta
in vec2 v_Size;          // szerokość/wysokość w pikselach
in vec4 v_Radii;         // promienie rogów: x=prawy-górny, y=prawy-dolny, z=lewy-dolny, w=lewy-górny
in vec4 v_Fill;
in vec4 v_Border;
in float v_BorderWidth;
out vec4 color;

float sdBox(vec2 p, vec2 b, vec4 r) {
    // wybór promienia po ćwiartce; zakładamy p.y rosnące ku górze
    // (przy UV rosnącym w dół zamień role góra/dół)
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
    vec4 c = mix(v_Fill, v_Border, inner) * vec4(1.0, 1.0, 1.0, shape);
    if (c.a <= 0.001) discard;
    color = c;
}
```

Kolejność promieni (który róg to `x/y/z/w`) trzeba ustalić zgodnie z tym, jak wypełnisz atrybut; wzór na wybór promienia po ćwiartce to standardowe rozszerzenie SDF. [WIEDZA]

**Krok 3: wyjście awaryjne „dowolny shader".** Skoro masz pętlę z sekcji 3, rysowanie jednego widgetu własnym shaderem to: osobny pipeline (z własnym blokiem uniformów), `MeshRenderer`-podobny przebieg z własnym uniform bufferem na ten widget. Tylko ten widget przerywa batch.

**Krok 4: efekty.** Blur tła, cień, poświata i przejścia: schemat z sekcji 4 (tekstury offscreen + dwa przebiegi + kompozycja).

**Krok 5: styl jako dane.** Motyw (JSON) + warstwa `UiGraphics` z sekcji 4.5 głównego raportu. Widgety nie znają szczegółów rysowania.

## 7. Pułapki znalezione w kodzie [KOD]

1. **Prekompilacja jest obowiązkowa.** Bez `precompilePipeline` (po `ShaderLoader.apply`) własny pipeline nie zadziała.
2. **Limit 32 własnych elementów formatu** w całej grze, współdzielony ze wszystkimi modami.
3. **Brak wsparcia vanilli dla `LINE_SMOOTH`.** Meteor dodaje to własnym mixinem na `RenderPipeline` (`IRenderPipeline`). Wygładzone linie wymagają więc ingerencji w wnętrza.
4. **`MeshRenderer` jest singletonem** (patrz sekcja 3), nie zagnieżdżaj rysowań.
5. **Głębia:** dołączaj ją do render passu tylko, gdy pipeline jej chce; po GUI czyść głębię, inaczej własne elementy mogą zostać przycięte.
6. **Uniformy dynamiczne:** `DynamicUniformStorage` trzeba czyścić raz na klatkę (`clear`/`flipFrame`), inaczej rośnie bez końca.
7. **Rozmiary tekstur przy zmianie okna:** wszystkie tekstury offscreen trzeba tworzyć na nowo (`ResolutionChangedEvent` w Meteorze).
8. **Kompatybilność:** Meteor ma osobne mixiny i warunkowe ładowanie dla Sodium, Iris itd. Własny rendering może z nimi kolidować.

## 8. Co zweryfikować przy pierwszym uruchomieniu

- [ ] Czy `RenderPipeline.Builder` w Twojej wersji (Mojang mappings) ma te same metody (`withSampler`, `withUniform`, `withBlend`…).
- [ ] Czy da się zarejestrować kilka własnych elementów wierzchołka o większej liczbie floatów (sekcja 6).
- [ ] Konwencja ścieżek shaderów (`shaders/*.vert` jak w Meteorze vs vanillowe `shaders/core`).
- [ ] Znaczenie flag w `createTexture(…, 15, …)`.
- [ ] Czy compute shadery są dostępne w `GpuDevice`.
- [ ] Wydajność uber-shadera i pasów blur na słabszym GPU.

## 9. Licencja

Meteor Client to **GPL-3.0**. Ten dokument opisuje wzorce i API oraz zawiera wyłącznie mój własny kod. Nie kopiuj kodu Meteora do własnego projektu, jeśli nie chcesz objąć go GPL-em.

## 10. Źródła

| Plik w Meteor @`4803071` | Co z niego |
|---|---|
| `renderer/MeshRenderer.java` | pętla render passu, singleton |
| `renderer/MeteorRenderPipelines.java` | definicje pipeline'ów, `precompile` |
| `renderer/MeteorVertexFormats.java`, `MeteorVertexFormatElements.java` | formaty i rejestracja elementów (limit 32) |
| `renderer/MeshUniforms.java` | `DynamicUniformStorage`, `Std140Builder` |
| `renderer/ExtendedRenderPipelineBuilder.java` | rozszerzenie buildera o `LINE_SMOOTH` |
| `renderer/Texture.java`, `FullScreenRenderer.java` | tekstury, współdzielony quad pełnoekranowy |
| `mixin/GameRendererMixin.java` | hooki po `GuiRenderer.render`, flush stanu GUI |
| `mixin/ShaderLoaderMixin.java` | prekompilacja po `ShaderLoader.apply` |
| `systems/modules/render/Blur.java` | schemat wieloprzebiegowego efektu |
| `assets/meteor-client/shaders/*` | konwencja shaderów (`pos_color`, `text`, `blur_down`) |
