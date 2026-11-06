# Research: klient (mod Fabric) do Minecraft 1.21.11+ z nowoczesnym GUI

- **Data badania:** 2026-11-05
- **Cel:** zebrać wiedzę potrzebną do napisania w pełni funkcjonalnego, „desktopowego" klienta (moda Fabric) na Minecraft **1.21.11 i nowsze**, ze szczególnym naciskiem na ładne GUI: prostokąty, menu, **zaokrąglone rogi**.
- **Wykluczone na życzenie:** Wurst i inne stare, nieaktualizowane klienty. Badałem tylko projekty żywe: **Meteor Client** (gałąź `1.21.11`, aktywna do 2026-08) i **LiquidBounce nextgen** (ostatni commit 2026-11-04).
- **Zakres, którego NIE badałem:** omijanie anti-cheatów i detekcji serwerów. Raport dotyczy architektury i renderowania.

## Legenda pewności

Każde twierdzenie ma znacznik. Czytaj go zanim zaufasz kodowi.

| Znacznik | Znaczenie |
|---|---|
| **[KOD]** | Sprawdzone bezpośrednio w kodzie źródłowym (repo + commit w tabeli źródeł na końcu). |
| **[DOK]** | Z oficjalnej dokumentacji Fabric (repo `fabric-docs`, wersjonowane `1.21.11`). |
| **[WIEDZA]** | Moja wiedza ogólna. **Niezweryfikowana** w tej sesji. Traktuj jako hipotezę. |
| **[SKILL]** | Z bazy wiedzy modderskiej (skill `minecraft-modding`, tabela wersji „zweryfikowana 2026-10-05"). Nie sprawdzałem tego osobiście w tej sesji; zweryfikuj przed bumpem wersji. |
| **[SZKIC]** | Kod napisany przeze mnie na podstawie wzorców z [KOD]. **Nie był kompilowany ani uruchamiany.** |

---

## 0. TL;DR: decyzje, które warto podjąć od razu

1. **Platforma:** mod **Fabric** (Loader + Loom + Fabric API) na 1.21.11, **Java 21**, **Mojang mappings od pierwszego dnia**. Yarn jest martwy (Fabric: „Tribute to Fabric Yarn (2016-2025)"), a wersje 26.x w ogóle nie mają mapowań, bo gra jest bez obfuskacji. [DOK]
2. **1.21.11 to ostatnia wersja z obfuskacją.** 26.1+ to nowy plugin Loom, Java 25, inne nazwy, a „żadne mody dla 1.21.11 lub starszych nie działają na 26.1, nawet jako zależność compile-only". [DOK] Pisz kod z cienką warstwą abstrakcji nad renderowaniem, żeby migracja nie bolała (sekcja 8).
3. **Architektura klienta** (wzorzec Meteora i LiquidBounce): `Module` + `Setting` + magistrala zdarzeń + mixiny jako źródło zdarzeń + osobny system HUD + komendy + konfig + system dodatków (addonów). Szczegóły w sekcji 3.
4. **Zaokrąglone rogi robi się shaderem SDF**, nie teksturami. Jedno `RenderPipeline` + własny format wierzchołków + `GuiElementRenderState`. Tak robi LiquidBounce, a w 1.21.11 działa to przez `GuiGraphics.guiRenderState`. Per-element uniformy **nie działają** (GUI jest batchowane), więc promień i rozmiar idą jako atrybuty wierzchołków. [KOD]
5. **Cztery drogi do własnego GUI** (sekcja 4.3). Rekomendacja: **ścieżka B** (własne elementy w pipeline'ie GUI Minecrafta) + własne drzewo widgetów w podklasie `Screen`. Ścieżka D (UI w przeglądarce, jak LiquidBounce) daje najlepszy wygląd, ale to osobny, ciężki projekt.
6. **Licencje:** Meteor i LiquidBounce to **GPL-3.0**. Kopiowanie ich kodu wymusza GPL na Twoim kliencie. Uczyć się z architektury można, a kod pisz własny. [KOD]
7. **Dowolne GUI, 2D i 3D, na 1.21.11 i na najnowszej wersji** (Mojang mappings): tak, da się. Tryb „bez ograniczeń" (ścieżka C) plus własny pipeline 3D. Jak to zorganizować, żeby jeden projekt obsługiwał obie wersje, opisuje sekcja 4.5.
8. **Testuj GUI zrzutami ekranu** przez Fabric Client Game Tests (`takeScreenshot` / `assertScreenshotEquals`). [KOD/DOK]

---

## 1. Jak rozumiem zadanie

„Desktopowy, że będzie można zrobić wszystko" interpretuję jako: **kompletny klient w formie moda** (moduły, GUI, HUD, komendy, konfiguracja, rozszerzalność), z GUI o jakości aplikacji. Mod Fabric ma pełny dostęp do JVM i przez Mixin może zmodyfikować dowolną klasę gry, więc ograniczeniem jest jakość architektury, a nie możliwości platformy. [WIEDZA]

Alternatywna interpretacja: osobny launcher/aplikacja desktopowa poza grą. Tego nie badałem. Jeśli o to chodziło, daj znać.

---

## 2. Wersje i toolchain

### 2.1 Stan na 2026-11-05

| Element | 1.21.11 | 26.x (nowsze) |
|---|---|---|
| Najnowsza stabilna wersja w repozytoriach | – | **26.3** (przykładowy mod, Fabric API, Meteor `master` i LiquidBounce celują w 26.3); Fabric API ma już gałąź `26.4` (snapshot `26.4-snapshot-7`) [KOD] |
| Java | **21** [KOD: `release = 21`, `fabric.mod.json`: `java >=21`] | **25** [KOD: komentarz w `libs.versions.toml` LiquidBounce, [DOK] porting 26.1] |
| Obfuskacja | **tak** (ostatnia wersja) [DOK] | **brak** [DOK] |
| Plugin Loom | `net.fabricmc.fabric-loom-remap` [DOK/KOD] | `net.fabricmc.fabric-loom` [DOK] |
| Mapowania | `loom.officialMojangMappings()` (zalecane) lub Yarn (martwy) | brak linii `mappings`; `modImplementation` → `implementation`; `remapJar` → `jar` [DOK] |
| Nagłówek class tweakera | `classTweaker v1 named` [DOK] | `official` zamiast `named` [DOK] |
| Gałąź Fabric API | `1.21.11` → wersja `0.141.6` [KOD] | `26.3` → `0.165.x` [KOD] |

### 2.2 Konfiguracja z oficjalnego szablonu (gałąź `1.21.11` repo `fabric-example-mod`)

Wartości z `gradle.properties` tej gałęzi [KOD]:

```properties
minecraft_version=1.21.11
loader_version=0.20.1
loom_version=1.19-SNAPSHOT
fabric_api_version=0.141.6+1.21.11
```

`build.gradle` (skrót, [KOD]):

```groovy
plugins {
    id 'net.fabricmc.fabric-loom-remap' version "${loom_version}"
    id 'maven-publish'
}
loom {
    splitEnvironmentSourceSets()          // kod klienta w osobnym source secie
    mods { "modid" { sourceSet sourceSets.main; sourceSet sourceSets.client } }
}
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
}
tasks.withType(JavaCompile).configureEach { it.options.release = 21 }
```

Wrapper Gradle w tej gałęzi: `gradle-9.8.2`. [KOD]

**Uwaga o rozbieżnych wersjach.** Trzy źródła podają różne wersje narzędzi dla tego samego 1.21.11:

| Źródło | Loader | Loom |
|---|---|---|
| Meteor (gałąź `1.21.11`) [KOD] | 0.18.2 | 1.14-SNAPSHOT |
| LiquidBounce (commit `0a3d2a25`, ostatni na 1.21.11) [KOD] | 0.18.4 | 1.15-SNAPSHOT |
| Szablon `fabric-example-mod` (gałąź `1.21.11`) [KOD] | 0.20.1 | 1.19-SNAPSHOT |

Oficjalna strona z zalecanymi wersjami (`fabricmc.net/develop`) była **niedostępna** w tej sesji (patrz sekcja 9). Zacznij od wartości z szablonu i zweryfikuj build.

### 2.3 Układ projektu

Szablon [KOD]: `src/main` (wspólny kod), `src/client` (tylko klient), `modid.mixins.json` + `modid.client.mixins.json`, w `fabric.mod.json` osobne wpisy `main` i `client` w `entrypoints` oraz mixiny z `"environment": "client"`. Dla klienta całość możesz trzymać w `src/client` i ustawić `"environment": "client"` (tak robi Meteor). [KOD]

Meteor ładuje dodatkowe pliki mixinów dla integracji (Baritone, Sodium, Lithium, Indigo, ViaFabricPlus) oraz własny `MixinPlugin`, żeby włączać je warunkowo. [KOD]

### 2.4 Class tweakery (access wideners)

Potrzebne do dotknięcia prywatnych pól gry. Format i wymagania (Loader ≥ 0.18.0, Loom ≥ 1.12): [DOK]

```classtweaker
classTweaker v1 named
accessible field net/minecraft/client/gui/GuiGraphics guiRenderState Lnet/minecraft/client/gui/render/state/GuiRenderState;
accessible field net/minecraft/client/gui/GuiGraphics scissorStack Lnet/minecraft/client/gui/GuiGraphics$ScissorStack;
accessible class net/minecraft/client/gui/GuiGraphics$ScissorStack
accessible field net/minecraft/client/renderer/RenderPipelines GUI_SNIPPET Lcom/mojang/blaze3d/pipeline/RenderPipeline$Snippet;
accessible method com/mojang/blaze3d/pipeline/RenderPipeline$Builder <init> ()V
accessible method com/mojang/blaze3d/pipeline/RenderPipeline$Builder withSnippet (Lcom/mojang/blaze3d/pipeline/RenderPipeline$Snippet;)V
```

Linie `accessible ...` pochodzą z pliku `liquidbounce.accesswidener` z commitu `0a3d2a25` (1.21.11) [KOD]. Nagłówek i rejestrację pliku w `build.gradle`/`fabric.mod.json` bierz z [DOK] `class-tweakers/index.md`.

---

## 3. Anatomia nowoczesnego klienta

### 3.1 Porównanie dwóch żywych projektów

| | **Meteor Client** | **LiquidBounce nextgen** |
|---|---|---|
| Język | Java | Kotlin 2.4 |
| Wersja gry w repo (2026-11) | `master` → 26.3 (nazwy Mojang, Java 25), osobna gałąź `1.21.11` (ostatni commit 2026-08-09) [KOD] | 26.3 [KOD]; ostatni commit na 1.21.11: 2026-04-27 [KOD] |
| Mapowania na 1.21.11 | **Yarn** (`yarn 1.21.11+build.3`) [KOD] | **Mojang** [KOD] |
| Licencja | GPL-3.0 [KOD] | GPL-3.0 [KOD] |
| GUI | Własny silnik widgetów rysowany meshami (`gui/`, 134 pliki) [KOD] | UI w **przeglądarce** (Svelte 5 + Vite, katalog `src-theme`) + natywne elementy GUI w pipeline'ie Minecrafta [KOD] |
| Zaokrąglone rogi | **brak**, same prostokąty [KOD] | **shader SDF** (`rounded_rect`) [KOD] |
| Magistrala zdarzeń | Orbit (`@EventHandler`) [KOD] | własna, z korutynami (`event/`) [KOD] |
| Rozszerzalność | system addonów (`MeteorAddon`, szablon `meteor-addon-template`) [KOD] | motywy (themes), marketplace, skrypty, addon CEF [KOD] |
| Mixiny | ok. 200 plików w `mixin/` (łącznie z accessorami) [KOD] | katalog `injection/mixins` [KOD] |

### 3.2 Moduły i ustawienia

**Meteor** [KOD]: `Module` ma kategorię, nazwę (kebab-case), opis, `Settings`, `Keybind`, flagi (`runInMainMenu`, `autoSubscribe`, `toggleOnBindRelease`, `chatFeedback`, `favorite`) oraz `onActivate()`/`onDeactivate()`. `toggle()` subskrybuje moduł na magistralę zdarzeń przy włączeniu i wypisuje przy wyłączeniu. Stan i ustawienia serializują się do NBT (`toTag`/`fromTag`). Ustawienia buduje się wzorcem Builder, z grupami i warunkową widocznością:

```java
private final Setting<Boolean> keepSprint = sgGeneral.add(new BoolSetting.Builder()
    .name("keep-sprint").description("...").defaultValue(false).build());
// widoczność zależna od innego ustawienia:
.visible(() -> mode.get() == Mode.Rage)
```

**LiquidBounce** [KOD]: moduł to Kotlinowy `object ModuleSprint : ClientModule("Sprint", ModuleCategories.MOVEMENT)`, a ustawienia to delegaty właściwości (`private val sprintMode by enumChoice("Mode", SprintMode.LEGIT)`). Konfiguracja przez Gson (`config/gson`). Dużo zwięźlej niż Java.

### 3.3 Magistrala zdarzeń i mixiny

Mixiny są **źródłem zdarzeń**: wstrzykują się w grę i wołają `EVENT_BUS.post(...)`, a moduły reagują na zdarzenia (`TickEvent.Pre/Post`, `Render2DEvent`, `Render3DEvent`, `PacketEvent.Send/Receive` itd.). [KOD]

Przykład z Meteora: mixin w `GameRenderer.renderWorld` buduje macierze, tworzy `Render3DEvent`, wysyła je na magistralę i rysuje zebrane prymitywy. [KOD: `mixin/GameRendererMixin.java`]

Orbit wymaga rejestracji fabryki lambd per pakiet addonu, dlatego `getPackage()` w addonie musi być poprawne, inaczej gra się wysypie po włączeniu modułu. [KOD: komentarz w `ModuleExample.java`]

### 3.4 HUD, komendy, addony

- **HUD Meteora** [KOD]: `HudElement` z `HudBox` (kotwice `XAnchor`/`YAnchor`, snapping), własne `Settings`, edytor HUD w `systems/hud/screens`, gotowe elementy (`ActiveModulesHud`, `ArmorHud`, `InventoryHud`, `PlayerRadarHud`…).
- **HUD przez Fabric API** [KOD: `HudElementRegistry`, gałąź `1.21.11`]: `addFirst`, `addLast`, `attachElementBefore/After(id, ...)`, `removeElement`, `replaceElement`; element to `HudElement.render(GuiGraphics, DeltaTracker)`. Stare `HudRenderCallback` jest przestarzałe od Fabric API 0.116. [DOK]
- **Addony** [KOD: `meteor-addon-template`]: klasa dziedzicząca po `MeteorAddon` rejestruje moduły (`Modules.get().add(...)`), komendy, elementy HUD i kategorie. To najprostszy wzorzec na „wszystko da się dodać".
- **Klawisze** [DOK]: `KeyBindingHelper.registerKeyBinding(...)` + obsługa w `ClientTickEvents`.
- **Zdarzenia Fabric** przydatne w kliencie [KOD: gałąź `1.21.11`]: `ClientTickEvents.START/END_CLIENT_TICK`, `ScreenEvents.AFTER_INIT/beforeRender/afterRender`, `WorldRenderEvents`, `ClientLifecycleEvents`.

---

## 4. Silnik renderowania w 1.21.11: to, co musisz wiedzieć

### 4.1 Model „extract → draw"

Od 1.21.6 renderowanie dzieli się na fazę **ekstrakcji** (zbieranie danych do obiektów `RenderState`) i fazę **rysowania**. W 1.21.8 na ten model przeszło GUI: metody `GuiGraphics` tylko dodają elementy do stanu, a zapis do `BufferBuilder` dzieje się na końcu fazy przygotowania (`GuiRenderer#prepare`). [DOK]

Konsekwencje dla Ciebie:

- Metody `draw*`/`render*` wołasz w fazie ekstrakcji. Nie masz natychmiastowego rysowania w trybie immediate.
- Własny element musi być obiektem stanu (`GuiElementRenderState`), a nie wywołaniem GL.
- Stan GUI jest **batchowany po (pipeline, textureSetup)**, więc uniformów per element użyć nie można. [KOD: komentarz w `RoundedRectGuiElementRenderState`]

### 4.2 Fakty o API GUI w 1.21.11

| Fakt | Źródło |
|---|---|
| Główna klasa to `GuiGraphics` (w 26.x przemianowana na `GuiGraphicsExtractor`) | [DOK] 1.21.11 vs `main` |
| Stos macierzy GUI to `Matrix3x2fStack` (od 1.21.8), metody bez parametru `z` | [DOK] |
| Kolor tekstu to **ARGB** (od 1.21.6); przekazanie samego RGB da przezroczysty tekst; pomocnik: `ARGB.opaque(...)` | [DOK] |
| `ResourceLocation` nazywa się **`Identifier`** (`Identifier.fromNamespaceAndPath(...)`) | [KOD/DOK] |
| Prostokąty: `fill`, `fillGradient`, `renderOutline`, `hLine`, `vLine`; tekstury: `blit(RenderPipelines.GUI_TEXTURED, ...)`; clipping: `enableScissor`/`disableScissor` (zagnieżdżalne) | [DOK] |
| Rysowanie tylko prostokątów „kwadratowych"; dla innych kształtów trzeba `BufferBuilder`/własny pipeline | [DOK] |
| `Screen`: widgety tworzy się w `init()`, nie w konstruktorze; `addRenderableWidget`; `render(...)` wołaj z `super` | [DOK] |
| Własny `AbstractWidget` implementuje `renderWidget(...)` i `updateWidgetNarration(...)` | [DOK] |
| Animacje czasowe: `Util.getMillis()`; animacje związane z tickiem: `DeltaTracker.getGameTimeDeltaPartialTick(false)` (zamarza przy `/tick freeze`) | [DOK] |

### 4.3 Cztery drogi do własnego GUI

#### A. Czysty `GuiGraphics` + sprite'y 9-slice z zasobów

Najprostsze, w pełni zgodne z grą. Zaokrąglone rogi tylko przez teksturę, więc stały promień i brak antyaliasingu na dowolnej skali. 9-slice (`nine_slice` w `.mcmeta` sprite'a GUI) istnieje w vanilli od 1.20.2 **[WIEDZA, nie zweryfikowane]**. Dobre na szybki prototyp, słabe na „ładne" GUI.

#### B. Własne `GuiElementRenderState` + własny `RenderPipeline` (rekomendowane)

Tak działa LiquidBounce na 1.21.11: własne rekordy implementujące `GuiElementRenderState` (`CircleGuiElementRenderState`, `QuadGuiElementRenderState`, `LineGuiElementRenderState`…), wysyłane przez `guiGraphics.guiRenderState.submitGuiElement(...)`. Mixin `MixinGuiRenderState` recyklinguje macierze przy `reset`. [KOD]

Zalety: wchodzi w naturalną kolejność warstw gry, współpracuje ze scissorem, z pozycjami `pose()` i z batchingiem. Wada: trzeba dotknąć wewnętrznego API (access wideners), które zmieniło się w 26.x.

#### C. Własne meshe i render passy po wyrenderowaniu vanilli (Meteor)

Meteor ma własny `Renderer2D`/`MeshBuilder`/`MeshRenderer` i własne pipeline'y (`UI_COLORED`, `UI_TEXTURED`, `UI_TEXT`, shadery `.vert/.frag` w `assets/meteor-client/shaders`). Kluczowy trik na 1.21.11 [KOD: `GameRendererMixin`]:

1. Mixin w `GameRenderer.render` **tuż po** `GuiRenderer.render(...)` (czyli po całym GUI vanilli).
2. `guiState.clear()`, utworzenie nowego `DrawContext`, wywołanie `widgetScreen.renderCustom(...)`.
3. Wyczyszczenie bufora głębi (`clearDepthTexture`) i ręczne wywołanie `guiRenderer.render(...)` + `incrementFrame()` (metoda `meteor$flushGuiState`).

Dzięki temu własne GUI rysuje się **na wierzchu** wszystkiego. Ekran rysuje w pikselach fizycznych (`matrices.scale(1.0f / window.getScaleFactor())`), więc linie są ostre niezależnie od GUI scale. [KOD: `GuiRenderer.begin`]

Rysowanie to bezpośrednio `RenderPass` na głównym framebufferze: `createRenderPass(...)`, `setPipeline`, `setUniform("MeshData", ...)`, `bindTexture`, `setVertexBuffer`, `setIndexBuffer`, `drawIndexed`. [KOD: `MeshRenderer.java`] Pipeline'y trzeba prekompilować hookiem po `ShaderLoader`, bo gra sama ich nie zna (`device.precompilePipeline(pipeline, shaderSource)`). [KOD: `MeteorRenderPipelines.precompile`, `ShaderLoaderMixin`]

Zalety: pełna kontrola, batch całego okna w kilku draw-callach. Wada: najwięcej własnego kodu, więcej miejsc do rozjazdu z grą.

#### D. UI w przeglądarce (LiquidBounce)

GUI to aplikacja webowa (Svelte 5 + Vite, Inter, SCSS) renderowana przez wbudowaną przeglądarkę do tekstury. Klient wystawia lokalny serwer (Ktor): REST + WebSocket/SSE, przez który UI czyta i zapisuje moduły i ustawienia. Backend przeglądarki jest abstrakcją (`BrowserBackend`), a implementacja CEF to osobny addon (`LiquidBounce-Addon-CEF`); jest też wariant „zewnętrzna przeglądarka systemowa". Motywy ładuje się z URL-a i można je developować na żywo (`npm run dev` + `.client theme set <url>`). [KOD]

Zalety: CSS daje zaokrąglone rogi, cienie, blur i animacje „za darmo", motywowanie i hot reload; najbliżej „desktopowego" wyglądu. Wady: natywne zależności per system operacyjny, osobny stos technologiczny, opóźnienie wejścia, trudniejsza dystrybucja jednym jarem.

**Rekomendacja:** B dla HUD i paneli, w podklasie `Screen` z własnym drzewem widgetów. Jeśli priorytetem jest efekt „wow" i szybka iteracja nad wyglądem, rozważ D jako projekt równoległy, ale wiedząc, co kosztuje.

### 4.4 Tryb „bez ograniczeń": własny silnik renderowania (ścieżka C rozwinięta)

Jeśli priorytetem jest **dowolne GUI, bez ram narzuconych przez `GuiGraphics`**, wybierz ścieżkę C i zbuduj własny silnik 2D. Co jest dowiedzione w kodzie Meteora (1.21.11), a co jest moją propozycją:

| Możliwość | Status |
|---|---|
| Dowolne shadery GLSL 330 i dowolne `RenderPipeline` (własny format wierzchołków, blending, uniform buffery, samplery) | [KOD] `MeteorRenderPipelines`, `MeshRenderer` |
| Własne tekstury i render targety poza ekranem (`device.createTexture(...)`, `createTextureView`) | [KOD] `Texture.java`, `Blur.java` |
| Wieloprzebiegowe efekty: łańcuch tekstur mniejszych/większych, rysowanie do wskazanego celu (`attachments(target, null)`) | [KOD] `Blur.java` (down/up-sampling w pętli) |
| Efekty pełnoekranowe (post-process: outline, obraz, blur) | [KOD] pipeline'y `POST_OUTLINE`, `POST_IMAGE`, `BLUR_*` |
| Własny atlas tekstu (TTF → STB) i atlas ikon (`TexturePacker`) | [KOD] `Font.java`, `GuiRenderer.java` |
| Rysowanie na wierzchu całego GUI vanilli, w pikselach fizycznych | [KOD] `GameRendererMixin`, `GuiRenderer.begin` |
| Własna pętla wejścia (`Screen` przechwytuje mysz/klawiaturę, własne drzewo widgetów) | [KOD] `WidgetScreen` |
| **Uber-shader** z kształtami SDF wybieranymi atrybutem (prostokąt, okrąg, linia, obraz, tekst) w jednym batchu | [SZKIC/propozycja], wzorzec atrybutów wierzchołków z sekcji 5.3 |
| **Tekst MSDF** (ostry przy dowolnym skalowaniu) zamiast zwykłego atlasu | [WIEDZA], nie sprawdzałem w kodzie klientów |
| Cienie, szkło (frosted glass), maski, przejścia między ekranami, cache warstw do tekstur | możliwe na bazie powyższych klocków; propozycja |

**Zalecana struktura silnika:**

1. `UiRenderer` zbiera polecenia (prostokąt, tekst, obraz, clip, warstwa) do jednej listy w trakcie ekstrakcji.
2. Na końcu klatki sortuje je w warstwy, układa w kilka dużych meshy (po pipeline'ie i teksturze) i rysuje kilkoma `drawIndexed`.
3. Warstwy z efektami (blur tła, cień) trafiają najpierw do tekstur offscreen, a potem są komponowane.
4. Wejście wstrzykujesz w `Screen`, a rysowanie w mixinie za `GuiRenderer.render` (wzorzec z sekcji 4.3 C).

**Co nadal Cię ogranicza (bez względu na ścieżkę):**

- W 1.21.11 rysowanie idzie przez abstrakcję Blaze3D (`GpuDevice`, `RenderPass`, `CommandEncoder`). Surowe wywołania OpenGL są nieprzewidywalne, więc trzymaj się tego API. [DOK: ostrzeżenie w `basic-concepts.md`]
- Nie sprawdzałem, czy API pozwala na compute shadery. [WIEDZA/niezweryfikowane]
- Każda aktualizacja gry (1.21.11 → 26.x) zmienia wnętrza, więc własny silnik trzeba utrzymywać i izolować w jednym pakiecie (patrz sekcja 8).
- Inne mody renderujące (Sodium, Iris) mogą kolidować. Meteor ma na to osobne mixiny kompatybilności. [KOD]

**Gdy chcesz wyglądu bez żadnych granic** (CSS, SVG, animacje jak w aplikacji), ścieżka D (UI w przeglądarce) daje największą swobodę wizualną kosztem ciężkich zależności natywnych.

---

### 4.5 Dowolne GUI (2D i 3D) na 1.21.11 i na najnowszej wersji, w Mojang mappings

**Cel:** móc zbudować *jakiekolwiek* GUI, płaskie albo przestrzenne, na **1.21.11** oraz na **najnowszym wydaniu**, zawsze na nazwach Mojanga (bez Yarn i bez intermediary).

#### Co to znaczy „najnowsza wersja" i „Mojang mappings"

| Wersja | Java | Nazwy w kodzie | Remap przy buildzie | Źródło |
|---|---|---|---|---|
| **1.21.11** | 21 | Mojang mappings (oficjalne), gra jest jeszcze obfuskowana | tak, Loom mapuje na czas działania | [DOK] sekcja 2 |
| **26.1 i nowsze** (stabilnie do **26.3**; 26.4 było tylko w snapshotach na 2026-10-05) | **25** | nazwy Mojanga są **natywne**, gra jest bez obfuskacji | **nie** (bez `remapJar`, bez intermediary, bez Yarn) | [SKILL] |

Dla 26.x Fabric Loader to u nich wszystkich 0.19.5, a Fabric API ma tag `+<wersja MC>` (np. `0.161.0+26.3`). [SKILL] Numery dryfują, a od 2026-10-05 mogło wyjść coś nowszego niż 26.3. Przed startem sprawdź aktualną wersję w `meta.fabricmc.net` i manifeście Mojanga. W tej sesji nie miałem do nich dostępu (patrz sekcja 9).

#### Jeden projekt, dwie wersje: układ

Kodu GUI nie da się skompilować raz na obie wersje, bo między 1.21.11 a 26.x zmieniają się klasy renderowania (sekcja 8). Dlatego:

```
core/            logika klienta, widgety, motyw, layout: BEZ importów z net.minecraft.client.renderer
ui-api/          interfejsy: UiGraphics, Mesh3D, Camera3D, InputSource
backend-1_21_11/ implementacja na 1.21.11 (GuiGraphics, GuiElementRenderState, Java 21)
backend-26_x/    implementacja na najnowszą (GuiGraphicsExtractor, nowe pakiety renderowania, Java 25)
```

- Dwa osobne moduły Gradle, każdy z własnym Loomem (1.21.11: plugin `fabric-loom` z Mojang mappings; 26.x: nowy plugin bez mapowań, Java 25). Jeden wspólny `core`/`ui-api` skompilowany na Javie 21 (bajtkod ładuje się na 25). [SKILL + DOK, SZKIC układu]
- W `fabric.mod.json` dwa JAR-y z zależnością `minecraft` (`~1.21.11` oraz `>=26.1`) albo dwa buildy tego samego kodu. Fabric nie zadziała z jednym JAR-em na obie wersje, jeśli dotykasz wnętrz renderera. [WIEDZA]
- Każdą wersję najpierw **kompiluj od zera** (`clean compileJava`), bo kompilator potrafi „zdradzić" liczbą błędów. Dla nowej wersji różnice wyciągaj przez `javap` na jarze docelowego Minecrafta, a nie z pamięci. [SKILL]

#### 2D: co możesz zrobić dowolnie

Wszystko z sekcji 4.4 i 5: własne `RenderPipeline`, shadery SDF (zaokrąglone rogi, cienie, szkło), własny tekst, warstwy offscreen, rysowanie nad całym GUI vanilli w pikselach fizycznych. Dotyczy:

- pełnych ekranów (`Screen`), nakładek HUD (`HudElementRegistry`), okien przeciąganych wewnątrz jednego ekranu, edytora HUD,
- GUI w przeglądarce (ścieżka D), jeśli wolisz CSS zamiast własnego silnika.

Na 26.2 i dalej GUI jest już w pełni „retained": `GuiGraphics` jest przemianowane na `GuiGraphicsExtractor` i tylko zbiera elementy. [SKILL + DOK] Na obu wersjach element musi być obiektem stanu, nie wywołaniem GL.

#### 3D: trzy realne warianty [WIEDZA + SZKIC, nie testowałem żadnego]

1. **Obiekt 3D wewnątrz GUI 2D** (podgląd modelu, gracza, przedmiotu, wykres 3D). Renderujesz scenę do **tekstury offscreen** (własny `RenderPass` + kamera z własną macierzą projekcji i głębią), a potem rysujesz tę teksturę jako zwykły prostokąt w GUI. Odporne na zmiany GUI, bo 3D jest odizolowane. Vanilla robi podobnie dla podglądu encji w ekwipunku, ale tamtejsze API zmieniało się między wersjami, więc własny render-to-texture jest stabilniejszy. [WIEDZA]
2. **GUI w świecie** (panel zawieszony w przestrzeni, billboard nad blokiem, hologram). Rysujesz w fazie renderowania świata własnym pipeline'em z głębią i macierzami kamery. Wzorzec „render przez ściany" masz w `fabric-docs` `rendering/world.md` (wersja 1.21.11). [DOK] Panel może być płaskim quadem z teksturą, na którą wcześniej wyrenderowałeś zwykłe GUI (wariant 1 odwrócony: 2D → tekstura → quad 3D).
3. **Cały ekran 3D** (menu jako scena, obracające się karty, głębia parallax). `Screen` bez tła vanilli, a w jego `render` własny pipeline z macierzą perspektywy i buforem głębi, wyczyszczonym przed rysowaniem 3D (to samo `clearDepthTexture`, które robi Meteor w sekcji 4.3 C). Wejście myszy przeliczasz przez odwróconą macierz na promień. [WIEDZA + SZKIC]

Wspólne zasady dla 3D: własny format wierzchołków (pozycja `vec3`, normalna, UV, kolor), własny shader, głębia włączona tylko dla tego przebiegu, po nim przywróć stan, żeby nie zepsuć GUI vanilli. Kolizje z Sodium/Iris omówione w sekcji 8 dotyczą także 3D.

#### Interfejs, który to spina [SZKIC, nie kompilowany]

```java
// ui-api: kod GUI nigdy nie widzi klas Minecrafta
public interface UiGraphics {
    void roundedRect(float x, float y, float w, float h, float radius, int argb);
    void text(String s, float x, float y, int argb, float size);
    void texture(UiTexture t, float x, float y, float w, float h);
    void pushClip(float x, float y, float w, float h);
    void popClip();
    void scene3d(Scene3D scene, float x, float y, float w, float h); // render-to-texture
}

public interface WorldUi { // wariant 2: GUI w przestrzeni świata
    void panel(Vec3 pos, Quat rot, float widthMeters, UiPanel content);
}
```

Backend 1.21.11 i backend 26.x implementują te same interfejsy innymi klasami Minecrafta. Dzięki temu widgety, motyw i logika są napisane raz.

#### Co jest pewne, a co nie

- **Pewne** [DOK/KOD]: dowolne shadery, pipeline'y, offscreen i rysowanie nad GUI działają na 1.21.11 (dowód: Meteor, LiquidBounce).
- **Wzięte z bazy wiedzy** [SKILL]: tabela wersji 26.x (Java 25, brak remapu, GuiGraphicsExtractor).
- **Hipotezy** [WIEDZA/SZKIC]: warianty 3D, interfejsy powyżej, pojedynczy wspólny `core`. Nie mam jara Minecrafta ani kompilacji, więc przed pracą zrób mały prototyp „czerwony trójkąt 3D w GUI" na obu wersjach.
- **Nie sprawdzałem**, czy w najnowszej wersji (26.3, a tym bardziej nowszych) nadal działa wzorzec hooka za `GuiRenderer.render` z 4.3 C. W 26.3 `RenderPipeline` leży w innym pakiecie (sekcja 8), więc backend 26.x napisz od nowa, a nie portuj przez zamianę nazw.

---

## 5. Zaokrąglone rogi

### 5.1 Techniki

| Technika | Plusy | Minusy |
|---|---|---|
| Tekstura 9-slice | prosta, vanilla | stały promień, bez AA na każdej skali |
| Geometria (wachlarz trójkątów w rogach) | bez shadera | dużo wierzchołków, AA wymaga MSAA/obwódek |
| **Shader SDF** (signed distance field) | dowolny promień, idealny AA, obramowanie/gradient/cień z tego samego pola | wymaga własnego pipeline'u |
| Przeglądarka (CSS) | wszystko za darmo | ścieżka D |

### 5.2 Matematyka SDF

Dla prostokąta o połowie rozmiaru `b` i promieniu `r`, w układzie środka (`p` = punkt względem środka):

```
q = abs(p) - b + r
d = length(max(q, 0)) + min(max(q.x, q.y), 0) - r      // d < 0 wewnątrz
alpha = 1 - smoothstep(-aa, 0, d),  aa = fwidth(d)      // AA na szerokość ok. 1 piksela
```

Jest to standardowy wzór signed-distance dla zaokrąglonego prostokąta (znany m.in. z materiałów Inigo Quilez) **[WIEDZA co do pochodzenia]**; LiquidBounce używa identycznej postaci (`sdRoundBox`) w `gui/rounded_rect.fsh`. [KOD]

Warianty zaobserwowane w LiquidBounce [KOD]:

- **Obramowanie:** ten sam `d`, ale `alpha = edge * inner`, gdzie `inner = smoothstep(-w - aa, -w + aa, d)`, a `w` to grubość linii.
- **Gradient pionowy:** kolor górny i dolny w wierzchołkach, interpolowany w fragmentach.
- **Elipsy / różne promienie X i Y:** wersja „anizotropowa" `sdRoundBoxAniso` dzieli współrzędne przez promień.
- **`discard` przy `alpha ≈ 0`** oszczędza zapis do framebuffera.

### 5.3 Integracja na 1.21.11 (ścieżka B) [SZKIC]

> **Ostrzeżenie:** poniższy kod to mój szkic ze wzorców LiquidBounce@`0a3d2a25` (1.21.11). Nie miałem JAR-a Minecrafta, więc **niczego nie skompilowałem**. Sygnatury (zwłaszcza `submitGuiElement`, `setUv1/2`, `TextureSetup.noTexture()`, konwencja ścieżek shaderów) zweryfikuj przy pierwszej kompilacji. Shader jest napisany od podstaw ze standardowego wzoru, a nie skopiowany (licencja GPL, sekcja 8).

**1) Format wierzchołków.** Dane elementu idą w atrybutach, bo uniformy per element odpadają (batching):

```java
public static final VertexFormat GUI_ROUNDED_RECT = VertexFormat.builder()
    .add("Position", VertexFormatElement.POSITION)
    .add("UV0",      VertexFormatElement.UV0)    // lokalne UV 0..1 w obrębie prostokąta
    .add("Color",    VertexFormatElement.COLOR)  // ARGB
    .add("UV1",      VertexFormatElement.UV1)    // (szerokość, wysokość) w jednostkach GUI, 2x short
    .add("UV2",      VertexFormatElement.UV2)    // x = promień w jednostkach GUI, 2x short
    .build();
```

Wzorzec `VertexFormat.builder().add("UV2", VertexFormatElement.UV2)` oraz kodowanie liczb całkowitych w `UV1/UV2` (zakres do 32767) pochodzą z `ClientVertexFormats.kt` i `CircleGuiElementRenderState.java` na 1.21.11. [KOD]

**2) Stan elementu.** Rekord implementujący `GuiElementRenderState`:

```java
public record RoundedRectState(
        float x0, float y0, float x1, float y1, float radius, int argb,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

    @Override public void buildVertices(VertexConsumer v) {
        int w = Math.round(x1 - x0), h = Math.round(y1 - y0), r = Math.round(radius);
        v.addVertexWith2DPose(pose, x0, y0).setUv(0, 0).setColor(argb).setUv1(w, h).setUv2(r, 0);
        v.addVertexWith2DPose(pose, x0, y1).setUv(0, 1).setColor(argb).setUv1(w, h).setUv2(r, 0);
        v.addVertexWith2DPose(pose, x1, y1).setUv(1, 1).setColor(argb).setUv1(w, h).setUv2(r, 0);
        v.addVertexWith2DPose(pose, x1, y0).setUv(1, 0).setColor(argb).setUv1(w, h).setUv2(r, 0);
    }
    @Override public RenderPipeline pipeline() { return MyPipelines.ROUNDED_RECT; }
    @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
}
```

Kolejność wierzchołków (lewy-górny → lewy-dolny → prawy-dolny → prawy-górny) i użycie `addVertexWith2DPose(pose, x, y)` pochodzą z `CircleGuiElementRenderState` (1.21.11). [KOD]

**3) Wysłanie elementu.** Macierz trzeba skopiować (`new Matrix3x2f(g.pose())`), bo stan żyje dłużej niż wywołanie, a `bounds` policzyć z uwzględnieniem scissora:

```java
public static void roundedRect(GuiGraphics g, float x0, float y0, float x1, float y1, float r, int argb) {
    ScreenRectangle rect = new ScreenRectangle((int) x0, (int) y0,
            (int) Math.ceil(x1 - x0), (int) Math.ceil(y1 - y0)).transformMaxBounds(g.pose());
    ScreenRectangle scissor = g.scissorStack.peek();
    ScreenRectangle bounds = scissor != null ? scissor.intersection(rect) : rect;
    g.guiRenderState.submitGuiElement(
        new RoundedRectState(x0, y0, x1, y1, r, argb, new Matrix3x2f(g.pose()), scissor, bounds));
}
```

Wzorzec `getBounds` (transformacja prostokąta przez `pose()` + przecięcie ze `scissorStack.peek()`) i `copyPose` pochodzą z `Render2D.kt`@`0a3d2a25`. [KOD] Pola `guiRenderState` i `scissorStack` wymagają class tweakera z sekcji 2.4.

**4) Pipeline.** Wzorzec rejestracji pochodzi z oficjalnej dokumentacji: `RenderPipelines.register(RenderPipeline.builder(SNIPPET).withLocation(Identifier...).with...().build())`. [DOK] LiquidBounce@1.21.11 dla GUI dobiera `RenderPipelines.GUI_SNIPPET` + własne shadery + `withVertexFormat(format, VertexFormat.Mode.QUADS)`. [KOD]

```java
public static final RenderPipeline ROUNDED_RECT = RenderPipelines.register(
    RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/gui_rounded_rect"))
        .withVertexShader(Identifier.fromNamespaceAndPath(MOD_ID, "core/gui_rounded_rect"))
        .withFragmentShader(Identifier.fromNamespaceAndPath(MOD_ID, "core/gui_rounded_rect"))
        .withVertexFormat(GUI_ROUNDED_RECT, VertexFormat.Mode.QUADS)
        .build());
```

Gdzie leżą pliki shaderów? Dwie praktyki z kodu: Meteor trzyma `assets/<ns>/shaders/*.vert|frag` i prekompiluje przez własny lookup w `ResourceManager`; LiquidBounce podaje źródła przez własny `ShaderSource`. Konwencja vanilli (`shaders/core/*.vsh`) **[WIEDZA, niezweryfikowana]**. Sprawdź przy pierwszym uruchomieniu.

**5) Shadery (standardowy SDF, pisane od zera) [SZKIC].** Styl deklaracji bloków `DynamicTransforms`/`Projection` i atrybutów (`in ivec2 UV2`, `flat out`) zgodny z shaderami LiquidBounce@1.21.11. [KOD]

