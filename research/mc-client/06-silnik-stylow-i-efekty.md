# 06. Silnik stylów i efekty: dowolny wygląd GUI

Plik opisuje, jak sprawić, żeby GUI mogło mieć **dowolny styl**: od płaskiego, przez szkło i neon, po pixel-art. Opiera się na wzorcach z Meteora i LiquidBounce oraz dokumentacji. Znaczniki jak w [00-index.md](00-index.md). Kod shaderów i Javy to **[SZKIC]**. Niczego nie kompilowałem ani nie uruchamiałem.

## 1. Model: prymitywy + styl jako dane + efekty

Trzy niezależne warstwy (rozwinięcie sekcji 4.5 starszego raportu):

| Warstwa | Odpowiada za | Zmienia się, gdy |
|---|---|---|
| **Prymitywy** | co da się narysować (kształty, wypełnienia, cienie, tekst, obraz) | dodajesz nową zdolność wizualną |
| **Styl** (dane) | wartości prymitywów dla ról widgetów | zmieniasz wygląd |
| **Efekty** | przebiegi offscreen (blur, glow, kompozycja) | dodajesz efekt pełnoekranowy |

Żaden widget nie wywołuje pipeline'ów wprost.

## 2. Zestaw prymitywów

| Prymityw | Parametry | Technika | Status wiedzy |
|---|---|---|---|
| **Box** | prostokąt, 4 promienie rogów, wypełnienie, obramowanie, cienie | SDF w fragment shaderze | podstawa [KOD] (LiquidBounce: promień jeden, obrys), reszta [WIEDZA] |
| **Gradient** | liniowy, radialny, stożkowy; kolory + punkty | parametry w atrybutach/UBO | pionowy [KOD], reszta [WIEDZA] |
| **Shadow** | offset, blur, spread, kolor, inner | rozszerzony SDF | [WIEDZA] |
| **Glow** | kolor, promień, siła | SDF lub blur kształtu | [WIEDZA] |
| **Image** | tekstura, region UV, tint, 9-slice, filtr | quad teksturowany | [KOD] pipeline teksturowany |
| **Path** | kontur, wypełnienie, obrys | teselacja do trójkątów | [WIEDZA] |
| **Text** | font, rozmiar, kolor, wyrównanie, cień | atlas glifów / MSDF | atlas [KOD], MSDF [WIEDZA] |
| **Clip** | prostokąt/zaokrąglony prostokąt | scissor / SDF w shaderze | scissor [KOD], zaokrąglony [WIEDZA] |
| **Layer** | alfa, transformacja, blur tła, tryb mieszania | render do tekstury + kompozycja | [KOD] wzorzec offscreen |

## 3. SDF: matematyka

### 3.1 Zaokrąglony prostokąt (jeden promień)
```
q = abs(p) - b + r
d = length(max(q, 0)) + min(max(q.x, q.y), 0) - r       // d < 0 wewnątrz
alpha = 1 - smoothstep(-aa, 0, d),  aa = fwidth(d)
```
`p` względem środka, `b` połowa rozmiaru, `r` promień. Postać znana z materiałów Inigo Quilez; LiquidBounce używa tej samej (`sdRoundBox`). [KOD + WIEDZA o pochodzeniu]

### 3.2 Cztery promienie
Wybierz promień po ćwiartce `p`, a potem licz jak wyżej. Ten sam wzór: prawa/lewa połowa wybiera parę, góra/dół wybiera jeden. [WIEDZA]

### 3.3 Obramowanie
```
inner = smoothstep(-w - aa, -w + aa, d)       // 1 poza wewnętrzną krawędzią obrysu
edge  = (1 - smoothstep(-aa, 0, d)) * inner
```
`w` to grubość linii. LiquidBounce rysuje wypełnienie i obrys **dwoma quadami** tego samego pipeline'u (obrys ma `LINE_WIDTH > 0`). [KOD]

### 3.4 Cień
Większy quad pod elementem; dla punktu wewnątrz quada policz `d` kształtu cienia, a alfę jako `exp(-k * max(d, 0))` albo `smoothstep(blur, 0, d)`. Miękkość = `blur`, rozmiar = `spread`, przesunięcie = `offset`. [WIEDZA]

### 3.5 Cień wewnętrzny
Odwróć znak `d` i licz zanik od krawędzi do środka; maskuj kształtem boxa. [WIEDZA]

