# 07. Testy, CI i debugowanie GUI

Jak testować klient z własnym GUI na 1.21.11 i 26.x. Źródła: `fabric-docs` (`automatic-testing.md`, `loom/fabric-api.md`, `debugging.md`, `reference/latest/**`), kod Meteora (tryb debug). Znaczniki jak w [00-index.md](00-index.md). Kod to **[SZKIC]**, o ile nie podano [DOK].

## 1. Co i czym testować (piramida)

| Poziom | Co sprawdza | Narzędzie | Potrzebuje gry? |
|---|---|---|---|
| 1. Logika czysta | layout, parsowanie motywów, easingi, `Value`, serializacja | JUnit 5 | nie |
| 2. Logika z rejestrami MC | rzeczy używające `ItemStack`, rejestrów | Fabric Loader JUnit | częściowo |
| 3. Lista poleceń rysowania | co widget rysuje (bez GPU) | JUnit + atrapa `UiGraphics` | nie |
| 4. Zrzuty ekranu | jak GUI wygląda naprawdę | Fabric Client Game Test | tak |
| 5. Ręcznie | wrażenie, płynność, kolizje z modami | `runClient` | tak |

Zasada: **jak najwięcej w poziomach 1 i 3**, bo są szybkie i deterministyczne.

## 2. Testy jednostkowe: Fabric Loader JUnit [DOK]

Zwykłe JUnit nie wystarczy, bo modowanie Minecrafta opiera się na modyfikacji bajtkodu (Mixin). Fabric dostarcza wtyczkę JUnit.

### 2.1 Konfiguracja
1. Dodaj Fabric Loader JUnit do zależności testowych w `build.gradle` (dokładne współrzędne i blok `test` są w `develop/automatic-testing.md` jako fragmenty `automatic_testing_1` i `automatic_testing_2`; nie przepisuję ich, bo w pliku są dołączane z innego miejsca).
2. Testy trzymaj w `src/test/java`, w tym samym pakiecie co testowana klasa (dostęp do metod pakietowych), z sufiksem `Test`.
3. Test uruchamia się przy `build` oraz w IDE (zielone strzałki).

### 2.2 Inicjalizacja rejestrów
Jeśli testowana klasa dotyka rejestrów (np. `ItemStack`), a gra nie jest zainicjalizowana, test pada z błędem inicjalizacji. Rozwiązanie z dokumentacji: dodaj na początku `beforeAll` kod bootstrapu (fragment `automatic_testing_7`). [DOK]

### 2.3 CI: raporty
W GitHub Actions po kroku `./gradlew build` dodaj krok z `if: failure()` wysyłający artefakty `**/build/reports/` i `**/build/test-results/`. [DOK]

## 3. Testy układu (layout) bez GPU [SZKIC]

Layout (plik 05) jest czystą matematyką, więc testuj go bez gry.

```java
class ColumnLayoutTest {
    @Test void stacksChildrenWithGap() {
        var col = new Column(gap(4));
        col.add(new FixedBox(100, 20));
        col.add(new FixedBox(100, 30));
        var size = col.measure(Size.of(500, 500), ctx());
        col.arrange(Rect.of(0, 0, size.w, size.h), ctx());
        assertEquals(54, size.h);                 // 20 + 4 + 30
        assertEquals(24, col.child(1).y);         // 20 + 4
    }
    @Test void roundsToPhysicalPixels() {
        var ctx = ctx().withScale(1.5f);
        var box = new FixedBox(10, 10);
        box.arrange(Rect.of(0.3f, 0.3f, 10, 10), ctx);
        assertEquals(Math.round(0.3f * 1.5f), box.px().x);
    }
}
```

Co warto pokryć: wagi i rozciąganie, minimalne/maksymalne rozmiary, marginesy, zaokrąglanie, przewijanie (offset), pusty kontener, bardzo duże i zerowe rozmiary.

## 4. Testy rysowania przez atrapę `UiGraphics` [SZKIC]

Zamiast sprawdzać piksele, zapisz **listę poleceń**, które widget wysłał:

```java
class RecordingGraphics implements UiGraphics {
    final List<Cmd> cmds = new ArrayList<>();
    public void box(Rect r, BoxStyle s) { cmds.add(new Cmd.Box(r, s)); }
    public void text(String s, Font f, float x, float y, Paint p) { cmds.add(new Cmd.Text(s, x, y)); }
    public void pushClip(Rect r, float[] radii) { cmds.add(new Cmd.Push(r)); }
    public void popClip() { cmds.add(Cmd.POP); }
    // ... reszta
}

@Test void buttonDrawsBoxThenLabel() {
    var g = new RecordingGraphics();
    new Button("OK").layoutAndDraw(g, theme("flat"));
    assertInstanceOf(Cmd.Box.class,  g.cmds.get(0));
    assertInstanceOf(Cmd.Text.class, g.cmds.get(1));
}
```