Vertex:

```glsl
#version 330
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; vec4 ColorModulator; vec3 ModelOffset; mat4 TextureMat; };
layout(std140) uniform Projection { mat4 ProjMat; };
in vec3 Position; in vec2 UV0; in vec4 Color; in ivec2 UV1; in ivec2 UV2;
out vec2 vUv; out vec4 vColor; flat out vec2 vSize; flat out float vRadius;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vUv = UV0; vColor = Color; vSize = vec2(UV1); vRadius = float(UV2.x);
}
```

Fragment:

```glsl
#version 330
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; vec4 ColorModulator; vec3 ModelOffset; mat4 TextureMat; };
in vec2 vUv; in vec4 vColor; flat in vec2 vSize; flat in float vRadius;
out vec4 fragColor;
float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}
void main() {
    vec2 b = 0.5 * vSize;
    float r = min(vRadius, min(b.x, b.y));
    float d = sdRoundBox((vUv - 0.5) * vSize, b, r);
    float aa = max(fwidth(d), 1e-4);
    vec4 c = vColor * ColorModulator;
    c.a *= 1.0 - smoothstep(-aa, 0.0, d);
    if (c.a <= 0.001) discard;
    fragColor = c;
}
```

Ponieważ `d` jest w jednostkach GUI, a `fwidth(d)` w „jednostkach GUI na piksel", antyaliasing jest poprawny przy każdym GUI scale. [WIEDZA, wniosek z konstrukcji]

