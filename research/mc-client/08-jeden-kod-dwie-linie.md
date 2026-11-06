# 08. Jeden kod, dwie linie: jak utrzymać 1.21.11 i 26.x

Problem: nie da się zbudować jednego JAR-a dla obu linii (plik 01, sekcja 1). Pytanie brzmi, jak **nie pisać wszystkiego dwa razy**. Plik porównuje strategie i proponuje architekturę rdzenia i adapterów. Znaczniki jak w [00-index.md](00-index.md). Kod i konfiguracje Gradle to **[SZKIC]** i nie były uruchamiane.

## 1. Co się różni, a co nie

### 1.1 Różni się (musi być osobno) [KOD / DOK]
| Obszar | Przykład różnicy |
|---|---|
| Toolchain | plugin Loom, Java 21 / 25, brak mapowań, `modImplementation` kontra `implementation` |
| Class tweaker | nagłówek `named` / `official`, nazwy klas |
| Nazwy API Fabric | `ClientCommandManager` / `ClientCommands`, `KeyBindingHelper` / `KeyMappingHelper`, `WorldRenderEvents` / `LevelRenderEvents` |
| Klasy GUI | `GuiGraphics` / `GuiGraphicsExtractor`, `drawString` / `text`, `submitGuiElement` / `addGuiElement` |
| API GPU | `blaze3d.*` kontra `renderpearl.api.*`, format wierzchołków, pipeline, bind groupy |
| Shadery | rozszerzenia plików, `layout(location)`, `#include` |
| Mixiny | cele wstrzyknięć (nazwy metod i klas wewnętrznych) |

### 1.2 Nie różni się (może być wspólne)
| Obszar | Dlaczego wspólne |
|---|---|
| Moduły, ustawienia, konfiguracja | nie dotykają Minecrafta (jeśli dobrze odseparowane) |
| Magistrala zdarzeń | własna implementacja bez zależności od MC |
| Drzewo widgetów i layout | czysta geometria |
| Motywy i style | dane (JSON) |
| Animacje | czas i matematyka |
| Matematyka SDF i logika efektów (specyfikacja) | tylko implementacja shaderów różni się formą |
| Testy logiki | bez gry |

Dowód z praktyki: LiquidBounce ma ten sam rekord `RoundedRectGuiElementRenderState` w obu liniach; różnią się importy, nazwa metody wysyłki i klasa grafiki. [KOD] To pokazuje, że cienka warstwa wystarcza.

## 2. Cztery strategie

### Strategia A: dwie gałęzie (wzorzec Meteora i LiquidBounce)
Meteor ma `master` (26.3) i osobną gałąź `1.21.11` (ostatni commit 2026-08-09). LiquidBounce ma zamrożony snapshot dla 1.21.11 (`0a3d2a25`, 2026-04-27) i rozwija `nextgen` na 26.3. [KOD]

| Plus | Minus |
|---|---|
| zero skomplikowanego buildu | poprawki trzeba przenosić ręcznie (cherry-pick) |
| każda linia może mieć własne uproszczenia | linie rozjeżdżają się w czasie |
| łatwe dla małego zespołu | stara linia zwykle „wygasa" |

Kiedy: gdy 1.21.11 jest tylko „drugą linią" na przejściowy okres.

### Strategia B: wspólny rdzeń + adaptery (wielomodułowy Gradle) — rekomendowana
Rdzeń bez zależności od Minecrafta, adaptery per linia, w jednym repozytorium.

```
repo/
  core/            czysty Java: moduły, wartości, zdarzenia, layout, styl, animacje, UiGraphics (interfejs)
  mc-1_21_11/      adapter: build dla 1.21.11, backend rysowania, mixiny, class tweaker
  mc-26/           adapter: build dla 26.x, backend rysowania, mixiny, class tweaker
  assets-shared/   motywy JSON, tłumaczenia, ikony
  tests-core/      testy jednostkowe rdzenia i layoutu
```

