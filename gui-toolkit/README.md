# gui-toolkit

Silnik GUI niezależny od Minecrafta (czysta Java 21, zero zależności). Opisuje GUI jako **listę poleceń
rysowania** (`DrawList`), a backend dla konkretnej wersji gry (1.21.11 albo 26.x) tylko je wykonuje.
Plan i uzasadnienie: [`research/dowolne-gui-2d-3d.md`](../research/dowolne-gui-2d-3d.md).

## Uruchomienie testów

```
cd gui-toolkit && gradle check-ui
```

Zweryfikowane w tej sesji: kompilacja Javą 21 i **9 z 9 testów przechodzi** (SDF, macierze, picking 3D,
layout, kliknięcia, slider, clip, `Custom`, sprężyna).

## Układ

| Pakiet | Zawartość |
|---|---|
| `math` | `Mat4` (perspektywa, ortho, lookAt, odwrotność), `Vec3`, `Ray` (picking, trafianie w panel w świecie), `Sdf` (wzorzec CPU dla shadera) |
| `draw` | `DrawCommand` (RoundedRect, Shadow, Text, Image, Clip, BackdropBlur, Mesh3D, **Custom**), `DrawList`, `UiBackend` |
| `widget` | `Widget`, `Stack`, `Label`, `Button`, `Slider`, `Screen` (wejście + layout + rysowanie) |
| `theme`, `anim` | `Theme` (tokeny), `Easing`, `Spring` |

## Czego tu NIE ma

Backendów do Minecrafta. Nie da się ich skompilować w środowisku bez dostępu do `maven.fabricmc.net`
i serwerów Mojanga. Kontrakt dla nich to `UiBackend`, a instrukcja ich napisania jest w dokumencie
badawczym.
