# Dowolne GUI (2D i 3D) na Minecraft 1.21.11 i najnowszym, w Mojang mappings

- **Data:** 2026-11-06
- **Uzupełnia:** [`minecraft-client-gui-1.21.11.md`](minecraft-client-gui-1.21.11.md) (sekcje 4.4, 4.5, 5)
- **Kod:** [`../gui-toolkit`](../gui-toolkit): działa i ma testy (9/9).

Znaczniki: **[ZWERYFIKOWANE]** = uruchomiłem w tej sesji; **[KOD]/[DOK]** = z kodu Meteora/LiquidBounce albo
`fabric-docs` (patrz główny raport); **[SZKIC]** = nie kompilowane, nie uruchamiane.

## 1. Czy może być „na 100%"? Uczciwa odpowiedź

Gwarancji „na 100%" nie da się uczciwie dać, dopóki żaden backend nie został skompilowany z prawdziwym jarem
Minecrafta (tu brak dostępu do `maven.fabricmc.net`, `meta.fabricmc.net`, serwerów Mojanga). Da się natomiast
zbudować architekturę, w której **ograniczeniem jest tylko to, co potrafi GPU i `RenderPass`, a nie toolkit**:

1. **Toolkit nie jest sufitem.** Wszystko, czego nie opisują gotowe polecenia, idzie przez `DrawCommand.Custom`:
   backend dostaje dowolny ładunek i rysuje własnym shaderem/render passem. Dodatkowo `Mesh3D` pokrywa 3D.
2. **Toolkit jest sprawdzony** [ZWERYFIKOWANE]: layout, trafianie myszą, klik, slider, clip, matematyka SDF,
   macierze i picking promieniem w panelu 3D.
3. **Ryzyko jest zawężone do jednej, małej rzeczy:** dwóch backendów (po jednym na wersję). To one muszą
   przejść bramki z sekcji 6. Dopiero wtedy można mówić „działa".

## 2. Co zostało zrobione (w repozytorium, działa)

`gui-toolkit` (Java 21, zero zależności): matematyka (`Mat4`, `Ray`, `Sdf`), polecenia rysowania, widgety,
motyw, animacje. Uruchom: `cd gui-toolkit && gradle check-ui`.

## 3. Mapa: „chcę X" → jak to się robi

| Chcę | Polecenie / mechanizm | Wymagania od backendu |
|---|---|---|
| Zaokrąglony prostokąt, obramowanie | `RoundedRect` | pipeline SDF (sekcja 4) |
| Cień, poświata | `Shadow` | shader rozmycia SDF |
| Szkło, blur tła | `BackdropBlur` | kopia framebufferu do tekstury + down/up-sampling (wzór: `Blur.java` w Meteorze [KOD]) |
| Tekst, ikony, obrazy | `Text`, `Image` | atlas glifów, własne tekstury |
| Przewijane listy, panele | `PushClip`/`PopClip` | scissor (zagnieżdżalny) |
| Model 3D w oknie GUI | `Mesh3D` + `Space.GUI_RECT` | render-to-texture z głębią, potem quad w GUI |
| Panel/hologram w świecie | `Mesh3D` + `Space.WORLD`, klik przez `Ray.hitPanel` | rysowanie w fazie świata z macierzami kamery |
| Cały ekran jako scena 3D | `Mesh3D` na pełnym oknie, wejście przez `Ray.fromScreen` | wyczyszczenie głębi przed 3D, przywrócenie stanu po |
| Efekt, którego nie przewidziałem | `Custom("klucz", ładunek)` | backend rejestruje handler pod kluczem |
| UI jak aplikacja webowa (CSS) | poza toolkitem: ścieżka D z głównego raportu | CEF, ciężkie zależności |

## 4. Shader zaokrąglonego prostokąta (ta sama matematyka, co `Sdf.java`) [SZKIC]