### 3.6 Gradient
- Liniowy: `t = dot(p - p0, normalize(p1 - p0)) / length(p1 - p0)`, `mix(c0, c1, clamp(t, 0, 1))`.
- Radialny: `t = length(p - c) / R`.
- Stożkowy: `t = atan(p.y - c.y, p.x - c.x) / (2π) + 0.5`.
[WIEDZA]

### 3.7 Antyaliasing
`aa = max(fwidth(d), 1e-4)` daje szerokość ok. 1 piksela w jednostkach `d`, więc AA jest poprawny przy każdej skali, jeśli `d` liczysz w jednostkach, w których rysujesz. [WIEDZA, wniosek z konstrukcji]

## 4. Shader „uber-box" [SZKIC]

Cel: jeden pipeline pokrywający Box + obramowanie + gradient + (opcjonalnie) cień. Wersja dla 26.x (z `layout(location)` i rozszerzeniem):

### Vertex
```glsl
#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;          // 0..1 w obrębie quada
layout(location = 2) in vec4 Color;        // wypełnienie (góra)
layout(location = 3) in vec4 Color2;       // wypełnienie (dół) lub obrys
layout(location = 4) in vec4 Params0;      // szerokość, wysokość, grubość obrysu, blur cienia
layout(location = 5) in vec4 Radii;        // 4 promienie rogów

layout(location = 0) out vec2 vUv;
layout(location = 1) out vec4 vColor;
layout(location = 2) out vec4 vColor2;
layout(location = 3) flat out vec4 vParams0;
layout(location = 4) flat out vec4 vRadii;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vUv = UV0; vColor = Color; vColor2 = Color2; vParams0 = Params0; vRadii = Radii;
}
```
Nazwy `ProjMat` i `ModelViewMat` pochodzą z dołączanych plików `minecraft:` w shaderze LiquidBounce 26.3. [KOD] Dla własnego pipeline'u poza rejestrem vanilli użyj własnego bloku `MeshData` jak w Meteorze.

### Fragment
```glsl
#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 vUv;
layout(location = 1) in vec4 vColor;
layout(location = 2) in vec4 vColor2;
layout(location = 3) flat in vec4 vParams0;
layout(location = 4) flat in vec4 vRadii;
layout(location = 0) out vec4 color;

float sdBox(vec2 p, vec2 b, vec4 r) {
    // r: x=prawy-górny, y=prawy-dolny, z=lewy-dolny, w=lewy-górny; p.y rośnie ku górze
    vec2 rs = (p.x > 0.0) ? r.xy : r.wz;
    float rr = (p.y > 0.0) ? rs.x : rs.y;
    vec2 q = abs(p) - b + rr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rr;
}

void main() {
    vec2 size = vParams0.xy;
    float bw = vParams0.z;
    vec2 p = (vUv - 0.5) * size;
    p.y = -p.y;                                   // UV rośnie w dół
    float d = sdBox(p, 0.5 * size, vRadii);
    float aa = max(fwidth(d), 1e-4);

    float shape = 1.0 - smoothstep(-aa, 0.0, d);
    vec4 fill = mix(vColor, vColor2, vUv.y);      // gradient pionowy
    float inner = (bw > 0.0) ? smoothstep(-bw - aa, -bw + aa, d) : 0.0;
    vec4 c = mix(fill, vColor2, inner);           // obrys w kolorze 2
    c.a *= shape;
    if (c.a <= 0.001) discard;
    color = c;
}
```

Zastrzeżenia:
- Format wierzchołków z atrybutem `vec4` wymaga tego, by `GpuFormat` takie dopuszczał (`RGBA32_FLOAT`?). **Nie sprawdzałem**, które wartości `GpuFormat` są dozwolone. [LUKA]
- W 1.21.11 ten sam shader musi mieć inny nagłówek i bloki uniformów ręcznie (plik 02, sekcja 8).
- W LiquidBounce jeden atrybut z promieniem jest kodowany w `ivec2`; przy czterech promieniach potrzeba więcej danych, np. dwa kanały `ivec2`. [WIEDZA]

## 5. Style jako dane

### 5.1 Tokeny
```json
{
  "tokens": {
    "color.accent":  "#4677ff",
    "color.panel":   "#000000e6",
    "color.text":    "#ffffff",
    "radius.s": 3, "radius.m": 5, "radius.l": 10,
    "space.s": 5, "space.m": 10, "space.l": 15,
    "font.base": { "family": "Inter", "size": 12, "weight": 500 },
    "motion.fast": 200, "motion.slow": 400
  }
}
```

