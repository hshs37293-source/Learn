# 09. Ryzyka, licencje, słownik, otwarte pytania

Zamknięcie zestawu: co może pójść źle, co wolno kopiować, co oznaczają pojęcia i czego **nie** wiem. Znaczniki jak w [00-index.md](00-index.md).

## 1. Ryzyka techniczne

| # | Ryzyko | Wpływ | Mitygacja |
|---|---|---|---|
| T1 | **Szybka dezaktualizacja API** renderowania (zmiany z 1.21.11 do 26.3 są głębokie, plik 02) | wysoki | cienka warstwa `UiGraphics`, backend w jednym pakiecie, testy zrzutów |
| T2 | Backend Vulkan/Metal (od 26.2) zachowuje się inaczej niż GL | średni | testy na obu backendach; unikanie niezdefiniowanego zachowania w shaderach; tylko `renderpearl.api` [DOK] |
| T3 | Mixiny celujące w wnętrza renderowania łamią się co wersję | wysoki | mało mixinów, cienkie, audyt w dev, zdarzenia Fabric API tam, gdzie wystarczą |
| T4 | Class tweakery z nazwami klas zmieniają się między liniami | średni | osobny plik per linia, `validateAccessWidener` w CI |
| T5 | Za mało miejsca na parametry w wierzchołku (uber-box) | średni | szerszy format, UBO/tekstura danych, ewentualnie instancing (niezweryfikowane) |
| T6 | Wydajność własnego silnika (blur, wiele warstw) | średni | batching, cache statycznych warstw, blur w niższej rozdzielczości; **pomiary konieczne** |
| T7 | Kolizje z modami renderującymi (Sodium, Iris itd.) | średni | testy; warunkowe mixiny; unikanie zależności od wnętrz |
| T8 | Zapis konfiguracji uszkodzony (zamknięcie gry, równoległy zapis) | niski/średni | zapis atomowy, blokada, kopie zapasowe, wersja schematu |
| T9 | Dwa pluginy Loom w jednym repo | średni | prototyp wcześnie; awaryjnie osobne repozytoria (plik 08) |
| T10 | Fonty i tekst międzynarodowy (CJK, RTL) | średni | planuj od początku; fallback fontów (LiquidBounce) |

## 2. Ryzyka produktowe i prawne

### 2.1 Licencje kodu, z którego się uczysz [KOD]
| Projekt | Licencja | Konsekwencja |
|---|---|---|
| Meteor Client | **GPL-3.0** | kopiowanie kodu wymusza GPL na całym Twoim kliencie |
| LiquidBounce | **GPL-3.0** (z wyraźnym zakazem użycia kodu w zamkniętej/obfuskowanej aplikacji) | j.w. |
| `fabric-example-mod` | CC0 | wolno używać dowolnie |
| `fabric-docs` (tekst) | CC BY-NC-SA 4.0 | tekst dokumentacji z uznaniem autorstwa, bez użycia komercyjnego i na tej samej licencji; licencji kodu referencyjnego nie sprawdzałem |

Praktyka, którą stosują te materiały: opisujemy **wzorce i API własnymi słowami** i piszemy **własny kod**. Nie kopiuj implementacji z klientów. Krótkie cytaty nazw metod i sygnatur API (Mojang/Fabric) to nie kopiowanie implementacji klienta, ale jeśli planujesz dystrybucję, skonsultuj licencje z kimś kompetentnym. [WIEDZA; to nie jest porada prawna]

### 2.2 Regulaminy serwerów i EULA
Klient z funkcjami „cheatowymi" może skutkować banem na serwerach, które tego zakazują. Meteor określa się jako mod „dla serwerów anarchicznych". [KOD] Bezpieczne środowiska testowe: singleplayer lub własny serwer. Zasad Mojang/EULA nie weryfikowałem; Loom wymaga zaakceptowania EULA do uruchamiania testów (`eula = true`). [DOK + LUKA]

### 2.3 Kompatybilność z innymi modami
Meteor deklaruje listę `breaks` (m.in. OptiFabric, Feather, Origins) i ma warunkowe mixiny dla Sodium/Lithium/Iris. [KOD] Zdecyduj, z czym deklarujesz zgodność, i testuj to.