### 5.4 Rozszerzenia warte zrobienia

- **Obramowanie** i **gradient** (w jednym elemencie, jak `RoundedRectGuiElementRenderState` z LiquidBounce 26.3 [KOD]).
- **Cień / poświata:** większy quad pod panelem + `alpha = exp(-k*d)` lub `smoothstep` po ujemnym `d` poza krawędzią. [WIEDZA]
- **Rozmycie tła (frosted glass):** LiquidBounce ma shadery `blur/ui_blur_h.frag` i `ui_blur_v.frag` (rozmycie rozdzielone na przebieg poziomy i pionowy), a Meteor `blur_down.frag`/`blur_up.frag` (z nazw: rozmycie dwufazowe przez zmniejszanie i powiększanie). Treści shaderów blur nie analizowałem. [KOD]
- **Różne promienie w czterech rogach:** wybór promienia po ćwiartce `p`, standardowe rozszerzenie SDF. [WIEDZA]
- **Okręgi, suwaki, przełączniki, kropki statusu:** LiquidBounce ma osobny pipeline z paletą kolorów w teksturze LUT (`gui_circle_lut`). [KOD]

### 5.5 Pułapki

- Brak `discard`/zerowy alpha daje „prostokątną poświatę" wokół rogów.
- Za duży promień względem rozmiaru: zawsze `min(r, min(b.x, b.y))`.
- Współrzędne zaokrąglaj do całych pikseli (Meteor robi `Math.round` w układzie widgetów), inaczej krawędzie „pływają". [KOD]
- Zapomniana kopia `pose()` daje elementy rysowane z cudzą macierzą.

