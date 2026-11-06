# Klient Minecraft (Fabric) dla 1.21.11 i 26.x: wiedza zebrana w jednym miejscu

- **Data:** 2026-11-06
- **Cel:** zebrać sprawdzone informacje potrzebne do napisania własnego klienta (moda Fabric) z własnym, dowolnie stylowanym GUI, który działa **na 1.21.11 oraz na liniach 26.x** (najnowsza w repozytoriach: 26.3).
- **Zakres:** architektura klienta, toolchain, API renderowania, własny silnik GUI, migracja między liniami, testy.
- **Poza zakresem:** omijanie anti-cheatów i detekcji serwerów. Ten materiał dotyczy architektury i renderowania.

## Jak czytać ten zestaw

Zestaw składa się z dziewięciu plików. Czytaj w kolejności, albo skocz do tego, czego potrzebujesz.

| Plik | O czym | Linie |
|---|---|---|
| [01-toolchain.md](01-toolchain.md) | Gradle, Loom, Loader, Java, mapowania, class tweakery, `fabric.mod.json`: obie linie obok siebie | ok. 330 |
| [02-migracja-1.21.11-do-26.x.md](02-migracja-1.21.11-do-26.x.md) | Kroki migracji, tabele zmian nazw (Fabric API, GUI, renderowanie) | ok. 380 |
| [03-architektura-klienta.md](03-architektura-klienta.md) | Moduły, ustawienia, zdarzenia, konfiguracja, komendy, HUD, addony: wzorce z Meteora i LiquidBounce | ok. 400 |
| [04-api-renderowania.md](04-api-renderowania.md) | API renderowania obu linii: 1.21.11 (Blaze3D) kontra 26.x (renderpearl) | ok. 360 |
| [05-toolkit-gui.md](05-toolkit-gui.md) | Projekt biblioteki widgetów: drzewo, layout, wejście, scissor, animacje, motywy | ok. 400 |
| [06-silnik-stylow-i-efekty.md](06-silnik-stylow-i-efekty.md) | Prymitywy, SDF, cienie, glow, blur, przebiegi offscreen, motyw jako dane | ok. 380 |
| [07-testy-ci-debug.md](07-testy-ci-debug.md) | Testy jednostkowe, client game tests ze zrzutami, CI, debug | ok. 260 |
| [08-jeden-kod-dwie-linie.md](08-jeden-kod-dwie-linie.md) | Jak utrzymać jeden projekt dla 1.21.11 i 26.x | ok. 360 |
| [09-ryzyka-licencje-slownik.md](09-ryzyka-licencje-slownik.md) | Ryzyka, licencje, słownik pojęć, otwarte pytania | ok. 220 |

Starsze, osobne opracowania nadal obowiązują i są głębsze w swoich tematach:

- [../minecraft-client-gui-1.21.11.md](../minecraft-client-gui-1.21.11.md): pierwotny raport (celuje w 1.21.11).
- [../custom-rendering-gui-guide.md](../custom-rendering-gui-guide.md): przewodnik po własnym renderingu oparty na kodzie 26.3.

## Legenda pewności

Każde twierdzenie ma znacznik. Czytaj go zanim zaufasz kodowi.

| Znacznik | Znaczenie |
|---|---|
| **[KOD]** | Przeczytane bezpośrednio w kodzie źródłowym (repo i commit w tabeli poniżej). |
| **[DOK]** | Z oficjalnej dokumentacji Fabric (repo `fabric-docs`, commit `2e637f7`). |
| **[WIEDZA]** | Wiedza ogólna lub wniosek. **Niezweryfikowane** w tej sesji. Traktuj jako hipotezę. |
| **[SZKIC]** | Kod lub projekt napisany przeze mnie na podstawie wzorców z [KOD]. **Nie był kompilowany ani uruchamiany.** |

## Źródła, na których to się opiera

| Źródło | Commit / data | Co z niego |
|---|---|---|
| [FabricMC/fabric-docs](https://github.com/FabricMC/fabric-docs) | `2e637f7`, 2026-11-01 | `develop/**` (najnowsza), `versions/1.21.11/**`, `versions/26.1.2/**` (porting) |
| [FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod) | gałąź `26.3` `91f1901` (2026-10-24); gałąź `1.21.11` | wersje narzędzi, `build.gradle`, `fabric.mod.json` |
| [MeteorDevelopment/meteor-client](https://github.com/MeteorDevelopment/meteor-client) | `master` `e3a2e77` (2026-10-27); gałąź `1.21.11` `4803071` (2026-08-10) | `renderer/**`, `systems/**`, `gui/**`, mixiny, shadery |
| [CCBlueX/LiquidBounce](https://github.com/CCBlueX/LiquidBounce) | `nextgen` `2a3762a` (2026-11-06) | `render/**`, `config/**`, `event/**`, `features/module/**` |

Repozytoria były klonowane (`git clone --depth 1`) do katalogu tymczasowego i czytane lokalnie. Nic nie było kompilowane, bo sesja nie miała dostępu do serwerów Fabric/Mojang (patrz plik 09).

## Co jest ustalone, a co jest hipotezą (stan na dziś)

Ustalone w kodzie lub dokumentacji:

1. 26.x to wersje **bez obfuskacji** (nazwy Mojang), wymagają **Javy 25** i innego pluginu Loom. 1.21.11 jest ostatnią wersją z obfuskacją i wymaga Javy 21. [DOK, KOD]
2. W 26.x zmieniło się **całe API GPU**: pakiet `com.mojang.renderpearl.api.*`, bind groupy, nowe formaty wierzchołków, nowe rozszerzenia shaderów i jawne `layout(location)`. [KOD]
3. Od 26.2 gra ma **opcjonalny backend Vulkan**; surowy OpenGL nie jest wspierany. [DOK]
4. Meteor i LiquidBounce dochodzą do nowoczesnego GUI dwiema drogami: własny silnik meshy (Meteor) oraz elementy w pipeline'ie GUI vanilli + UI w przeglądarce (LiquidBounce). [KOD]

Hipotezy do sprawdzenia, gdy będzie dostęp do JAR-a gry:

1. Instancing w nowym `VertexFormat.builder(stepRate)`.
2. Dozwolone wartości `GpuFormat` dla atrybutów.
3. Konwencja ścieżek shaderów dla własnych pipeline'ów poza rejestrem vanilli.
4. Wydajność własnego silnika na Vulkanie.

Pełna lista otwartych pytań jest w pliku 09.
