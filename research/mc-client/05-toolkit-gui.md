# 05. Toolkit GUI: projekt biblioteki widgetów

Ten plik opisuje, jak zbudować własną bibliotekę widgetów (okna, panele, przyciski, listy, pola), niezależną od stylu i od linii gry (1.21.11 / 26.x). Opiera się na tym, jak działa GUI Meteora (`gui/**`, 135 plików) oraz na dokumentacji Fabric. Znaczniki jak w [00-index.md](00-index.md). Kod w blokach to **[SZKIC]**, niekompilowany.

## 1. Co udowodniono w kodzie Meteora [KOD]

### 1.1 Drzewo widgetów (tryb retained)
`WWidget` ma: `visible`, `theme`, `x`, `y`, `width`, `height`, `minWidth`, `parent`, `tooltip`, `mouseOver`, `focused`, licznik `mouseOverTimer`.

Przebieg:
1. `init()`: budowa dzieci.
2. `calculateSize()`: wylicza rozmiar (`onCalculateSize()` w podklasach), stosuje `minWidth` przeskalowany przez `theme.scale(...)`, **zaokrągla szerokość i wysokość do całych pikseli**.
3. `calculateWidgetPositions()`: zaokrągla `x`, `y` i układa dzieci (`onCalculateWidgetPositions()`).
4. `render(renderer, mouseX, mouseY, delta)`.

`move(dx, dy)` przesuwa z zaokrągleniem (`Math.round`). Zaokrąglanie do pikseli jest świadomym zabiegiem przeciw „pływającym" krawędziom.

### 1.2 Zdarzenia wejścia
Dla każdego widgetu: `mouseClicked`, `mouseReleased`, `mouseMoved`, `mouseScrolled`, `keyPressed`, `keyRepeated`, `charTyped`, z haczykami `onXxx` dla podklas. Zdarzenia przechodzą z korzenia w dół drzewa.

### 1.3 Kontenery i komórki
Kontenery: pionowa lista, pozioma lista, tabela, sekcja (zwijana), widok przewijany, okno. Układ opisuje **komórka** (`Cell`): wyrównanie, padding, `expandWidgetX/Y`, `minWidth`.

### 1.4 Ekran jako host
`WidgetScreen` trzyma korzeń (`WRoot`), motyw, renderer i animację otwarcia (`animProgress`). Rysuje w `renderCustom`, a klawisze i mysz przekazuje do korzenia. Animacja otwarcia: `animProgress += (delta / 20 * 14) * (closing ? -1 : 1)`, clamp 0..1, używana jako alfa całego drzewa. [KOD, opisane wcześniej]

### 1.5 Tryb debug
`GuiDebugRenderer` rysuje obrysy widgetów korzenia i widgetu podpowiedzi. Bardzo przydatne przy pracy nad układem.

### 1.6 Motyw
`GuiTheme` jest obiektem z ustawieniami (kolory, skala, odstępy) i fabryką widgetów. Domyślny `MeteorGuiTheme` dostarcza konkretne klasy (`MeteorWidget`, `themes/meteor/widgets/*`). Użytkownik edytuje motyw w tym samym GUI, bo motyw to `Settings`. Wartości trójstanowe (normalny / hover / wciśnięty) są wzorcem do powielenia.

## 2. Zasady projektu dla Twojego toolkitu

### Z1. Widget nie zna stylu
Widget deklaruje **rolę** (np. `button`, `button.primary`, `panel`), a motyw dobiera wygląd. Dzięki temu „każdy styl" to wymiana danych, a nie przepisanie widgetów. (Szczegóły: plik 06.)

### Z2. Layout jest oddzielony od rysowania
Faza 1: pomiar i układ (czysta matematyka, testowalna bez GPU). Faza 2: rysowanie z gotowych prostokątów. Dzięki temu da się napisać testy jednostkowe układu (plik 07).

### Z3. Piksele fizyczne i skala
Pracuj w jednostkach logicznych z jednym współczynnikiem skali, a rysuj w pikselach fizycznych (Meteor: `Utils.unscaledProjection()`, `theme.scale(...)`). Zaokrąglaj pozycje i rozmiary do całych pikseli fizycznych. [KOD]

### Z4. Jedno wejście, jeden fokus
Jeden obiekt `InputState` (mysz, klawisze, fokus, przechwycenie myszy). Widget „przechwytujący" (np. przeciągany suwak) może zażądać capture, dopóki nie puszczono przycisku.

### Z5. Brak logiki gry w widgetach
Widgety czytają i zapisują `Value<T>` (plik 03), a nie obiekty modułów. Dzięki temu te same widgety obsłużą ustawienia modułów, HUD i motywu.

## 3. Model danych widgetu [SZKIC]