---

## 6. Menu i ClickGUI: układ, widgety, motyw, animacje, tekst

### 6.1 Drzewo widgetów (Meteor) [KOD]

Tryb retained: `WWidget` ma `x, y, width, height`, `theme`, `parent`, `mouseOver`, `focused`, `tooltip`. Przebieg: `init()` → `calculateSize()` → `calculateWidgetPositions()` → `render(...)`. Rozmiary i pozycje są zaokrąglane do całych pikseli. Zdarzenia: `mouseClicked/Released/Moved/Scrolled`, `keyPressed/Repeated`, `charTyped`, każde z haczykiem `onXxx` dla podklas.

Kontenery: `WVerticalList`, `WHorizontalList`, `WTable`, `WSection`, `WView` (przewijany), `WWindow`. Układ opisuje `Cell` (wyrównanie, padding, `expandWidgetX/Y`, `minWidth`). Widgety: przyciski, checkbox, slider, dropdown, textbox, edytory liczb, `WKeybind`, `WTooltip`.

`WidgetScreen` ma wbudowany tryb debug z osobnym `GuiDebugRenderer` (do podglądu układu widgetów), co jest bardzo przydatne przy układzie. [KOD, szczegółów działania nie śledziłem]

### 6.2 Motyw jako dane

W Meteorze motyw (`GuiTheme`) sam jest obiektem `Settings`, więc użytkownik edytuje kolory w tym samym GUI. Wzorzec „trzech stanów" (`ThreeStateColorSetting`: normalny / hover / wciśnięty) jest wart skopiowania. Domyślne wartości: akcent `(145, 61, 226)`, tło `(20,20,20,200)` → `(30,30,30,200)` → `(40,40,40,200)`. [KOD]