| Plus | Minus |
|---|---|
| ~większość kodu napisana raz | trzeba zdyscyplinować granicę rdzeń/adapter |
| testy rdzenia bez gry | dwa buildy Loom w jednym repo (konfiguracja bywa kłopotliwa) |
| łatwo dodać trzecią linię | mixiny i backendy rysowania i tak są dwa razy |

### Strategia C: preprocesor wersji (jedno źródło, wiele celów)
Narzędzia do „modowania wielowersyjnego" (generują warianty kodu z jednego źródła na podstawie dyrektyw). Znam je z ogólnej wiedzy; **nie badałem żadnego w tej sesji**. [WIEDZA, niezweryfikowane] Plusy: jedno źródło dla drobnych różnic. Minusy: zależność od narzędzia, trudniejsze debugowanie, ograniczone przy dużych różnicach (a tu mamy zmianę całego API GPU).

### Strategia D: tylko najnowsza linia
Zdecyduj, że wspierasz wyłącznie 26.x. Najprostsza i najtańsza; traci użytkowników 1.21.11.

### Rekomendacja
Jeśli celem jest **oba światy z jednym zespołem**, wybierz **B**. Jeśli to ma być drugorzędna linia tymczasowa, **A**. Nie wybieraj C bez prototypu, bo zmiana API GPU jest zbyt duża dla prostych dyrektyw.

## 3. Architektura rdzeń + adapter

### 3.1 Co wchodzi do rdzenia
Pakiety, które **nie mogą** importować `net.minecraft.*`, `com.mojang.*` ani `net.fabricmc.*`:

```
core/
  event/       EventBus, definicje zdarzeń (jako czyste obiekty)
  module/      Module, ModuleManager
  value/       Value<T> + typy
  config/      zapis/odczyt (Path, JSON), migracje
  ui/layout/   Rect, Size, Constraints, kontenery
  ui/widget/   Widget i kontrolki (rysują przez UiGraphics)
  ui/style/    Theme, StyleSheet, tokeny, animacje
  ui/gfx/      UiGraphics (interfejs), BoxStyle, TextStyle, Paint, ...
```

Wymuszenie granicy: **osobny moduł Gradle bez zależności od Minecrafta** (kompilator nie pozwoli zaimportować). Dodatkowo test architektury (np. ArchUnit) pilnujący zakazu importów. [WIEDZA co do ArchUnit]

### 3.2 Poziom Javy rdzenia
Skompiluj rdzeń z `release = 21`; bajtkod Javy 21 działa na JVM 25, więc ten sam JAR rdzenia może trafić do obu linii. Adapter 26.x kompiluje się z `release = 25`. [WIEDZA, ogólna zasada kompatybilności bajtkodu; sprawdź przy buildzie]

### 3.3 Co wchodzi do adapterów
- Entrypoint (`ClientModInitializer`) i rejestracja (klawisze, komendy, HUD).
- Mixiny jako źródła zdarzeń.
- Implementacja `UiGraphics` (backend rysowania).
- Pipeline'y, shadery, format wierzchołków, kompilacja.
- Class tweaker i `fabric.mod.json`.
- Adaptery wejścia (`Screen` ↔ `InputState`).
- Adaptery narzędzi (schowek, czas, ścieżki).

### 3.4 Kontrakt `UiGraphics` (rdzeń definiuje, adapter implementuje) [SZKIC]
```java
public interface UiGraphics {
    void box(Rect r, BoxStyle style);
    void text(CharSequence s, Font font, float x, float y, Paint paint);
    void image(TextureHandle t, Rect dst, Rect srcUv, ImageStyle style);
    void path(Path p, PathStyle style);
    void pushClip(Rect r, float[] cornerRadii);
    void popClip();
    void pushLayer(LayerStyle style);
    void popLayer();
    void custom(Rect r, ShaderEffect effect);        // wyjście awaryjne
    float scale();                                    // współczynnik pikseli fizycznych
    long nowMillis();                                 // czas rzeczywisty
}
```
Własności kontraktu:
- **Nie ma w nim typów Minecrafta** (`Identifier`, `GuiGraphics`...). Zasoby to `TextureHandle` i `ResourceId` (własne).
- Współrzędne w jednostkach logicznych, `scale()` zwraca skalę do pikseli fizycznych.
- Brak metod „natychmiastowych": wszystko to polecenia (zgodne z modelem extract→draw).

