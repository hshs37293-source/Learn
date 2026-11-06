# mcgui: GUI w dowolnym stylu, 2D i 3D, dla Minecrafta 1.21.11 i 26.x

Biblioteka do robienia własnych interfejsów w modach Fabric. Nazwy Mojang, jedna logika dla obu linii gry.

```
core/            czysty Java 21, BEZ Minecrafta: widgety, layout, motywy, animacje, 2D i 3D, rasteryzacja
mc-1_21_11/      adapter dla 1.21.11 (osobny projekt Gradle, Loom "remap", Java 21)
mc-26/           adapter dla 26.x   (osobny projekt Gradle, Loom bez mapowań, Java 25)
shaders/         opcjonalne szkice shaderów SDF (niepodłączone)
scripts/         test-core.sh: kompilacja i testy rdzenia samym javac
```

## Status: co jest sprawdzone, a co nie

| Część | Status |
|---|---|
| `core/` (≈3700 linii kodu + ≈480 linii testów) | **Kompiluje się i przechodzi 51 testów** (`./scripts/test-core.sh` oraz `gradle runTests`) |
| `SimpleBackend`, `Rasterizer`, 9-slice, motywy JSON, layout, wejście, projekcja 3D | testowane w rdzeniu na atrapie powierzchni |
| `mc-1_21_11/`, `mc-26/` (≈900 linii z konfiguracją Gradle) | **NIEKOMPILOWANE.** Piszę je w środowisku bez dostępu do serwerów Fabric/Mojang. Sygnatury wzięte z kodu referencyjnego Fabric docs (kompilowanego w ich CI) i z kodu Meteora; lista rzeczy do sprawdzenia poniżej |
| Wygląd w grze, wydajność, Vulkan | **nie sprawdzane** |

Uczciwie: rdzeń działa i jest testowany; **adaptery pod grę musisz zbudować i poprawić** (spodziewaj się drobnych błędów w nazwach).

## Szybki start

```bash
./scripts/test-core.sh          # kompilacja + 51 testów, tylko JDK 21, bez sieci
# albo
gradle runTests                 # w katalogu mcgui/
```

