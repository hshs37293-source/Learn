# Learn

Notatki z nauki i research.

## Research

- [Klient (mod Fabric) do Minecraft 1.21.11+ z nowoczesnym GUI](research/minecraft-client-gui-1.21.11.md): toolchain, architektura klienta, silnik renderowania, zaokrąglone rogi (SDF), projekt menu, testy, ryzyka.
  - Sekcja 4.5: dowolne GUI **2D i 3D** na **1.21.11** i na najnowszej wersji (26.x) w **Mojang mappings**, układ jednego projektu na dwie wersje.
- [Dowolne GUI 2D/3D: plan, mapa możliwości i bramki akceptacji](research/dowolne-gui-2d-3d.md)

## Kod

- [`gui-toolkit/`](gui-toolkit): silnik GUI w czystej Javie 21 (polecenia rysowania, widgety, SDF, macierze, picking 3D). Testy: `cd gui-toolkit && gradle check-ui` (9/9 przechodzi).