## 3. Ryzyka procesowe

1. **Rozjazd linii** (strategia A z pliku 08): poprawki ginące w jednej z gałęzi. Lek: wspólny rdzeń.
2. **Brak testów wizualnych**: regresje wyglądu zauważasz dopiero po wydaniu. Lek: zrzuty w CI.
3. **Zbyt wczesna rozbudowa silnika efektów**: najpierw prostokąty z SDF i tekst, dopiero potem blur i glow.
4. **Niezweryfikowane założenia** (sekcja 6) stają się „faktami". Lek: oznaczaj i sprawdzaj przy pierwszym uruchomieniu.

## 4. Słownik

| Pojęcie | Znaczenie |
|---|---|
| **Fabric Loader** | ładowarka modów Fabric |
| **Loom** | plugin Gradle do budowania modów Fabric |
| **Fabric API** | biblioteka zdarzeń i narzędzi dla modów Fabric |
| **Mapowania** | tłumaczenie obfuskowanych nazw gry na czytelne (Yarn wycofany; Mojang) |
| **Class tweaker / access widener** | plik otwierający prywatne elementy klas gry |
| **Mixin** | mechanizm modyfikacji bajtkodu klas gry |
| **Mixin: `@Inject`** | wstrzyknięcie kodu w wybrane miejsce metody |
| **`@Local` (MixinExtras)** | dostęp do zmiennej lokalnej metody docelowej |
| **Blaze3D** | warstwa pośrednia między grą a GPU w 1.21.x |
| **renderpearl** | nazwa pakietu nowej warstwy GPU w 26.x (`com.mojang.renderpearl.api`) [KOD] |
| **`RenderPipeline`** | opis stanu rysowania: shadery, format wierzchołków, głębia, blend |
| **`RenderPass`** | pojedynczy przebieg rysowania do celu |
| **`CommandEncoder`** | obiekt zapisujący polecenia dla GPU |
| **Bind group (`BindGroupLayout`)** | układ uniformów i tekstur dostępnych dla pipeline'u (26.x) |
| **`GpuBufferSlice`** | wycinek bufora (bufor + offset + długość) |
| **UBO / uniform buffer** | bufor z danymi dla shadera (std140) |
| **Std140** | układ pamięci uniform buffera |
| **Vertex format** | zestaw i układ atrybutów wierzchołka |
| **Topologia / DrawMode** | sposób składania wierzchołków w prymitywy |
| **SDF (signed distance field)** | funkcja odległości do kształtu; podstawa zaokrąglonych rogów i cieni |
| **MSDF** | wielokanałowy SDF dla ostrego tekstu |
| **AA / antyaliasing** | wygładzanie krawędzi |
| **`fwidth`** | pochodna w shaderze używana do szerokości piksela |
| **Scissor** | prostokąt przycinania rysowania |
| **9-slice** | rysowanie ramki z tekstury z rozciąganiem środka i zachowaniem rogów |
| **Offscreen** | rysowanie do własnej tekstury zamiast na ekran |
| **Post-process** | efekt nakładany na gotowy obraz |
| **Extract → draw** | podział na zbieranie stanu i rysowanie |
| **`GuiElementRenderState`** | rekord opisujący element GUI do narysowania |
| **`GuiGraphics` / `GuiGraphicsExtractor`** | główna klasa rysowania GUI (1.21.11 / 26.x) |
| **HUD element** | element nakładki rysowanej na ekranie gry |
| **ClickGUI** | menu z modułami i ustawieniami sterowane myszą |
| **Retained mode** | drzewo obiektów-widgetów trzymane między klatkami |
| **Immediate mode** | UI opisywane od nowa w każdej klatce |
| **Hot reload** | przeładowanie motywu/zasobu bez restartu |
| **Game test** | test uruchamiający prawdziwy klient lub serwer gry |

## 5. Co ustaliłem (krótko)