```glsl
#version 330
in vec2 vLocal;      // piksele od środka prostokąta
in vec2 vHalf;       // połowa wymiarów
in float vRadius;
in vec4 vColor;
out vec4 fragColor;

float roundedBox(vec2 p, vec2 half, float r) {
    r = min(r, min(half.x, half.y));
    vec2 q = abs(p) - (half - r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}
void main() {
    float d = roundedBox(vLocal, vHalf, vRadius);
    float a = clamp(0.5 - d, 0.0, 1.0);   // = Sdf.coverage
    if (a <= 0.0) discard;
    fragColor = vec4(vColor.rgb, vColor.a * a);
}
```

Promień i rozmiar jako **atrybuty wierzchołków**, bo GUI jest batchowane (główny raport, 4.1).

## 5. Jak napisać backend

Implementujesz `UiBackend` (3 metody) i tłumaczysz każde `DrawCommand`. Nieznane polecenie = wyjątek.

**Backend 1.21.11** (Java 21, Mojang mappings, Loom) [SZKIC wg sekcji 4.3 B/C głównego raportu]:
- wejście: mixin za `GuiRenderer.render` w `GameRenderer.render` (wzorzec Meteora) albo `GuiElementRenderState`
  wysyłany przez `guiGraphics.guiRenderState.submitGuiElement` (wzorzec LiquidBounce),
- własne `RenderPipeline` prekompilowane hookiem po `ShaderLoader`,
- rysowanie: `RenderPass` na głównym framebufferze, `drawIndexed`,
- piksele fizyczne: skala `1 / window.getScaleFactor()`.

**Backend 26.x** (Java 25, bez obfuskacji i bez remapu [SKILL]): napisz od nowa, nie przez zamianę nazw.
Znane różnice: `GuiGraphics` → `GuiGraphicsExtractor`, `submitGuiElement` → `addGuiElement`,
`RenderPipeline` w innym pakiecie (`com.mojang.renderpearl.api.pipeline`, wg 26.3). Różnice wyciągaj z jara przez
`javap`, nie z pamięci.

**Strukturę Gradle** (dwa moduły, dwa Loomy, wspólny `gui-toolkit`) opisuje sekcja 4.5 głównego raportu.

## 6. Bramki: dopiero po nich wolno powiedzieć „działa"

Każda bramka osobno dla 1.21.11 i dla 26.x. Nie przeskakuj.

1. **Czysta kompilacja** (`clean compileJava`) backendu.
2. **Czerwony prostokąt** z `RoundedRect` na ekranie, ostry krawędziowo przy GUI scale 1, 2, 3, 4.
3. **Tekst** (atlas) i **clip** (przewijana lista nie wycieka poza ramkę).
4. **Blur tła** (`BackdropBlur`) bez migotania i bez zepsucia GUI vanilli.
5. **Trójkąt 3D w oknie GUI** (`Mesh3D`/`GUI_RECT`), głębia nie psuje reszty.
6. **Panel w świecie** (`WORLD`) z działającym klikiem przez `Ray.hitPanel`.
7. **Zrzuty ekranu** przez Fabric Client Game Tests (`takeScreenshot`/`assertScreenshotEquals`).
8. **Test z Sodium/Iris**: sprawdź, czy własne pipeline'y się nie wykrzaczają.
9. **Test `Custom`**: zarejestruj własny handler z prostym shaderem i potwierdź, że ma dostęp do tego, czego chcesz.

Jeśli któraś bramka nie przechodzi na danej wersji, to właśnie to jest granica możliwości tej wersji, i wtedy
potrzebna jest decyzja (inna ścieżka z głównego raportu), a nie obchodzenie błędu.

## 7. Czego jeszcze nie wiem

- Czy compute shadery są dostępne w abstrakcji Blaze3D [WIEDZA, niezweryfikowane].
- Czy hook za `GuiRenderer.render` z Meteora działa w 26.3 (klasy się przenoszą).
- Aktualna najnowsza wersja Minecrafta po 2026-10-05 (stabilnie 26.3 wg [SKILL]).
- Wydajność. Nic nie mierzyłem.