```java
public abstract class Widget {
    // geometria obliczona przez layout
    public float x, y, w, h;
    public Widget parent;
    public final List<Widget> children = new ArrayList<>();

    // wejście stylu
    public String role = "widget";          // np. "button.primary"
    public final EnumSet<State> states = EnumSet.noneOf(State.class); // HOVER, PRESSED, FOCUSED, DISABLED, ACTIVE

    // layout
    public Constraints constraints = Constraints.DEFAULT;
    public abstract Size measure(Size available, LayoutContext ctx);
    public abstract void arrange(Rect finalRect, LayoutContext ctx);

    // rysowanie (korzysta tylko z UiGraphics i StyleSheet)
    public abstract void draw(UiGraphics g, StyleSheet s, FrameState f);

    // wejście
    public boolean onMouse(MouseEvent e) { return false; }
    public boolean onKey(KeyEvent e)     { return false; }
}
```

`FrameState` przenosi czas (`nowMillis`), mysz i skalę. Animacje czytają `nowMillis`, a nie liczą klatek.

## 4. Układ (layout)

### 4.1 Dwuprzebiegowy układ „measure / arrange"
1. `measure`: rodzic pyta dziecko „ile miejsca potrzebujesz przy dostępnych X×Y?".
2. `arrange`: rodzic przydziela końcowy prostokąt.

### 4.2 Kontenery do zaimplementowania na start
| Kontener | Zachowanie |
|---|---|
| `Row` / `Column` | układ liniowy z odstępem, wyrównaniem i `weight` (rozciąganie) |
| `Stack` | warstwy w jednym miejscu (do ikon na przyciskach, odznak) |
| `Grid` | siatka o stałej liczbie kolumn |
| `ScrollView` | przycięcie scissorem + przesunięcie + pasek przewijania |
| `Panel` / `Window` | ramka z tytułem, przeciąganiem, zwijaniem |
| `Spacer` | pusty element z wagą |

### 4.3 Odpowiednik `Cell` z Meteora
Parametry komórki: `padding`, `alignX/Y`, `expandX/Y`, `minWidth`. W Twoim toolkicie to `Constraints` per dziecko: `margin`, `align`, `weight`, `minSize`, `maxSize`.

### 4.4 Układ a zaokrąglanie
Pozycje liczone w `float`, ale **zaokrąglane przy `arrange`** do pikseli fizycznych (Z3). Unikaj akumulacji błędów: zaokrąglaj współrzędne bezwzględne, nie przyrosty.

## 5. Rysowanie

### 5.1 Widget rysuje przez `UiGraphics`
Zamiast `GuiGraphics` czy pipeline'ów widgety używają interfejsu (plik 08), np. `g.box(rect, boxStyle)`, `g.text(...)`, `g.image(...)`, `g.pushClip(...)`.

### 5.2 Kolejność warstw
Drzewo rysuj w głąb (rodzic, potem dzieci). Elementy „na wierzchu" (menu rozwijane, tooltipy, dialogi) trafiają do osobnej warstwy overlay rysowanej po całym drzewie, żeby nie były przycinane przez scissor rodzica. Meteor ma osobny `renderTooltip` na końcu przebiegu. [KOD]

### 5.3 Batching
Meteor zbiera operacje w dwóch `Renderer2D` (jeden kolorowy, jeden teksturowany) i teksty w osobnej liście, a na końcu rysuje: kolor → tekstury → tekst „normalny" → tekst tytułowy. Dzięki temu cały ekran idzie w kilku draw-callach. [KOD]

W Twoim silniku: zbieraj polecenia w liście z kluczem sortowania (warstwa, pipeline, tekstura), potem grupuj.

## 6. Scissor i przewijanie

Wzorzec z Meteora [KOD]:
- Stos scissorów z pulą obiektów.
- Nowy scissor jest **przecinany z rodzicem** (nigdy nie wychodzi poza obszar rodzica).
- `scissorStart(x, y, w, h)` / `scissorEnd()` wokół zawartości `ScrollView`.
- Zawartość rysowana jest z przesunięciem o offset przewijania; widgety poza widocznym obszarem można pomijać (culling), co oszczędza polecenia.

Przewijanie myszą: `mouseScrolled` w najgłębszym widgecie pod kursorem, który potrafi przewijać; jeśli nie, zdarzenie idzie do rodzica.

## 7. Wejście

### 7.1 Źródła
Hooki do wejścia: zdarzenia klawiatury/myszy z magistrali (plik 03) albo metody `Screen` (`mouseClicked`, `keyPressed`, `charTyped`, `mouseScrolled`). Meteor używa własnych zdarzeń (`KeyInputEvent`, `MouseClickEvent`) do globalnych skrótów (otwarcie GUI), a metod `Screen` do wejścia w ekranie. [KOD]

### 7.2 Mapowanie współrzędnych
Mysz z gry jest w jednostkach „GUI scale". Jeśli rysujesz w pikselach fizycznych, **pomnóż** pozycję myszy przez `guiScale` (Meteor: `mouseX *= s`, gdzie `s = window.getGuiScale()`). [KOD]

### 7.3 Fokus i klawiatura
- Jeden widget z fokusem (`focused`), zmieniany klikiem i klawiszem Tab.
- Pola tekstowe: obsługa `charTyped`, zaznaczanie, kursor, schowek (kopiuj/wklej). Meteor ma w GUI przyciski kopiuj/wklej oparte o ikony. [KOD]
- Blokada klawiszy gry, gdy pole tekstowe ma fokus (inaczej litery wpisywane w polu uruchomią funkcje gry). Meteor utrzymuje `GuiKeyEvents.canUseKeys`. [KOD]