### 6.3 Tokeny designu z ClickGUI LiquidBounce (Svelte/SCSS) [KOD]

Praktyczne liczby do zaadaptowania:

| Element | Wartość |
|---|---|
| Akcent | `#4677ff` |
| Panel | szerokość 250 px, `border-radius: 5px`, cień `0 0 10px` (czerń 50 %) |
| Nagłówek panelu | tło czerń 90 %, dolna linia 2 px w kolorze akcentu, padding `10px 15px`, tytuł 14 px / waga 500 |
| Ciało panelu | czerń 80 %, hover modułu 85 % |
| Moduł | tekst 12 px / waga 500, padding 10 px, włączony = kolor akcentu |
| Ustawienia modułu | tło czerń 50 %, lewy pasek 4 px w kolorze akcentu |
| Przełącznik | tor 22×12 (`border-radius: 4px`), gałka 12 px okrągła |
| Scrollbar | 2 px, zaokrąglony |
| Przejścia | kolory 0,2 s `ease`; przełączniki i strzałki 0,4 s; rozwinięcie panelu 300 ms `ease` |
| Font | Inter (Light / Regular / Medium / Bold) |
| Promienie w całym motywie | najczęściej 3 px i 5 px, okręgi 50 % |

Wniosek projektowy: ładne GUI to głównie **spójne, małe promienie (3-5 px), półprzezroczyste ciemne warstwy, jeden kolor akcentu, delikatny cień i krótkie przejścia 200-400 ms**. [WIEDZA co do oceny estetycznej]

