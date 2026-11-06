# 01. Toolchain: 1.21.11 i 26.x obok siebie

Wszystkie wartości pochodzą z oficjalnego szablonu `fabric-example-mod` (dwie gałęzie) i z dokumentacji `fabric-docs`, o ile nie napisano inaczej. Znaczniki jak w [00-index.md](00-index.md).

## 1. Zestawienie jednej strony

| Element | **1.21.11** | **26.x** (przykład: 26.3) |
|---|---|---|
| Minecraft | `1.21.11` | `26.3` [KOD: gałąź `26.3` szablonu] |
| Obfuskacja | **jest** (ostatnia wersja) [DOK] | **brak** [DOK] |
| Java (kompilacja) | `release = 21` [KOD] | `release = 25` [KOD] |
| `fabric.mod.json` → `java` | `>=21` [KOD] | `>=25` [KOD] |
| `fabric.mod.json` → `minecraft` | `~1.21.11` [KOD] | `~26.3` [KOD] |
| Plugin Loom | `net.fabricmc.fabric-loom-remap` [KOD] | `net.fabricmc.fabric-loom` [KOD] |
| Wersja Loom (szablon) | `1.19-SNAPSHOT` [KOD] | `1.19-SNAPSHOT` [KOD] |
| Fabric Loader (szablon) | `0.20.1` [KOD] | `0.20.1` [KOD] |
| Fabric API | `0.141.6+1.21.11` [KOD] | `0.165.0+26.3` [KOD] |
| Gradle wrapper | `gradle-9.8.2` [KOD, gałąź 26.3; dla 1.21.11 wcześniej też 9.8.2] | `gradle-9.8.2` [KOD] |
| Mapowania | `mappings loom.officialMojangMappings()` (zalecane) [DOK] | **brak linii `mappings`** [DOK] |
| Zależności modów | `modImplementation` / `modCompileOnly` / `modApi` | `implementation` / `compileOnly` / `api` [DOK] |
| Zadanie pakujące | `remapJar` | `jar` [DOK] |
| Nagłówek class tweakera | `classTweaker v1 named` | `classTweaker v1 official` [DOK] |
| Zgodność modów | mody 1.21.11 nie działają na 26.x, **nawet jako zależność compile-only** [DOK] | j.w. |
| IntelliJ IDEA | dowolna aktualna | **2025.3 lub nowsza** (pełne wsparcie Javy 25) [DOK] |

Zasada praktyczna: **nie da się zbudować jednego artefaktu, który działa na obu liniach**, bo różni się plugin, Java, nazwy API i format shaderów. Strategie utrzymania obu linii są w pliku [08](08-jeden-kod-dwie-linie.md).

## 2. Wzorcowe pliki dla 1.21.11

### `gradle.properties` [KOD]
```properties
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=true

minecraft_version=1.21.11
loader_version=0.20.1
loom_version=1.19-SNAPSHOT

version=1.0.0
group=com.example

fabric_api_version=0.141.6+1.21.11
```

### `build.gradle` (skrót, zgodnie z wzorcem szablonu)
```groovy
plugins {
    id 'net.fabricmc.fabric-loom-remap' version "${loom_version}"
    id 'maven-publish'
}

loom {
    splitEnvironmentSourceSets()
    mods {
        "modid" {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
}

tasks.withType(JavaCompile).configureEach { it.options.release = 21 }
java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```
`mappings loom.officialMojangMappings()` jest linią zalecaną w dokumentacji migracji Yarn → Mojang; szablon 1.21.11 używał mapowań wskazanych w swoich właściwościach. [DOK + wniosek] Sprawdź przy pierwszym buildzie, której linii mapowań używa Twoja kopia szablonu.

### `fabric.mod.json` (zależności) [KOD]
```json
"depends": {
    "fabricloader": ">=0.20.1",
    "minecraft": "~1.21.11",
    "java": ">=21",
    "fabric-api": "*"
}
```

## 3. Wzorcowe pliki dla 26.3

### `gradle.properties` [KOD]
```properties
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=true

minecraft_version=26.3
loader_version=0.20.1
loom_version=1.19-SNAPSHOT

version=1.0.0
group=com.example

fabric_api_version=0.165.0+26.3
```

### `build.gradle` [KOD]
```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
    id 'maven-publish'
}

loom {
    splitEnvironmentSourceSets()
    mods {
        "modid" {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
}

tasks.withType(JavaCompile).configureEach { it.options.release = 25 }

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
```

Różnice względem 1.21.11 w pięciu linijkach:
1. `fabric-loom-remap` → `fabric-loom`.
2. Znika linia `mappings …`.
3. `modImplementation` → `implementation`.
4. `release = 21` → `release = 25`.
5. `VERSION_21` → `VERSION_25`.