### 5.2 Klasy stylów
```json
{
  "button": {
    "box": { "fill": "color.panel", "radius": "radius.m", "border": null },
    "states": {
      "hover":   { "box": { "fill": "color.accent" }, "transition": "motion.fast" },
      "pressed": { "box": { "fill": "color.accent", "shadow": null } }
    }
  },
  "panel.glass": {
    "layer": { "backdropBlur": 12 },
    "box":   { "fill": "#ffffff22", "radius": "radius.l",
               "border": { "width": 1, "color": "#ffffff44" } }
  }
}
```

### 5.3 Rozwiązywanie stylu
1. Weź klasę widgetu po `role` (z dziedziczeniem: `button.primary` rozszerza `button`).
2. Nałóż nadpisania stanów (`hover`, `pressed`, `disabled`).
3. Zastąp referencje tokenów wartościami.
4. Jeśli stan się zmienił, uruchom animację przejścia (plik 05, sekcja 8.3).
5. Wynik to `BoxStyle`/`TextStyle`/`LayerStyle` przekazane do `UiGraphics`.

### 5.4 Wzorce istniejące w klientach [KOD]
- Meteor: motyw jako `Settings`, trzy stany koloru (normalny, hover, wciśnięty), domyślny akcent `(145, 61, 226)`.
- LiquidBounce: motywy jako osobne aplikacje webowe ładowane z URL-a i rozwijane z hot reloadem.

### 5.5 Hot reload
Dla motywu w pliku: obserwuj plik (`WatchService`) lub przeładowuj przy F3+T przez `ResourceManagerReloadListener`; po wczytaniu przebuduj tablicę stylów i oznacz drzewo jako „brudne" (relayout + redraw). Nie przeładowuj na wątku renderowania podczas rysowania. [WIEDZA]

## 6. Wybrane tokeny designu z ClickGUI LiquidBounce [KOD]

| Element | Wartość |
|---|---|
| Akcent | `#4677ff` |
| Panel | szerokość 250 px, `border-radius: 5px`, cień `0 0 10px` (czerń 50%) |
| Nagłówek | tło czerń 90%, dolna linia 2 px w akcencie, padding `10px 15px`, tytuł 14 px / waga 500 |
| Ciało panelu | czerń 80%, hover modułu 85% |
| Moduł | tekst 12 px / waga 500, padding 10 px, włączony = akcent |
| Ustawienia | tło czerń 50%, lewy pasek 4 px w akcencie |
| Przełącznik | tor 22×12 (`border-radius: 4px`), gałka 12 px okrągła |
| Scrollbar | 2 px, zaokrąglony |
| Przejścia | kolory 0,2 s `ease`; przełączniki i strzałki 0,4 s; panel 300 ms `ease` |
| Font | Inter (Light/Regular/Medium/Bold) |
| Promienie | najczęściej 3 i 5 px, okręgi 50% |

Wniosek projektowy: spójne małe promienie, półprzezroczyste ciemne warstwy, jeden akcent, delikatny cień, przejścia 200–400 ms. [WIEDZA co do oceny estetycznej]

## 7. Przepisy na style

Dla każdego stylu: które prymitywy i parametry.

### 7.1 Płaski / Material
Box (promień 4–8), jedno wypełnienie, cień `0 2 8` czerń 30%, akcent na stanach. Bez blura.

### 7.2 Glassmorphism
Warstwa z blurem tła (sekcja 8), Box z półprzezroczystym wypełnieniem (alfa 10–20%) i jasnym obrysem 1 px. Wymaga przebiegu offscreen przed rysowaniem panelu.

### 7.3 Neumorfizm
Box w kolorze tła + dwa cienie (jasny z przesunięciem w lewo-górę, ciemny w prawo-dół) + subtelny gradient. Dla stanu wciśniętego użyj cieni wewnętrznych.

### 7.4 Neon / cyberpunk
Obrys 1–2 px w jaskrawym kolorze + glow (cień zewnętrzny z dużym blurem tym samym kolorem) + ciemne tło. Opcjonalnie lekki szum.

### 7.5 Pixel-art / retro
Sprite'y 9-slice, filtr `NEAREST`, skalowanie całkowite, font bitmapowy. Wyłącz AA dla krawędzi (promień 0, brak SDF). Skala UI musi być liczbą całkowitą.

### 7.6 Skeuomorficzny
Tekstury tła (obrazy), cienie wewnętrzne, gradienty, ikony rastrowe. Image + Box + Shadow.

### 7.7 Styl zbliżony do vanilli
Sprite'y z zasobów gry i `blit` (ścieżka A w starszym raporcie). Najprościej, ale bez AA przy skalowaniu.

## 8. Efekty wieloprzebiegowe