### 3.5 Adapter 1.21.11 (szkic odpowiedzialności)
- Implementuje `UiGraphics` przez `GuiGraphics` (proste kształty) i własne `GuiElementRenderState` z pipeline'em SDF (zaokrąglone rogi).
- Rejestruje pipeline `RenderPipelines.register(...)` z `GUI_SNIPPET`.
- Class tweaker `named` z polami `guiRenderState`, `scissorStack`.
- Wysyłka: `submitGuiElement`.

### 3.6 Adapter 26.x (szkic odpowiedzialności)
- Implementuje `UiGraphics` przez `GuiGraphicsExtractor` i własne elementy z `addGuiElement`.
- Pipeline'y w nowym API (bind groupy, `PrimitiveTopology`, `ColorTargetState`).
- Class tweaker `official` z klasami nowych nazw.
- Hook „na wierzchu": `GuiRenderer.render` na `TAIL`.

### 3.7 Kto wywołuje kogo
```
mixin/adapter  --zdarzenia-->  core (EventBus)
core.module    --Value-->      core.config
core.ui        --UiGraphics--> adapter.backend  --API gry-->  Minecraft
adapter.input  --InputEvent--> core.ui
```
Rdzeń nigdy nie wywołuje adaptera wprost; zawsze przez interfejs wstrzyknięty przy starcie.

## 4. Gradle: szkic wielomodułowy [SZKIC]

`settings.gradle`:
```groovy
rootProject.name = 'myclient'
include 'core', 'mc-1_21_11', 'mc-26'
```

`core/build.gradle` (bez Minecrafta):
```groovy
plugins { id 'java-library' }
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
tasks.withType(JavaCompile).configureEach { options.release = 21 }
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'   // wersja przykładowa
}
test { useJUnitPlatform() }
```

`mc-26/build.gradle`:
```groovy
plugins { id 'net.fabricmc.fabric-loom' version "${loom_version}" }
dependencies {
    minecraft "com.mojang:minecraft:26.3"
    implementation "net.fabricmc:fabric-loader:${loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"
    implementation project(':core')
    include project(':core')            // doklejenie rdzenia do JAR-a (jar-in-jar)
}
tasks.withType(JavaCompile).configureEach { options.release = 25 }
```

`mc-1_21_11/build.gradle`:
```groovy
plugins { id 'net.fabricmc.fabric-loom-remap' version "${loom_version}" }
dependencies {
    minecraft "com.mojang:minecraft:1.21.11"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version_1_21_11}"
    implementation project(':core')
    include project(':core')
}
tasks.withType(JavaCompile).configureEach { options.release = 21 }
```

Zastrzeżenia: [WIEDZA/LUKA]
- Czy dwa różne pluginy Loom można mieszać w jednym multi-projekcie bez konfliktów, **nie sprawdzałem**. To najbardziej ryzykowny punkt tego szkicu. Awaryjnie: dwa osobne repozytoria lub dwa niezależne buildy skryptowane z jednego `core` publikowanego do lokalnego repo Maven.
- Dokładny sposób dołączenia rdzenia (`include` / jar-in-jar / shadow) zależy od Loom; sprawdź w `develop/loom/`. [DOK do uzupełnienia]
- Wersje bibliotek testowych w szkicu są przykładowe.

## 5. Współdzielenie zasobów

| Zasób | Jak |
|---|---|
| Motywy JSON, tłumaczenia, ikony | katalog `assets-shared`, kopiowany do `processResources` obu adapterów |
| Shadery GLSL | **osobno** (różne nagłówki i `layout`); można generować z jednego szablonu skryptem Gradle (podstawienie nagłówka i rozszerzenia) |
| Zrzuty wzorcowe | osobno per linia |
| `fabric.mod.json` | szablon z podstawieniem `minecraft`, `java`, `depends` |