### `fabric.mod.json` (zależności) [KOD]
```json
"depends": {
    "fabricloader": ">=0.20.1",
    "minecraft": "~26.3",
    "java": ">=25",
    "fabric-api": "*"
}
```

## 4. Układ projektu (`splitEnvironmentSourceSets`)

Szablon rozdziela kod na dwa source sety [KOD]:

```
src/main/java/...             kod wspólny (klient + serwer)
src/main/resources/           fabric.mod.json, modid.mixins.json, assets
src/client/java/...           kod tylko klienta
src/client/resources/         modid.client.mixins.json
```

W `fabric.mod.json`:

```json
"entrypoints": {
    "main":   [ "com.example.ExampleMod" ],
    "client": [ "com.example.client.ExampleModClient" ]
},
"mixins": [
    "modid.mixins.json",
    { "config": "modid.client.mixins.json", "environment": "client" }
]
```

Klient „czysty" (bez części serwerowej) może trzymać wszystko w `src/client` i ustawić `"environment": "client"`. Tak robi Meteor. [KOD, wcześniejsza analiza]

Konwencja nazw plików zasobów dla własnych shaderów (z kodu klientów):
- Meteor 26.3: `assets/meteor-client/shaders/*.vsh|fsh`. [KOD]
- Meteor 1.21.11: `assets/meteor-client/shaders/*.vert|frag`. [KOD]
- LiquidBounce 26.3: `resources/liquidbounce/shaders/**/*.vsh|fsh` (część plików nadal `.vert|frag`). [KOD]

## 5. Mapowania