Mod (po sprawdzeniu sekcji „Do weryfikacji"):

```bash
cd mc-26 && gradle runClient          # lub: cd mc-1_21_11 && gradle runClient
```
W grze: **Right Shift** otwiera demo (przyciski motywów u góry, suwak, pole tekstu, podgląd 3D, lista).
W świecie na (0, 100, 0) rysuje się znacznik 3D widoczny przez ściany. W rogu ekranu jest HUD z FPS.

## Jak zrobić GUI (2D)

```java
Property<Boolean> on  = new Property<>(true);
Property<Double>  vol = new Property<>(0.5);

Panel panel = new Panel();                         // kolumna z tłem z motywu
panel.add(new Label("Ustawienia"));
panel.add(new Toggle(on));
panel.add(new Slider(vol, 0, 1));
panel.add(new Button("Zapisz", "button.primary", () -> save()));

UiRoot ui = new UiRoot(Themes.builtin("glass"), panel);
Minecraft.getInstance().setScreen(new UiScreen(Component.literal("Moje GUI"), ui));   // 26.x: mc.gui.setScreen(...)
```

Widgety: `Label`, `Button`, `Toggle`, `Slider`, `TextField`, `ProgressBar`, `ImageView`, `Spacer`,
`Panel`/`Column`/`Row`/`Stack`/`Grid`, `ScrollView`, `Viewport3D`. Wartości łączysz przez `Property<T>`.

## Dowolny styl: trzy poziomy

1. **Motyw JSON.** Widget zna tylko swoją *rolę* (`"button.primary"`), wygląd bierze z motywu: kolory, gradient,
   promienie rogów (osobno dla każdego), obramowanie, cienie, czcionka, odstępy, animacje przejść między stanami
   (`hover`, `pressed`, `focused`, `active`, `disabled`). Wbudowane: `flat`, `glass`, `neon`, `pixel`.
   ```java
   UiRoot ui = new UiRoot(Themes.fromJson(Files.readString(Path.of("mojmotyw.json"))), panel);
   ui.setTheme(Themes.builtin("neon"));   // zmiana w locie, bez przeładowania ekranu
   ```
   Format: patrz `core/src/main/resources/dev/mcgui/themes/*.json` i opis w `ThemeLoader`.
2. **Własny widget.** Dziedziczysz po `Widget` i rysujesz przez `UiGraphics`: `box`, `text`, `image` (także 9-slice),
   `path` (dowolne ścieżki, krzywe), `line`, `pushClip`, `pushTranslate/Rotate/Scale`, `pushLayer`, `custom`.
   Przykład: `core/.../demo/RadialMeter.java` (okrągły miernik z łuków). Widget działa na obu liniach gry bez zmian.
3. **Własny backend.** Implementujesz `UiGraphics` (albo tylko `Surface`) po swojemu, np. z shaderami SDF
   (`shaders/`, `research/mc-client/06`). Widgety i motywy zostają bez zmian.

## 3D

- **W świecie gry:** `World3D.show("id", g -> { g.box(...); g.sphere(...); g.arrow(...); g.line(...); g.grid(...); })`.
  API `Graphics3D`: linie, polilinie, trójkąty, quady, boxy, sfery, okręgi, strzałki, siatka, transformacje
  (`pushPose/translate/rotate/scale/popPose`), `depthTest(false)` = widoczne przez ściany.
  Linie są zamieniane na wstęgi zwrócone do kamery (`Ribbons`), więc potrzebny jest tylko potok wypełnionych quadów.
  Teksty 3D (`g.text`) są w API, ale **adaptery ich jeszcze nie rysują**.
- **W GUI:** widget `Viewport3D` rysuje scenę 3D programową projekcją (`Camera3D` + `Projector2D`) przez `UiGraphics`,
  więc działa wszędzie, z orbitą myszą (przeciąganie) i zoomem (kółko). Do podglądów, wykresów, minimap.

## Wersje

| | 1.21.11 | 26.x |
|---|---|---|
| Adapter | `mc-1_21_11/` | `mc-26/` (domyślnie 26.2) |
| Plugin Loom / Java | `fabric-loom-remap` / 21 | `fabric-loom` / 25 |
| Rysowanie GUI | `GuiGraphics`, `drawString`, `Screen.render` | `GuiGraphicsExtractor`, `text`, `extractRenderState` |
| Świat 3D | `WorldRenderEvents` + `BufferBuilder` + `MappableRingBuffer` | `LevelExtractionEvents`/`LevelRenderEvents` + `StagedVertexBuffer` |
| Klawisze | `KeyBindingHelper` | `KeyMappingHelper` |
| Otwieranie ekranu | `mc.setScreen` | `mc.gui.setScreen` |

**26.3:** przykładowy mod (`fabric-example-mod`, gałąź 26.3) i Meteor `master` używają już pakietów
`com.mojang.renderpearl.api.*` zamiast `com.mojang.blaze3d.*`. Część GUI (`McSurface`, `UiScreen`) nie zmienia się,
ale `World3D` w `mc-26/` jest napisany pod 26.2 (kod referencyjny Fabric docs) i dla 26.3 wymaga dostosowania importów
i sygnatur draw. Szczegóły migracji: `research/mc-client/02-migracja-1.21.11-do-26.x.md` i `04-api-renderowania.md`.

## Co umie `SimpleBackend` (domyślny backend, bez shaderów)

Działa na każdej wersji, bo używa tylko `fill`/`fillGradient`/tekstu/`blit`/scissor:
zaokrąglone rogi z osobnymi promieniami i antyaliasingiem krawędzi, obramowanie (inside/center/outside),
cienie (warstwy), gradient pionowy, ścieżki i linie (rasteryzacja trójkątów), 9-slice, alfa warstw, przesunięcie/skala/obrót.

**Nie umie:** rozmycia tła (`backdropBlur` ignorowane, więc styl `glass` wygląda jak półprzezroczysty, bez blura),
gradientów innych niż pionowe (używa koloru średniego), cieni wewnętrznych, zaokrąglonego przycinania (scissor jest prostokątny),
`custom(...)` poza zarejestrowanymi ręcznie efektami. Do tego służy backend z shaderami (poziom 3 wyżej).

## Do weryfikacji przy pierwszym buildzie adapterów

To są założenia, których nie mogłem sprawdzić (brak JAR-a gry):

1. Pakiet i metody `MouseButtonEvent` / `KeyEvent` / `CharacterEvent` (`net.minecraft.client.input`; akcesory `x()`, `y()`, `button()`, `key()`, `modifiers()`, `codepoint()` potwierdzone w kodzie Meteora, pakiet nie).
2. Przeciążenie `blit(..., int color)` z tintem w `McSurface.drawImage` (jeśli brak, użyj wersji bez koloru).
3. `Matrix3x2fStack.rotateAbout(angle, x, y)` w `McSurface`.
4. `Minecraft.getFps()` w `McGuiClient`.
5. Zachowanie `enableScissor` w obecności macierzy `pose()` (skalowanie 1/guiScale).
6. `Font.lineHeight`, `Font.width(String)`, `mc.getWindow().getGuiScale()`.
7. `World3D`: kolejność faz i `pipeline.getVertexFormatBinding(0)` (26.x), `RenderPipelines.DEBUG_FILLED_BOX` (1.21.11), `ClientLifecycleEvents.CLIENT_STOPPING`.
8. Wersje narzędzi w `gradle.properties` (zgodne z szablonem `fabric-example-mod`, ale sprawdź `fabricmc.net/develop`).

## Licencja

W `fabric.mod.json` wpisano `CC0-1.0` jako *domyślną propozycję*. To Twoja decyzja. Kod nie kopiuje implementacji
Meteor Client ani LiquidBounce (GPL-3.0): z nich wzięto wyłącznie wiedzę o API i wzorcach (opisaną w `research/`).