### 6.4 Komponenty do zaplanowania

Okno/panel (przeciągalny, zwijalny), lista kategorii/modułów, wiersz modułu z przełącznikiem, ustawienia wszystkich typów (bool, liczba/suwak, zakres, enum, lista, kolor, klawisz, tekst), wyszukiwarka, tooltipy z opóźnieniem, scroll, edytor HUD z przyciąganiem do siatki, dialogi potwierdzeń. Pełną listę typów ustawień ClickGUI LiquidBounce widać w `src-theme/src/routes/clickgui/setting/` (Boolean, Int, Float, zakresy, Choice, MultiChoose, Color, Key/Bind, Text, Vector, File, listy, Curve). [KOD]

### 6.5 Animacje

- Meteor: `animProgress += (delta / 20 * 14) * (closing ? -1 : 1)`, czyli pełne otwarcie w ok. 70 ms, z `clamp(0..1)` i użyciem jako alfa. [KOD]
- Zalecenie z dokumentacji: czasu rzeczywistego (`Util.getMillis()`) używaj do animacji UI, a `partialTick` tylko dla rzeczy związanych z tickiem gry. [DOK]
- Funkcje easingu (ease-out/in-out) to standard. [WIEDZA]

### 6.6 Tekst

- Vanilla: `GuiGraphics.drawString(font, text, x, y, argb, shadow)`, kolor ARGB. [DOK]
- Własne fonty (potrzebne dla Inter itd.): Meteor ładuje TTF przez **STB TrueType** do atlasu 1-kanałowego (`TextureFormat.RED8`, filtr LINEAR) i rysuje własnym pipeline'em `UI_TEXT` z shaderem mnożącym alfę z tekstury przez kolor. LiquidBounce ma zarządcę czcionek z „glyph pages" (`FontGlyphPageManager`, `GlyphAtlasTexture`) i czcionki systemowe z fallbackiem (Segoe UI / Helvetica / DejaVu; osobno CJK). [KOD]
- Możliwy jest też własny provider TTF w systemie fontów vanilli **[WIEDZA, niezbadane]**.