Generowanie shaderów z szablonu [SZKIC]: plik źródłowy `box.glsl.in` z markerami `@HEADER@` i `@IN_LOCATIONS@`; zadanie Gradle podstawia nagłówek dla linii i zapisuje `.vsh/.fsh`. Zmniejsza ryzyko rozjazdu dwóch kopii.

## 6. Mixiny w dwóch liniach

Mixiny są **najbardziej kruchą** częścią: celują w nazwy metod i klas wewnętrznych. Meteor ma ~213 plików mixinów (`master`). [KOD] Zasady:
1. Mixiny w adapterach, nie w rdzeniu.
2. Każdy mixin robi jedno: wysyła zdarzenie z rdzenia.
3. Zbiór mixinów dla renderowania (najkruchszy) trzymaj w osobnym pliku `*.mixins.json`.
4. Po aktualizacji gry uruchamiaj `MixinEnvironment.audit()` w dev (Meteor tak robi), aby wyłapać niepasujące cele. [KOD]
5. Warunkowe mixiny dla innych modów przez `MixinPlugin`. [KOD]

## 7. Pipeline wydawniczy (CI)

```
push → test-core (JUnit, bez gry)
     → build mc-1_21_11 + runClientGameTest (zrzuty 1.21.11)
     → build mc-26      + runClientGameTest (zrzuty 26.x)
     → porównanie list poleceń rysowania między liniami
     → artefakty: myclient-1.21.11.jar, myclient-26.x.jar
```
Wersjonowanie: jedna wersja rdzenia, sufiks linii w nazwie pliku (np. `1.4.0+mc1.21.11`, `1.4.0+mc26.3`). Wspierane wersje w `fabric.mod.json` przez `depends.minecraft` (np. `~26.3`, `~1.21.11`). [KOD: format zależności]

## 8. Polityka wsparcia (ustal wcześnie)

| Pytanie | Rekomendacja |
|---|---|
| Czy 1.21.11 dostaje nowe funkcje? | tak przez pewien czas, potem tylko poprawki błędów |
| Czy 26.x wszystkie podwersje? | celuj w najnowszą minor (np. 26.3); `~26.3` w zależności; poprzednie tylko jeśli kosztuje mało |
| Co z 26.1/26.2? | nie badałem różnic 26.1 → 26.3 poza tym, co wynika z kodu klientów [LUKA] |
| Kto decyduje o porzuceniu starej linii? | zdefiniuj próg (np. spadek użycia lub koszt utrzymania) |

## 9. Plan wdrożenia strategii B

1. Utwórz moduł `core` i przenieś do niego wszystko, co nie dotyka Minecrafta. Dodaj test blokujący importy MC.
2. Zdefiniuj `UiGraphics` i atrapę nagrywającą (plik 07).
3. Napisz adapter dla linii, na której masz większość wiedzy (np. 26.x, bo dokumentacja jest najnowsza).
4. Dopiero potem dodaj drugi adapter; różnice wynikną z tabeli w pliku 02.
5. Dodaj CI z oboma buildami i zrzutami.
6. Dopracuj generowanie shaderów z szablonu.
7. Zapisz decyzje (ADR) o granicy rdzeń/adapter.

## 10. Największe ryzyka tej strategii

1. **Przeciek typów MC do rdzenia** (np. `Identifier` w interfejsie). Lek: zakaz importów + test.
2. **Zbyt szerokie `UiGraphics`**: im więcej metod, tym trudniej o zgodność. Zaczynaj od minimum (`box`, `text`, `pushClip`, `popClip`).
3. **Dwa Loomy w jednym repo**: patrz zastrzeżenie w sekcji 4.
4. **Różne zachowanie wizualne** między liniami (antyaliasing, font): rozwiązują zrzuty wzorcowe per linia i tolerancje.
5. **Koszt utrzymania mixinów**: nieuniknione; minimalizuj liczbę i trzymaj je cienkie.