1. Toolchain obu linii i ich różnice (plik 01) [KOD/DOK].
2. Zmiany nazw Fabric API 1.21.11 → 26.1 (plik 02) [DOK].
3. Architektura nowoczesnych klientów (moduły, wartości, zdarzenia, konfiguracja, HUD, addony; plik 03) [KOD].
4. API renderowania obu linii i dwa sposoby dodania własnego rysowania (plik 04) [KOD].
5. Wzorce toolkitu GUI z Meteora (plik 05) [KOD].
6. Wzorce stylów i efektów oraz szkice shaderów (plik 06) [KOD + SZKIC].
7. Jak testować GUI zrzutami (plik 07) [DOK].
8. Strategie utrzymania dwóch linii (plik 08) [SZKIC].

## 6. Czego **nie** wiem (otwarte pytania)

Poniższe trzeba sprawdzić, gdy będzie dostęp do JAR-a gry i dokumentacji, której ta sesja nie miała.

### 6.1 Renderowanie
1. Które wartości `GpuFormat` są dozwolone dla atrybutów wierzchołków (np. `RGBA32_FLOAT`)?
2. Czy niezerowy „step rate" w `VertexFormat.builder(stepRate)` daje instancing i czy `withVertexBinding` przyjmuje kolejne indeksy?
3. Co znaczy liczba `15` w `createTexture(label, 15, ...)`?
4. Czy compute shadery są dostępne w `renderpearl.api`?
5. Konwencja ścieżek shaderów dla pipeline'ów rejestrowanych przez `RenderPipelines.register` (np. `core/position_tex_color` w LiquidBounce) — czy to ścieżki w zasobach gry bez rozszerzenia?
6. Jak wymusić backend (GL/Vulkan/Metal) w uruchomieniu testowym?
7. Jak zachowuje się własny rendering na backendzie Metal?

### 6.2 Narzędzia
1. Czy dwa różne pluginy Loom można mieszać w jednym multi-projekcie?
2. Jak dokładnie dołączyć rdzeń do JAR-a w Loom (include / shadow / jar-in-jar) w obu liniach?
3. Dokładna składnia wpisu class tweakera w `build.gradle` w obu liniach (fragment dołączany w docs).
4. Aktualne „zalecane" wersje z `fabricmc.net/develop` (strona była niedostępna).
5. Dokładna nazwa i sygnatury API `ClientGameTestContext` w obu liniach (np. ekran, czekanie na tiki, `assertScreenshotEquals`).

### 6.3 Wersje
1. Różnice 26.1 → 26.3: kiedy dokładnie zmieniło się API GPU?
2. Czy 26.4 (gałąź Fabric API `26.4`, snapshot) wprowadza kolejne zmiany w renderowaniu? [LUKA]

### 6.4 Produkt
1. Mojang EULA i zasady dotyczące modyfikacji klienta (nie weryfikowane).
2. Strategia licencyjna własnego klienta (jeśli nie GPL).

## 7. Czego tu nie ma (poza zakresem)

- Omijanie anti-cheatów i detekcji serwerów.
- Treści rozgrywki (bloki, encje, sieć) inne niż to, co dotyka klienta i GUI.
- Launcher/aplikacja desktopowa poza grą (osobna interpretacja zadania: „desktopowy klient").
- `owo-lib` (deklaratywne UI) i `imgui-java`: kandydaci na osobne badanie; w klonowanych repozytoriach nie znalazłem dowodów integracji z MC w tych wersjach. [LUKA]

## 8. Zalecana kolejność pierwszych kroków

1. Zbuduj szkielet z `fabric-example-mod` dla jednej linii (rekomendacja: 26.3, najnowsza dokumentacja) i uruchom `runClient`.
2. Dodaj `core` z `Value`, `EventBus` i layoutem plus testy jednostkowe.
3. Dodaj `UiGraphics` + atrapa + pierwszy ekran z prostokątami (bez zaokrągleń).
4. Dodaj backend SDF z zaokrąglonymi rogami (plik 06) i sprawdź otwarte pytania 6.1.
5. Dodaj motyw JSON i dwa style.
6. Dodaj client game test ze zrzutami.
7. Dodaj drugi adapter (1.21.11) i CI z oboma liniami.
8. Dopiero potem efekty (blur, glow), ClickGUI generowany z `Value`, edytor HUD.