### 8.1 Blur (down/up-sampling) [KOD]
Schemat Meteora:
1. Utwórz N tekstur o skalach `1/2^i`.
2. Przebiegi **w dół**: z tekstury i do i+1 shaderem uśredniającym 5 próbek (jedna z wagą 4 + cztery narożne, podzielone przez 8).
3. Przebiegi **w górę**: odwrotnie, z innym jądrem.
4. Parametry: `u_HalfTexelSize` (1/(2·rozmiar)) i `u_Offset` (siła) w uniform buffer.
5. Kompozycja: shader „passthrough" na ekran.

Zwiększ siłę rozmycia przez liczbę iteracji i offset, nie przez rozmiar jądra. LiquidBounce używa zamiast tego rozdzielonego blura poziomego/pionowego (`ui_blur_h/_v`). [KOD]

### 8.2 Blur tła pod panelem (glassmorphism)
1. Skopiuj aktualny kolor sceny do tekstury (lub użyj kopii z pierwszego przebiegu GUI).
2. Rozmyj do tekstury końcowej.
3. Rysuj panel, próbkując rozmytą teksturę **w kształcie panelu** (SDF jako maska), z mieszaniem z półprzezroczystym wypełnieniem.

### 8.3 Glow
Dwie drogi: (a) cień zewnętrzny SDF z dużym blurem i jaskrawym kolorem (tani); (b) renderuj kształt do tekstury, rozmyj, nałóż addytywnie (droższy, lepszy dla dowolnych kształtów).

### 8.4 Przejścia między ekranami
Renderuj stary i nowy ekran do osobnych tekstur, komponuj shaderem z parametrem `t` (przenikanie, przesunięcie, rozmycie). [WIEDZA]

### 8.5 Cache statycznych warstw
Jeśli panel się nie zmienia, narysuj go raz do tekstury i wyświetlaj kolejne klatki z cache (inwalidacja przy zmianie stanu/rozmiaru). Zyskujesz na wydajności kosztem pamięci. [WIEDZA]

## 9. Tekst

Opcje, od najprostszej:

1. **Tekst vanilli** (`text` / `drawString`): font Minecrafta, kolor ARGB. Dla 1.21.11 `drawString(font, text, x, y, argb, shadow)`, dla 26.x `text(...)`. [DOK]
2. **Atlas TTF → tekstura jednokanałowa** (STB TrueType) i własny pipeline tekstowy: kolor × alfa z tekstury. Tak robi Meteor. [KOD]
3. **Menedżer glifów** (LiquidBounce): „glyph pages", atlas glifów, fonty systemowe z fallbackiem, osobno CJK. [KOD]
4. **MSDF**: ostry tekst przy dowolnym skalowaniu. Nie widziałem w kodzie klientów. [WIEDZA]

Reguły: cache glifów, batch tekstu per font, wyrównanie do pikseli fizycznych (rozmycie tekstu przy ułamkowych pozycjach), kerning i tekst wielojęzyczny wymagają świadomych decyzji (HarfBuzz/ICU w Javie nie jest trywialne). [WIEDZA]

## 10. Wydajność

| Zagadnienie | Zalecenie |
|---|---|
| Liczba draw-calli | sortuj po (warstwa, pipeline, tekstura), batchuj |
| Overdraw | `discard` przy alfa ≈ 0; mały quad wokół kształtu |
| Alokacje | pule obiektów (Meteor: `Pool<Scissor>`, `Pools.Mat3x2f`) [KOD] |
| Blur | w obniżonej rozdzielczości; ogranicz iteracje |
| Layout | licz tylko, gdy coś się zmieniło (flaga „brudne") |
| Cache | statyczne panele do tekstury |
| Profilowanie | bez mierzenia nie optymalizuj; **nie mierzyłem** wydajności żadnego z opisanych rozwiązań [LUKA] |

## 11. Ograniczenia (uczciwie)

1. Liczba parametrów na element: format wierzchołków może być zbyt ciasny; alternatywy: UBO/tekstura danych/instancing (nieweryfikowane w kodzie klientów). [WIEDZA]
2. Compute shadery: niesprawdzone. [LUKA]
3. Zgodność z modami renderującymi (Sodium, Iris): wymaga testów. [KOD: Meteor ma mixiny zgodności]
4. Pełne możliwości CSS/SVG (filtry, złożona typografia) tanio odtwarza tylko ścieżka z przeglądarką. [WIEDZA]
5. Każda nowa wersja gry może zmienić wnętrza: izoluj backend (plik 08).