### 5.1 Co się stało
- Yarn jest **wycofany** („Tribute to Fabric Yarn (2016-2025)"). Dla 1.21.11 zalecane są **mapowania Mojang**. [DOK]
- 26.x nie ma obfuskacji, więc **nie ma w ogóle mapowań** i nie ma linii `mappings` w Gradle. [DOK]
- Fabric API w 26.1 zmieniło nazwy publicznych klas i metod, aby pasowały do nazw Mojang (np. `ClientCommandManager` → `ClientCommands`). Te zmiany **nie są obsługiwane przez narzędzia do migracji mapowań**. [DOK]

### 5.2 Konsekwencja dla projektu
Jeżeli chcesz mieć jeden styl kodu na obu liniach, **od początku używaj nazw Mojang**, także dla 1.21.11. Wtedy migracja do 26.x ogranicza się do zmian API (nie nazw klas Minecrafta). Meteor 1.21.11 używał Yarn, a LiquidBounce Mojang, co jest widoczne w obu bazach. [KOD]

### 5.3 Migracja Yarn → Mojang (jeśli startujesz z Yarn)
Procedura z dokumentacji (`develop/porting/mappings`): [DOK]
1. Zacznij od repozytorium budującego się na Yarn.
2. Uruchom zadanie migracji mapowań Loom (`migrateMappings`) do mapowań Mojang.
3. Zbuduj i popraw ręcznie to, czego narzędzie nie zmieniło (głównie Fabric API, oraz nazwy w mixinach, które są stringami).
4. Dopiero potem przejdź do 26.x.

Szczegółowe nazwy zadań i flag sprawdź w `develop/porting/mappings/` repozytorium `fabric-docs`, bo mogą się zmienić. [WIEDZA co do aktualności nazw]

## 6. Class tweakery (dawniej access wideners)

Co to jest: narzędzie uzupełniające Mixin, pozwalające m.in. otworzyć prywatne pola i klasy gry w czasie kompilacji i uruchomienia. [DOK]

Wymagania: **Fabric Loader ≥ 0.18.0**, **Loom ≥ 1.12**; mogą dotyczyć tylko klas vanilli. [DOK]

### Format
Pierwsza linia:
```classtweaker
classTweaker  v1  official      # 26.x
classTweaker  v1  named         # 1.21.11 z mapowaniami Mojang (Loom "named")
```

Pliki nazywa się zwyczajowo `modid.classtweaker`, trzyma w `resources`. W `fabric.mod.json` pole nadal nazywa się `accessWidener` (kompatybilność wsteczna):
```json
"accessWidener": "example-mod.classtweaker"
```
W `build.gradle` plik trzeba wskazać w bloku `loom` (dokładny zapis jest w `develop/class-tweakers/index.md`, sekcja „build.gradle"; nie przepisuję go, bo w dokumentacji jest dołączany jako fragment kodu, którego tu nie widziałem). [DOK co do konieczności, WIEDZA co do składni]

### Dyrektywy
`accessible`, `extendable`, `mutable`, `inject-interface`, `extend-enum`; z prefiksem `transitive-` widoczne dla modów zależnych. [DOK]

### Walidacja
Domyślnie wpisy odwołujące się do nieistniejących celów są **ignorowane po cichu**. Uruchom `./gradlew validateAccessWidener`, żeby to wyłapać. [DOK]

### Przykłady z klientów (do GUI)
LiquidBounce 26.3 [KOD]:
```classtweaker
accessible class net/minecraft/client/renderer/state/gui/GuiRenderState$Node
accessible class net/minecraft/client/gui/GuiGraphicsExtractor$ScissorStack
accessible field net/minecraft/client/gui/GuiGraphicsExtractor scissorStack Lnet/minecraft/client/gui/GuiGraphicsExtractor$ScissorStack;
accessible field net/minecraft/client/gui/GuiGraphicsExtractor guiRenderState Lnet/minecraft/client/renderer/state/gui/GuiRenderState;
```
Odpowiedniki na 1.21.11 (z poprzedniej analizy LiquidBounce `0a3d2a25`) [KOD]:
```classtweaker
accessible field net/minecraft/client/gui/GuiGraphics guiRenderState Lnet/minecraft/client/gui/render/state/GuiRenderState;
accessible field net/minecraft/client/gui/GuiGraphics scissorStack Lnet/minecraft/client/gui/GuiGraphics$ScissorStack;
```
Zwróć uwagę, że **zmieniły się nazwy klas i pakiety** (`gui.render.state.GuiRenderState` → `renderer.state.gui.GuiRenderState`, `GuiGraphics` → `GuiGraphicsExtractor`). Wniosek: class tweaker trzeba prowadzić **osobno dla każdej linii**.

## 7. Zdarzenia i narzędzia Fabric API przydatne w kliencie

Nazwy dla 1.21.11 i dla 26.x (po renamach z `porting/fabric-api`) [DOK]:

| Zastosowanie | 1.21.11 | 26.x |
|---|---|---|
| Klawisze | `KeyBindingHelper.registerKeyBinding(...)` | `KeyMappingHelper.registerKeyMapping(...)` (pakiet `...client.keymapping.v1`) |
| Komendy klienta | `ClientCommandManager` | `ClientCommands` |
| Źródło komendy | `FabricClientCommandSource.getWorld()` | `getLevel()` |
| Tik świata | `ClientTickEvents.START_WORLD_TICK` / `END_WORLD_TICK` | `START_LEVEL_TICK` / `END_LEVEL_TICK` |
| Zmiana świata | `ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE` | `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE` |
| Ekrany: pomocnik | `Screens.getButtons / getTextRenderer / getClient` | `Screens.getWidgets / getFont / getMinecraft` |
| Renderowanie świata | `WorldRenderEvents.*` (np. `AFTER_ENTITIES`) | `LevelRenderEvents.*` (pakiet `...rendering.v1.level`) |
| Specjalne elementy GUI | `SpecialGuiElementRegistry` | `PictureInPictureRendererRegistry` |
| Dekoracje przedmiotów | `DrawItemStackOverlayCallback` | `RenderItemDecorationsCallback` |
| HUD | `HudElementRegistry` (nowe), `HudRenderCallback` (przestarzałe od API 0.116) | `HudElementRegistry` |

## 8. Co uruchomić zaraz po pierwszym zbudowaniu

Lista kontrolna dla szkieletu (obie linie):

- [ ] `./gradlew build` przechodzi.
- [ ] `./gradlew runClient` startuje i widać mod na liście.
- [ ] `./gradlew validateAccessWidener` nie zgłasza błędów.
- [ ] W dev-środowisku Mixiny są „audytowane" (Meteor wymusza to w trybie deweloperskim przez `MixinEnvironment.audit()`). [KOD]
- [ ] Jeden test jednostkowy z Fabric Loader JUnit przechodzi (plik 07).
- [ ] Jeden client game test robi zrzut ekranu (plik 07).

## 9. Wersje narzędzi: co jest pewne, a co nie

- Wartości z gałęzi `26.3` i `1.21.11` szablonu są pewne dla tego dnia. [KOD]
- Strona `fabricmc.net/develop` z „zalecanymi" wersjami była niedostępna z tej sesji, więc **nie porównywałem szablonu z oficjalną stroną**. Przed startem sprawdź ją ręcznie. [LUKA]
- Meteor (1.21.11) używał Loader 0.18.2 / Loom 1.14, a LiquidBounce 0.18.4 / Loom 1.15. Szablon podaje nowsze wartości. To różnica w czasie, a nie sprzeczność. [KOD]