---

## 7. Testowanie i debugowanie GUI

- **Fabric Client Game Tests** [DOK/KOD]: klasa implementuje `FabricClientGameTest`, w `runTest(ClientGameTestContext)` tworzy świat (`context.worldBuilder().create()`), czeka na chunki i robi `context.takeScreenshot("nazwa")`. Jest też `assertScreenshotEquals(...)` z porównaniem „fuzzy" względem obrazu wzorcowego. Uruchomienie: zadanie Gradle `runClientGameTest`.
- **CI na GitHub Actions** [DOK]: czasem trzeba dodać `-Dfabric.client.gametest.disableNetworkSynchronizer=true` do argumentów JVM.
- To daje **regresję wizualną** dla zaokrąglonych rogów, motywów i układu. Warto ustawić od początku.
- Pomocne: tryb debug widgetów (Meteor, sekcja 6.1) i logger z nazwą modu. [DOK/KOD]

---

## 8. Ryzyka i zgodność

1. **Szybka dezaktualizacja API.** Między 1.21.11 a 26.3 zmieniło się m.in.:
   - `GuiGraphics` → `GuiGraphicsExtractor` [DOK/KOD],
   - `guiRenderState.submitGuiElement` → `addGuiElement` [KOD: 1.21.11 vs 26.3],
   - `com.mojang.blaze3d.pipeline.RenderPipeline` → `com.mojang.renderpearl.api.pipeline.RenderPipeline` w 26.3 [KOD],
   - plugin Loom, Java 25, brak mapowań [DOK].
   Wniosek: trzymaj własny kod GUI za interfejsami (`UiGraphics`), a dotykanie wnętrz gry w jednym pakiecie.
