# 03. Architektura klienta: wzorce z Meteora i LiquidBounce

Źródła: Meteor `master` `e3a2e77` (Java) i LiquidBounce `nextgen` `2a3762a` (Kotlin), oba 26.3. Gdzie mowa o 1.21.11, korzystam z gałęzi `4803071` (Meteor) i `0a3d2a25` (LiquidBounce). Znaczniki jak w [00-index.md](00-index.md). Kod klientów to **GPL-3.0**: opisuję wzorce własnymi słowami i nie kopiuję implementacji (patrz plik 09).

## 1. Dwa warianty tej samej idei

| | **Meteor Client** | **LiquidBounce nextgen** |
|---|---|---|
| Język | Java | Kotlin |
| Wejście | `MeteorClient implements ClientModInitializer` | obiekt `LiquidBounce` (Kotlin) [KOD] |
| Moduł | klasa dziedzicząca po `Module` | klasa/obiekt dziedziczący po `ClientModule` |
| Ustawienia | `Settings` → `SettingGroup` → `Setting<T>` budowane Builderem | drzewo `ValueGroup` / `Value<T>` jako delegaty właściwości |
| Zdarzenia | magistrala Orbit (`@EventHandler`) | własny `EventManager` + `EventListener`, z obsługą korutyn |
| Konfiguracja | serializacja do **NBT** (jeden plik na „system") | **Gson** (`config/gson`), `ConfigSystem` |
| GUI | własny silnik widgetów (Java) | UI w przeglądarce (Svelte) + elementy natywne w pipeline GUI |
| Rozszerzenia | `MeteorAddon` | marketplace, skrypty, addon CEF |

Pierwsza rzecz do przeniesienia do własnego projektu to **rozdział ról**: kto dostarcza zdarzenia, kto je konsumuje, kto trzyma stan, kto go zapisuje, kto go rysuje.

## 2. Cykl życia i kolejność inicjalizacji

Meteor `MeteorClient.onInitializeClient()` wykonuje po kolei [KOD]:

1. Zabezpieczenie przed podwójnym wywołaniem (`INSTANCE`).
2. `mc = Minecraft.getInstance()`.
3. W środowisku deweloperskim: wymuszenie ładowania mixinów (`MixinEnvironment...audit()`).
4. Utworzenie folderu danych klienta w katalogu gry, ewentualne zadania „przed wczytaniem" (np. włączenie jednego modułu przy pierwszym uruchomieniu).
5. `AddonManager.init()`: wykrycie addonów (z `fabric.mod.json` / entrypointów).
6. Rejestracja fabryk lambd magistrali zdarzeń **per pakiet addonu** (inaczej wysypie się po włączeniu modułu; wcześniejsza analiza, [KOD: komentarz w szablonie addonu]).
7. `ReflectInit.registerPackages()` i wywołanie metod oznaczonych `@PreInit`.
8. `Categories.init()`: kategorie modułów.
9. `Systems.init()`: utworzenie wszystkich „systemów" (moduły, konfig, HUD, makra, znajomi, profile, waypointy, konta, proxy).
10. `EVENT_BUS.subscribe(this)` dopiero po załadowaniu systemów.
11. `addon.onInitialize()` dla każdego addonu (rejestracja własnych modułów).
12. `Modules.get().sortModules()`: sortowanie po tym, jak addony dodały swoje.
13. `Systems.load()`: wczytanie plików z dysku.
14. Metody oznaczone `@PostInit`.
15. Hook zamknięcia JVM: `Systems.save()` i zapis motywów GUI.

Wnioski do skopiowania jako **idee** (nie kod):
- Kolejność **pre-init → systemy → addony → load → post-init** pozwala addonom dodać moduły *przed* wczytaniem zapisanych ustawień.
- Zapis w `Runtime.addShutdownHook` to ostatnia linia obrony; główny zapis powinien być też przy zmianie ustawień i przy opuszczeniu świata (Meteor zapisuje także na `GameLeftEvent`). [KOD]
- Inicjalizację oznaczaną adnotacjami (`@PreInit` / `@PostInit` + skan pakietów) da się zastąpić jawną listą kroków; skan refleksją jest wygodny, ale utrudnia debugowanie. [WIEDZA]

## 3. Moduł (jednostka funkcjonalności)

### 3.1 Co ma moduł w Meteorze [KOD]
- `category`, `name` (kebab-case), `title` (wyliczany z nazwy), `description`, `aliases`.
- `color` (losowy odcień HSV), `addon` (wykrywany po pakiecie klasy).
- `settings` (własny obiekt `Settings`).
- Stan `active`, flaga `serialize`.
- Klawisz: `keybind`, flaga `toggleOnBindRelease`.
- Flagi: `runInMainMenu`, `autoSubscribe`, `chatFeedback`, `favorite`.
- Hooki: `onActivate()`, `onDeactivate()`, opcjonalnie `getWidget(GuiTheme)` do własnego fragmentu GUI.
- Metody: `toggle()`, `enable()`, `disable()`, `info/warning/error` z prefiksem czatu.

### 3.2 `toggle()` w opisie
Włączenie: `active = true`, dodaj do listy aktywnych w `Modules`, wywołaj `settings.onActivated()`, a jeśli `runInMainMenu || Utils.canUpdate()`, to: jeśli `autoSubscribe`, zapisz moduł do magistrali zdarzeń i wywołaj `onActivate()`. Wyłączenie jest odbiciem lustrzanym. [KOD]

Co z tego warto zapamiętać:
- **Subskrypcja zdarzeń jest powiązana ze stanem modułu.** Wyłączony moduł nie dostaje zdarzeń (zero kosztu).
- Hooki aktywacji nie wykonują się, gdy nie ma świata (chyba że moduł jawnie działa w menu głównym).
- Informacje zwrotne (czat) są konfigurowalne globalnie i per moduł.

### 3.3 Co ma moduł w LiquidBounce [KOD]
Konstruktor `ClientModule` przyjmuje: nazwę, kategorię, domyślny klawisz i akcję klawisza (`BindAction.TOGGLE`), domyślny stan, flagi `notActivatable`/`disableActivation`/`disableOnQuit`, aliasy i `hide`. Klasa jest jednocześnie `ToggleableValueGroup` (czyli ma ustawienia), `EventListener` i ma skróty do Minecrafta (`MinecraftShortcuts`). Moduły to zwykle Kotlinowe `object` (singletony), co upraszcza dostęp z innych modułów.

### 3.4 Wzorzec do własnego projektu [SZKIC]
```java
public abstract class Module {
    public final String name;          // kebab-case, bez spacji (komendy)
    public final Category category;
    public final Settings settings = new Settings();
    private boolean active;

    protected void onActivate() {}
    protected void onDeactivate() {}

    public final void setActive(boolean on) {
        if (on == active) return;
        active = on;
        if (on) { EventBus.subscribe(this); onActivate(); }
        else    { onDeactivate(); EventBus.unsubscribe(this); }
    }
}
```
Pułapki: wywołuj `unsubscribe` także, gdy `onDeactivate` rzuci wyjątek (try/finally); nie pozwalaj na włączenie modułu poza kontekstem, w którym jego zdarzenia mają sens.

## 4. Ustawienia

### 4.1 Meteor [KOD]
`Setting<T>` ma: `name`, `title`, `description`, `defaultValue`, bieżącą `value`, `onChanged`, `onModuleActivated`, `visible` (warunek widoczności) oraz wskazanie `module`. Metody: `get`, `set` (z walidacją `isValueValid`), `reset`, `parse(String)` (dla komend), `wasChanged`, `getSuggestions` (podpowiedzi w komendach), serializacja `toTag`/`fromTag` (`CompoundTag`).

Katalog gotowych typów (`settings/`): Bool, Int, Long, Double, String, StringList, Enum, Color, ColorList, Keybind, Vector3d, File, FontFace, Generic, oraz wiele typów „gry": Block, BlockList, BlockPos, Item, ItemList, EntityTypeList, Enchantment, Potion, Particle, SoundEvent, Packet, ScreenHandler, StatusEffect itd.

Zasada: ustawienia są pogrupowane (`SettingGroup`), tworzone Builderem z łańcuchem `.name(...).description(...).defaultValue(...).visible(() -> ...).onChanged(...).build()`.

### 4.2 LiquidBounce [KOD]
Ustawienia są delegatami Kotlina (`val tryMode by enumChoice("Mode", Mode.X)`); wszystko dziedziczy z `ValueGroup`, a drzewo wartości ma **klucze** (`"liquidbounce.module.sprint.mode"`). `ConfigSystem` umie wyszukać wartość po kluczu (`findValueByKey`) i grupę po kluczu, a także podpowiadać klucze. To daje API komend i REST (GUI w przeglądarce) „za darmo".

### 4.3 Co wybrać [SZKIC]
Dla własnego klienta ze stylami i motywami wybierz **ustawienia jako drzewo z kluczami** (jak LiquidBounce), bo ten sam mechanizm obsłuży:
- komendy (`.set sprint.mode legit`),
- serializację do JSON,
- GUI generowane automatycznie z typów ustawień,
- motywy (kolory/promienie jako ustawienia z kluczami `theme.panel.radius`).

Minimalny interfejs:
```java
public interface Value<T> {
    String key();                 // "module.sprint.mode"
    T get();
    boolean set(T v);             // z walidacją
    T defaultValue();
    boolean visible();            // warunek zależny od innych wartości
    void toJson(JsonObject o);
    void fromJson(JsonObject o);
}
```

## 5. Zdarzenia

### 5.1 Meteor: magistrala Orbit [KOD]
- Moduły i inne obiekty rejestrują się `EVENT_BUS.subscribe(this)`, a metody oznaczone `@EventHandler` odbierają zdarzenia.
- Rejestracja wymaga **fabryki lambd per pakiet** (`registerLambdaFactory`), co jest powodem, dla którego `getPackage()` w addonie musi być poprawne.
- Zdarzenia to klasy w pakietach `events/*` (69 plików Java w `master`). [KOD]
- Przykłady: `TickEvent.Pre/Post`, `Render2DEvent`, `Render3DEvent`, `KeyInputEvent`, `MouseClickEvent`, `PacketEvent.Send/Receive`, `ResolutionChangedEvent`, `GameLeftEvent`, `RenderAfterWorldEvent`.
- Zdarzenia z pulą obiektów: `Render3DEvent.get(...)` zwraca instancję z puli (mniej alokacji). [KOD]

### 5.2 LiquidBounce: własny EventManager [KOD]
Lista wszystkich klas zdarzeń jest deklarowana w jednej tablicy `ALL_EVENT_CLASSES` (np. `GameTickEvent`, `GameRenderEvent`, `WorldRenderEvent`, `OverlayRenderEvent`, `ScreenRenderEvent`, `WindowResizeEvent`, `FramebufferResizeEvent`, `MouseButtonEvent`, `KeyboardKeyEvent`, `KeyboardCharEvent`, `ChatSendEvent`, `ChatReceiveEvent`, `PlayerTickEvent`…). Dzięki jednej tablicy rejestr i generowanie struktur jest przewidywalne. Są też `CoroutineTicker`, `TickLoopTaskExecutor` i obsługa handlerów `suspend`.

### 5.3 Co wynika dla GUI
Zdarzenia, które muszą istnieć w każdym kliencie z własnym GUI:

| Zdarzenie | Po co |
|---|---|
| tik gry (przed/po) | logika modułów, animacje tickowe |
| `Render2D` (faza HUD) | rysowanie HUD |
| `ScreenRender` / render ekranu | rysowanie własnych ekranów |
| zmiana rozdzielczości okna / framebuffera | odtworzenie tekstur offscreen (blur, cache) |
| klawisz, znak, mysz (klik, ruch, scroll) | wejście do drzewa widgetów |
| otwarcie/zamknięcie ekranu | stos ekranów, blur tła |
| opuszczenie świata | zapis konfiguracji, reset stanu |

### 5.4 Wzorzec do własnego projektu [SZKIC]
Jeśli nie chcesz zależeć od zewnętrznej magistrali: prosta, typowana magistrala z `Map<Class<?>, List<Listener>>` i jawnymi `register/unregister` wystarczy. Kluczowa decyzja: **czy zdarzenia są anulowalne** i **w jakiej kolejności są wołane słuchacze** (priorytety). Obie rzeczy zdefiniuj od początku, bo trudno je dołożyć później. [WIEDZA]

## 6. Konfiguracja i trwałość

### 6.1 Meteor: „systemy" [KOD]
- Każdy `System<T>` ma nazwę i plik `<nazwa>.nbt` w folderze klienta, metody `init`, `save`, `load`, `toTag`, `fromTag`.
- Zapis jest **atomowy**: dane trafiają do pliku tymczasowego (`Files.createTempFile`), potem `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`; jeśli system plików nie wspiera ruchu atomowego, kopia zwykła. Plik tymczasowy jest sprzątany w `finally`.
- Błąd zapisu jest logowany („Possibly corrupted?"), ale nie przerywa gry.
- Systemy: konfiguracja główna (`Config`), moduły (`Modules`), HUD, makra, znajomi, profile, waypointy, konta, proxy.
- Przy opuszczeniu gry (`GameLeftEvent`) systemy są zapisywane. [KOD]

### 6.2 LiquidBounce [KOD]
`ConfigSystem` (obiekt) tworzy `rootFolder` w katalogu gry, podkatalogi `configs` (konfiguracje użytkownika) i `backups`, przechowuje listę `Config` (drzew wartości) i wykrywa pierwsze uruchomienie (`isFirstLaunch`). Serializacja Gson z własnymi adapterami (`config/gson`), a poprawka z 2026-11-06 w repo dotyczy „race condition replacing save", czyli problemów z równoległym zapisem. [KOD: tytuł ostatniego commita]

### 6.3 Zalecenia [SZKIC / WIEDZA]
1. Zapis **atomowy** (plik tymczasowy → rename) zawsze.
2. Kopie zapasowe przed migracją formatu; **wersja schematu** w pliku.
3. Jeden zapis w czasie (blokada) lub kolejka zapisów (po błędzie LB z równoległym zapisem).
4. Format: JSON dla czytelności i narzędzi; NBT, jeśli chcesz dzielić kod ze światem gry.
5. Konfiguracja motywu GUI **osobno** od konfiguracji modułów (Meteor zapisuje motywy osobno: `GuiThemes.save()`).

## 7. Komendy

Meteor [KOD]: `Command` ma nazwę, tytuł, opis i aliasy; dziedziczące klasy budują drzewo Brigadiera (`LiteralArgumentBuilder`, `RequiredArgumentBuilder`) na `ClientSuggestionProvider`. Komendy mieszkają w `commands/commands/*` (np. bind, binds, friends, fov, gamemode, drop...). Prefiks komend jest konfigurowalny (`Config.get().prefix`), a otwarcie czatu z prefiksem ma osobny klawisz (`KeyBinds.OPEN_COMMANDS`).

Wzorzec: komendy **nie** są rejestrowane przez serwer, tylko przechwytywane po stronie klienta w czacie. Fabric API ma własny system komend klienta (`ClientCommands` w 26.x, `ClientCommandManager` w 1.21.11). Który wybrać:
- Fabric API: stabilne, wspiera podpowiedzi, mniej kodu własnego.
- Własny prefiks (`.` lub `,`): umożliwia komendy bez ukośnika i bez interakcji z serwerem; wymaga przechwycenia wysyłki czatu (event `ChatSend`). [WIEDZA]

## 8. HUD

### 8.1 Dwa poziomy
1. **HUD vanilli przez Fabric API**: `HudElementRegistry` (dodawanie, kolejność względem elementów vanilli, zastępowanie). Element ma sygnaturę „grafika + `DeltaTracker`". `HudRenderCallback` jest przestarzały od API 0.116. [DOK]
2. **Własny system HUD** (Meteor): `HudElement` z `HudBox`, kotwicami `XAnchor`/`YAnchor`, `autoAnchors`, przyciąganiem do siatki (`Snapper`), własnymi `Settings`, włączaniem/wyłączaniem i edytorem (`systems/hud/screens`). [KOD]

### 8.2 Co wybrać [SZKIC]
Jeśli HUD ma być edytowalny przez użytkownika (przeciąganie, rozmiary, kotwice), zbuduj własny system, ale **wpiąć go** przez `HudElementRegistry` jako jeden duży element, który rysuje listę własnych elementów. Dzięki temu poprawnie współpracujesz z F1 (ukrywanie HUD) i kolejnością warstw gry.

### 8.3 Dane elementu HUD (minimum)
- pozycja względna do kotwicy, kotwica (poziomo/pionowo),
- rozmiar (często wyliczany z zawartości),
- ustawienia własne elementu,
- stan aktywności,
- serializacja.

## 9. Addony

Meteor [KOD]: `MeteorAddon` (klasa abstrakcyjna) wykrywana przez `AddonManager`; ma `onInitialize()`, `getPackage()` (używane do fabryki lambd i przypisania modułu do addonu), dodaje moduły, komendy, elementy HUD i kategorie. Szablon addonu to osobne repozytorium (`meteor-addon-template`).

LiquidBounce [KOD]: adnotacja `@AddonApi` oznacza stabilne API dla addonów; są motywy ładowane z URL-a, skrypty i marketplace.

Wzorzec: osobny **entrypoint** w `fabric.mod.json` (np. `"yourclient-addon"`), który Fabric Loader potrafi wyliczyć (`FabricLoader.getEntrypointContainers`). [WIEDZA co do nazwy API; mechanizm entrypointów jest w `fabric-mod-json.md`]

## 10. Mixiny jako źródło zdarzeń

Zasada obu klientów [KOD]: mixin jest **cienki**. Wstrzykuje się w miejsce w grze, buduje obiekt zdarzenia i wysyła na magistralę; logika siedzi w modułach.

Liczby (Meteor `master`, `src/main/java`): `mixin/` 213 plików, `mixininterface/` 20, `systems/` 255, `gui/` 135, `utils/` 127, `events/` 69, `commands/` 58, `settings/` 38, `renderer/` 22. [KOD] Mixinów jest więc dużo, co tłumaczy, czemu każda aktualizacja gry zmusza do przeglądu.

Dobre praktyki widoczne w kodzie:
- Warunkowe ładowanie mixinów dla integracji (Sodium, Iris, Lithium…) przez `MixinPlugin`.
- `@Local` (MixinExtras) do pobrania zmiennych lokalnych zamiast kopiowania logiki gry.
- Wydzielone interfejsy `mixininterface/*` do wystawiania pól i metod dodanych mixinem.
- Hooki renderowania są **najbardziej kruche** (zmieniają się w każdej wersji): trzymaj je w jednym pakiecie.

## 11. Sugestia struktury pakietów dla własnego klienta [SZKIC]

```
core/        Client (entrypoint), Lifecycle, Paths, Log
event/       Event, EventBus, definicje zdarzeń
module/      Module, ModuleManager, Category
value/       Value<T> + typy (Bool, Number, Enum, Color, Key, Text, List)
config/      ConfigStore (atomowy zapis), migracje schematu
command/     CommandRegistry, komendy
hud/         HudElement, HudLayout, edytor
ui/
  gfx/       UiGraphics (interfejs), backend/ (osobno dla 1.21.11 i 26.x)
  style/     Theme, tokeny, StyleSheet
  widget/    Widget, Container, Layout, kontrolki
  screen/    ClickGuiScreen, HudEditorScreen
mixin/       tylko źródła zdarzeń i akcesory
addon/       API dla rozszerzeń
```

Zależności mają iść „w dół": `ui` zna `value` i `event`, ale `module` nie zna `ui` (moduł może wystawiać „widget" tylko przez abstrakcję). Dzięki temu można zmienić cały silnik GUI bez ruszania modułów.