Plus: test, że `push/pop` jest zbilansowany, że stan `hover` zmienia styl, że wyłączony widget ma styl `disabled`.

To samo narzędzie służy do testów **parzystości między liniami**: ta sama scena drawing-list musi być identyczna dla 1.21.11 i 26.x (różnią się tylko backendy).

## 5. Fabric Client Game Test: zrzuty ekranu [DOK]

Client game test uruchamia prawdziwego klienta i pozwala robić zrzuty oraz symulować wejście.

### 5.1 Konfiguracja przez Loom
W `build.gradle` (przykład z dokumentacji, `automatic_testing_game_test_1`):
```gradle
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "example-mod-test-${project.name}"
        enableGameTests = true            // domyślnie true
        enableClientGameTests = true      // domyślnie true
        eula = true                       // akceptacja EULA Minecrafta
    }
}
```
Opcje (z `loom/fabric-api.md`): `createSourceSet` (osobny source set `gametest`, domyślnie `false`), `modId` (wymagane przy `createSourceSet`), `enableGameTests`, `enableClientGameTests`, `eula` (domyślnie `false`), `clearRunDirectory` (domyślnie `true`), `username` (domyślnie `Player0`).

### 5.2 `fabric.mod.json` testów
W `src/gametest/resources/fabric.mod.json`: [DOK, przykład z repo dokumentacji]
```json
{
  "schemaVersion": 1,
  "id": "example-mod-test",
  "version": "1.0.0",
  "environment": "*",
  "entrypoints": {
    "fabric-gametest": [ "com.example.docs.ExampleModGameTest" ],
    "fabric-client-gametest": [ "com.example.docs.ExampleModClientGameTest" ]
  }
}
```

### 5.3 Pierwszy test
Wzorzec z dokumentacji: [DOK]
```java
public class ExampleModClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            context.takeScreenshot("example-mod-singleplayer-test");
        }
    }
}
```
W 1.21.11 klasy testowe są w tym samym pakiecie `net.fabricmc.fabric.api.client.gametest.v1`. Różnice nazw (np. `getClientWorld` kontra `getClientLevel`) sprawdź w Javadocu obu wersji. [WIEDZA, patrz tabela renamów w pliku 02]

### 5.4 Uruchamianie
- Serwerowe testy idą w zadaniu `build`.
- Klientowe: `./gradlew runClientGameTest`. [DOK]

### 5.5 CI na GitHub Actions
Dokumentacja podaje zadanie produkcyjne: [DOK]
```gradle
tasks.register("runProductionClientGameTest", net.fabricmc.loom.task.prod.ClientProductionRunTask) {
    jvmArgs.add("-Dfabric.client.gametest")
    // XVFB: wirtualny framebuffer dla środowisk bez ekranu; domyślnie true tylko na Linuksie
    // gdy ustawiona jest zmienna środowiskowa "CI". Wymaga: apt install xvfb
    useXVFB = true
}
```
Jeśli test pada przez błąd „network synchronizer", dodaj do JVM args: `-Dfabric.client.gametest.disableNetworkSynchronizer=true`. [DOK]

Potrzeba też zadania w workflow, które uruchamia powyższy `runProductionClientGameTest` (fragment `automatic_testing_game_test_3` w `.github/workflows/build.yaml` repo dokumentacji).

### 5.6 Test GUI: przepis [SZKIC]
```java
public void runTest(ClientGameTestContext context) {
    context.setScreen(() -> new DemoClickGuiScreen(Theme.load("flat")));
    context.waitTicks(2);                         // pozwól wykonać layout i pierwszy render
    context.takeScreenshot("clickgui-flat");

    context.setScreen(() -> new DemoClickGuiScreen(Theme.load("glass")));
    context.waitTicks(2);
    context.takeScreenshot("clickgui-glass");
}
```
Nazwy metod `setScreen`/`waitTicks` są przykładowe; **sprawdź w Javadocu** `ClientGameTestContext` w Twojej wersji Fabric API. [LUKA]