## 8. Animacje

### 8.1 Zasady z dokumentacji [DOK]
- Czas rzeczywisty: `Util.getMillis()`.
- Animacje powiązane z tickiem gry: `DeltaTracker.getGameTimeDeltaPartialTick(false)` (zamarza przy `/tick freeze`).
- Do UI **używaj czasu rzeczywistego**.

### 8.2 Prymityw animacji [SZKIC]
```java
public final class Tween {
    private float from, to; private long start; private int durationMs; private Easing easing;
    public float value(long now) {
        float t = Math.min(1f, (now - start) / (float) durationMs);
        return from + (to - from) * easing.apply(t);
    }
    public boolean done(long now) { return now - start >= durationMs; }
}
```
Typowe czasy z ClickGUI LiquidBounce: przejścia kolorów 0,2 s `ease`; przełączniki i strzałki 0,4 s; rozwinięcie panelu 300 ms. [KOD]

### 8.3 Animacja stanu widgetu
Zamiast przełączać styl natychmiast, animuj **przejście między stanami** (normalny → hover): interpoluj wartości liczbowe stylu (kolor, promień, cień). Motyw definiuje czas i easing dla każdego przejścia.

### 8.4 Pułapki
- Animacje zależne od klatek (`+= 0.1f` na klatkę) zachowują się różnie przy 30 i 240 FPS. Używaj czasu.
- Wartość animowana musi być klampowana (0..1).
- Animacja „zamykania" ekranu musi dokończyć się przed faktycznym zamknięciem (Meteor: `closing` + `animProgress`). [KOD]

## 9. Typy kontrolek, które warto zrobić (kolejność)

Na podstawie listy typów ustawień w ClickGUI LiquidBounce (Boolean, Int, Float, zakresy, Choice, MultiChoose, Color, Key/Bind, Text, Vector, File, listy, Curve) i widgetów Meteora [KOD]:

| Etap | Kontrolki |
|---|---|
| 1 | label, button, checkbox/switch, slider (liczba), textbox |
| 2 | dropdown (enum), keybind, color picker, number input |
| 3 | lista (z dodawaniem/usuwaniem), tabela, zakładki, sekcja zwijana |
| 4 | tooltip z opóźnieniem, menu kontekstowe, dialog potwierdzenia, wyszukiwarka |
| 5 | edytor krzywej, wybór pliku, widget 3D (model gracza) |

Każda kontrolka mapuje się na typ `Value<T>` (plik 03, sekcja 4.3).

## 10. ClickGUI jako przykład kompozycji

Wzorzec (obu klientów): [KOD]
- Okna/panele = kategorie modułów (przeciągalne, zwijalne).
- Wiersz modułu = etykieta + przełącznik; kliknięcie PPM lub strzałka rozwija ustawienia.
- Ustawienia są **generowane z typów** (fabryka widgetów po typie `Value`).
- Wyszukiwarka filtruje moduły po nazwie/aliasie.
- Motyw definiuje wygląd; panel nie zna kolorów.

Meteor ma `DefaultSettingsWidgetFactory`, czyli mapowanie typ ustawienia → widget. [KOD]

## 11. Dostępność i lokalizacja

- **Dostępność:** vanilla ma narrację (`updateWidgetNarration` w `AbstractWidget`). Jeśli używasz własnych widgetów w `Screen`, zaimplementuj metody narracji, żeby czytniki ekranu nie traciły informacji. [DOK]
- **Kontrast:** kolory akcentu i tła sprawdzaj pod kątem czytelności (np. kontrast tekstu do tła co najmniej 4,5:1). [WIEDZA]
- **Lokalizacja:** teksty UI przez klucze tłumaczeń (`text-and-translations` w dokumentacji); klawisze mają własne klucze kategorii `key.category.<ns>.<path>`. [DOK]
- **Układ RTL / CJK:** LiquidBounce ma osobne czcionki z fallbackiem (Segoe UI / Helvetica / DejaVu; CJK osobno), co pokazuje, że fonty trzeba planować od początku. [KOD]

## 12. Plan implementacji toolkitu (kolejność) [SZKIC]

1. Geometria i layout (`Rect`, `Size`, `Constraints`, `Row/Column/Stack`) + testy jednostkowe.
2. `UiGraphics` w wersji z atrapą (nagrywa polecenia do listy) + testy rysowania jako listy poleceń.
3. Jeden backend (np. prostokąty przez `GuiGraphics`) do sprawdzenia całości.
4. Widgety etapu 1.
5. Wejście, fokus, scissor, przewijanie.
6. Motyw i `StyleSheet` (plik 06).
7. Własny backend z zaokrąglonymi rogami (SDF).
8. Animacje stanów.
9. ClickGUI generowany z `Value`.
10. Narzędzia: tryb debug (obrysy), zrzuty ekranu (plik 07).
