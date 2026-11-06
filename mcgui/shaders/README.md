# Shadery SDF (opcjonalny backend wysokiej jakości)

`SimpleBackend` (rdzeń) rysuje wszystko przez zwykłe `fill` i nie wymaga shaderów, ale ma ograniczenia
(brak rozmycia, tylko pionowe gradienty, cień jako warstwy). Pliki w tym katalogu to **szkice** pipeline'u SDF,
który daje pełną jakość: dowolne gradienty, cztery promienie rogów, obramowanie i cień w jednym przebiegu.

**Status: NIEPODŁĄCZONE I NIEPRZETESTOWANE.** Nie ma ich w żadnym adapterze. Aby je użyć, trzeba:
1. zarejestrować pipeline (1.21.11/26.x: `RenderPipelines.register(...)` z `GUI_SNIPPET`),
2. zaimplementować `GuiElementRenderState` wysyłający atrybuty z `BoxStyle` (wzorzec: `research/mc-client/06`),
3. napisać `UiGraphics` korzystający z tego elementu dla `box(...)`, a `SimpleBackend` zostawić jako awaryjny.

Szczegóły i pułapki: [`research/mc-client/04-api-renderowania.md`](../research/mc-client/04-api-renderowania.md)
oraz [`research/mc-client/06-silnik-stylow-i-efekty.md`](../research/mc-client/06-silnik-stylow-i-efekty.md).
Format shaderów (`layout(location)`, `#include <minecraft:...>`) dotyczy linii 26.x; na 1.21.11 trzeba
deklarować bloki uniformów ręcznie (tabela w `research/mc-client/02`).