### 5.7 Porównywanie zrzutów
Dokumentacja i wcześniejsza analiza wskazują na `assertScreenshotEquals(...)` z porównaniem „fuzzy" względem obrazu wzorcowego. [DOK/KOD, szczegóły sygnatury sprawdź w Javadocu] Zasady:
1. Zrzuty wzorcowe trzymaj w repo (np. `src/gametest/resources/golden/<linia>/<nazwa>.png`), **osobno dla każdej linii gry**, bo różnice w antyaliasingu i fontach są możliwe.
2. Używaj tolerancji (kilka procent różnych pikseli), bo GPU/sterowniki dają drobne różnice.
3. Stała rozdzielczość okna i stały GUI scale.
4. Wyłącz animacje lub ustaw je w stan końcowy przed zrzutem (stały czas lub tryb „bez animacji").
5. Deterministyczny świat i pora dnia, gdy zrzut obejmuje tło sceny; najlepiej testuj GUI na stałym tle (menu główne, pusty ekran).

## 6. Macierz testów

| | 1.21.11 | 26.x (GL) | 26.x (Vulkan) |
|---|---|---|---|
| Testy jednostkowe layoutu | ✔ | ✔ | n/d |
| Lista poleceń rysowania | ✔ | ✔ | n/d |
| Zrzuty ClickGUI (wszystkie motywy) | ✔ | ✔ | ✔ |
| Zrzuty HUD | ✔ | ✔ | ✔ |
| Test uruchomienia z Sodium/Iris | ręcznie | ręcznie | ręcznie |

Backend Vulkan (od 26.2) trzeba sprawdzić osobno, bo własne pipeline'y mogą zachowywać się inaczej. Jak wymusić backend w uruchomieniu testowym, **nie wiem** (nie znalazłem w docs). [LUKA]

## 7. Debugowanie

### 7.1 Logowanie [DOK]
Logger z nazwą modu (SLF4J). Poziom `debug` włączasz osobno. Trzymaj konsolę czystą: nie loguj w pętli renderowania na poziomie `info`.

### 7.2 Breakpointy i hotswap [DOK]
Dokumentacja opisuje breakpointy w IntelliJ, warunki/przełączniki oraz hotswap w działającej instancji. Hotswap działa dla zmian ciał metod; zmiany w mixinach i strukturze klas wymagają restartu. [WIEDZA co do ograniczeń hotswapu]

### 7.3 Debug układu GUI (wzorzec Meteora) [KOD]
`GuiDebugRenderer` rysuje ramki widgetów. Zbuduj własny: przełącznik (klawisz/komenda), który po drzewie widgetów rysuje obrys `x,y,w,h`, kolor zależny od głębokości, etykietę z rolą i stanem. Jedno z najtańszych i najpożyteczniejszych narzędzi.

### 7.4 Narzędzia dla rendera
1. Nazwane render passy: wszystkie `createRenderPass(() -> "nazwa", ...)` przyjmują dostawcę etykiety; nadawaj czytelne nazwy (Meteor: `"Meteor MeshRenderer"`), bo narzędzia (RenderDoc, debug GL) je pokazują. [KOD]
2. RenderDoc: do inspekcji przebiegów (przy OpenGL; przy Vulkanie też obsługuje). Nie sprawdzałem współpracy z Minecraftem 26.x. [WIEDZA, niezweryfikowane]
3. Weryfikuj shadery przy starcie: prekompilacja pipeline'ów (Meteor) ujawnia błędy kompilacji od razu. [KOD]
4. Zrzut do pliku w teście: `takeScreenshot` zapisuje obraz, który możesz obejrzeć po nieudanym CI.

### 7.5 Typowe objawy i przyczyny [WIEDZA]

| Objaw | Prawdopodobna przyczyna |
|---|---|
| Nic się nie rysuje | pipeline nie skompilowany/zarejestrowany; zła kolejność wierzchołków (culling); alfa 0; zły scissor |
| Prostokątna poświata wokół zaokrąglonego rogu | brak `discard` przy alfa ≈ 0 lub brak mnożenia alfy przez maskę |
| Rozmyty tekst | pozycje ułamkowe, brak zaokrąglenia do pikseli fizycznych |
| Elementy z cudzą transformacją | nieskopiowane `pose()` w elemencie stanu |
| Zawartość „skacze" przy skalowaniu okna | tekstury offscreen nie odtworzone po zmianie rozdzielczości |
| Crash przy starcie po zmianie shadera | niezgodność `layout(location)` z `VertexFormat` |
| Różny wygląd na Vulkanie | zależność od niezdefiniowanego zachowania w shaderze; sprawdź inicjalizację zmiennych |

## 8. Lista kontrolna jakości przed wydaniem

- [ ] Testy jednostkowe layoutu zielone.
- [ ] Parzystość list poleceń między liniami.
- [ ] Zrzuty ze wszystkich motywów dla obu linii zatwierdzone.
- [ ] Brak logów `warn/error` z renderowania w czystym uruchomieniu.
- [ ] Sprawdzone z Sodium i Iris (jeśli deklarujesz zgodność).
- [ ] Sprawdzone przy różnych GUI scale (1, 2, 3, 4) i rozdzielczościach.
- [ ] Zmiana rozmiaru okna w trakcie otwartego GUI nie psuje wyglądu.
- [ ] Zapis konfiguracji przetrwał wymuszone zamknięcie gry (plik nie jest uszkodzony).