2. **Licencje.** Meteor i LiquidBounce: GPL-3.0 (LiquidBounce wprost zakazuje użycia kodu w zamkniętej/obfuskowanej aplikacji). [KOD] `fabric-example-mod`: CC0. [KOD] `fabric-docs`: CC BY-NC-SA 4.0 (tekst dokumentacji; licencji kodu referencyjnego nie sprawdzałem). [KOD] Uczenie się z architektury jest bezpieczne, kopiowanie kodu nie.
3. **Regulaminy serwerów.** Klient z „cheatami" może skutkować banem na serwerach, które tego zakazują. Meteor określa się jako mod „dla serwerów anarchicznych". [KOD] Bezpieczne środowiska testowe: singleplayer lub własny serwer. Zasad Mojang/EULA nie weryfikowałem. [WIEDZA]
4. **Kompatybilność z modami.** Meteor ma listę `breaks` (m.in. OptiFabric, Feather, Origins) i warunkowe mixiny dla Sodium/Lithium/Iris; render własnych pipeline'ów może kolidować z modami renderującymi. [KOD]
5. **Wydajność.** Nie mierzyłem. Wzorce z kodu: pooling obiektów (`Pools.Mat3x2f`, `Pool<Scissor>`), batch po pipeline'ie, `discard` przy zerowym alfa. [KOD]

---

## 9. Luki i ograniczenia tego badania

- **Sieć:** sesja miała dostęp do `git clone` z GitHuba i Maven Central, ale **nie** do `docs.fabricmc.net`, `fabricmc.net`, `maven.fabricmc.net`, `meta.fabricmc.net`, serwerów Mojang, `minecraft.wiki` i Modrinth. Dlatego dokumentację Fabric czytałem z repozytorium `fabric-docs`, a nie ze strony.
- **Brak JAR-a Minecrafta 1.21.11:** zero kompilacji i uruchomień. Każda sygnatura z 1.21.11 pochodzi z kodu LiquidBounce@`0a3d2a25`, referencji `fabric-docs` dla 1.21.11 i gałęzi `1.21.11` Fabric API.
- **Niezweryfikowane:** szczegóły 9-slice w vanilli, konwencja ścieżek shaderów, własne providery TTF vanilli, wydajność, treść Mojang EULA.
- **Nie badałem:** `owo-lib` (deklaratywne UI, ma gałąź `1.21.11`), `imgui-java` (binding Dear ImGui; ma backend LWJGL3/OpenGL, a w 1.21.11 rysowanie idzie przez abstrakcję `GpuDevice`, więc mieszanie surowego GL jest ryzykowne; **zero dowodów** integracji z MC w sklonowanym repo). To kandydaci do osobnego badania.
- **Rozbieżne wersje narzędzi** (tabela w 2.2): nie rozstrzygnąłem, które są „zalecane".

---

## 10. Proponowana architektura i plan

```
src/client/java/<pkg>/
  core/        Client (entrypoint), EventBus, Config (JSON), Keybinds
  module/      Module, ModuleManager, Category, setting/ (Bool, Number, Enum, Color, Key, List…)
  event/       TickEvent, Render2DEvent, Render3DEvent, PacketEvent…
  mixin/       tylko źródła zdarzeń + accessory (cienkie)
  ui/
    gfx/       UiGraphics (interfejs), RoundedRect, Text, Blur, Pipelines, shadery
    widget/    Widget, Container, Cell, Window, Button, Switch, Slider…
    theme/     Theme (dane), tokeny, animacje
    screen/    ClickGuiScreen, HudEditorScreen
  hud/         HudElement + rejestracja przez HudElementRegistry
  command/     komendy klienta (Fabric Command API)
  addon/       punkt rozszerzeń (entrypoint "yourclient-addon")
```

Fazy:

0. **Szkielet:** szablon Fabric 1.21.11, Mojang mappings, split source sets, class tweaker, CI.
1. **Rdzeń:** magistrala zdarzeń, `Module`/`Setting`, konfiguracja JSON, klawisze, komendy.
2. **Fundament renderowania:** pipeline kolorowego quada i zaokrąglonego prostokąta (SDF), scissor, DPI, tekst (atlas STB).
3. **Toolkit UI:** widgety, układ `Cell`, motyw z tokenami (sekcja 6.3), ClickGUI, edytor HUD.
4. **Moduły przykładowe:** HUD, rysowanie 3D przez własny pipeline (wzorzec „render przez ściany" z `fabric-docs` `rendering/world.md`), narzędzia.
5. **Testy:** client game tests ze zrzutami ekranu.
6. **Twardnienie i migracja:** warstwa abstrakcji, przygotowanie do 26.x.

---

## 11. Źródła (wszystkie pierwotne; repozytoria sklonowane do scratchpada)

| Źródło | Commit / data | Co z niego wzięto |
|---|---|---|
| [FabricMC/fabric-docs](https://github.com/FabricMC/fabric-docs) | `2e637f7`, 2026-11-01 | `versions/1.21.11/develop/**` (rendering, porting, mappings, loom, class-tweakers, events, key-mappings, automatic-testing), `reference/1.21.11/**` (kod referencyjny), `versions/26.1.2/develop/porting`, `develop/**` (26.x) |
| [FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod) (gałąź `1.21.11`) | `8272aad`, 2026-10-24 | wersje narzędzi, `build.gradle`, `fabric.mod.json`, szablon mixinów |
| [FabricMC/fabric-api](https://github.com/FabricMC/fabric-api) (gałąź `1.21.11`, v0.141.6) | `28e4019`, 2026-07-28 | `HudElementRegistry`, `HudElement`, `SpecialGuiElementRegistry`, `ScreenEvents`, `ClientTickEvents`, `fabric-client-gametest-api-v1` |
| [MeteorDevelopment/meteor-client](https://github.com/MeteorDevelopment/meteor-client) (gałąź `1.21.11`) | `4803071`, 2026-08-09 | `renderer/**`, `gui/**`, `GameRendererMixin`, `Module`, `HudElement`, shadery, `libs.versions.toml` |
| [MeteorDevelopment/meteor-addon-template](https://github.com/MeteorDevelopment/meteor-addon-template) | `c2a7075`, 2026-10-17 | wzorzec addonu i modułu |
| [CCBlueX/LiquidBounce](https://github.com/CCBlueX/LiquidBounce) (`nextgen`) | HEAD `a0261b87`, 2026-11-04; **snapshot 1.21.11: `0a3d2a25`, 2026-04-27** | `render/**`, shadery `rounded_rect`/`gui_circle_lut`, `GuiElementRenderState`, `MixinGuiRenderState`, `liquidbounce.accesswidener`, `src-theme/**` (ClickGUI), `integration/**` (przeglądarka) |
| [wisp-forest/owo-lib](https://github.com/wisp-forest/owo-lib), [SpaiR/imgui-java](https://github.com/SpaiR/imgui-java) | tylko lista gałęzi / README | wzmianka w sekcji 9 |

Wykorzystywałem `git clone` / `git fetch` oraz `git show <commit>:<ścieżka>` na historii LiquidBounce od 2025-12-01, żeby odczytać stan z ery 1.21.11.
