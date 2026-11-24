# PvP HUD (Fabric 1.21.4)

Mod wyłącznie wizualny, client-side. Nie zmienia mechaniki gry.

## Jak zbudować plik .jar
1. Zainstaluj JDK 21.
2. W folderze projektu uruchom: `gradle wrapper` (raz), potem `./gradlew build` (Windows: `gradlew.bat build`).
   Można też otworzyć folder w IntelliJ IDEA i uruchomić zadanie `build`.
3. Gotowy mod: `build/libs/pvphud-1.0.0.jar` (NIE ten z `-sources`). Wrzuć do `.minecraft/mods` razem z Fabric API.

## Obsługa
- **Prawy Shift** – menu (zmienisz w Sterowaniu: "Otwórz menu PvP HUD").
- Menu jest przezroczyste, a przy otwartym menu panel celu pokazuje podgląd na Tobie.
- Config: `config/pvphud.json`.

## Funkcje
- Panel celu po uderzeniu gracza (albo gdy na niego patrzysz): nick, zdrowie (pasek/liczba), itemy w rękach, zbroja,
  efekty z poziomem i czasem (1m21s -> `1m`). Wytrzymałość: pasek vanilla / liczba / oba. Układ poziomy lub pionowy.
- Effect status, Armor status, Item status (własne). Totem i inne itemy bez wytrzymałości pokazują liczbę
  takich itemów w ekwipunku zamiast durability.
- Obracający się marker w środku hitboxu gracza, na którego patrzysz.
- Własne hitboxy graczy: kolor, grubość, zasięg, inny kolor gdy na gracza patrzysz (nie widać ich przez ściany).
- Tekstura ekwipunku: wrzuć `inventory.png/.jpg/.bmp/.gif` do `config/pvphud/`, w menu zmieniasz rozmiar,
  przezroczystość i przesunięcie na żywo.
- Color picker jak w Paincie + pole HEX (`#RRGGBB` lub `RRGGBBAA`) i suwak przezroczystości.
- Zaokrąglone rogi wszędzie, przezroczystość i kolory menu do ustawienia.

## Uwaga o wersjach
Kod jest napisany pod **1.21.4** (Yarn 1.21.4+build.8, Fabric API 0.119.4). Wersje 1.21.5–1.21.11 zmieniały
m.in. renderowanie GUI, klucze sterowania (KeyBinding.Category) i silnik renderu świata, więc potrzebują osobnych
buildów (najwygodniej przez Stonecutter).

## Miejsca, które najprawdopodobniej trzeba będzie poprawić przy pierwszej kompilacji
Kod nie był kompilowany (brak dostępu do sieci w środowisku, w którym powstał). Najbardziej "wrażliwe" API:
- `Cells.drawEffect` – `drawSpriteStretched` / `getStatusEffectSpriteManager()`
- `WorldVisuals` – `ctx.tickCounter().getTickDelta(false)`, `RenderLayer.getGuiOverlay()`, `getDebugQuads()`
- `ImageManager` – `drawTexture(..., color)`
- `InventoryScreenMixin` – nazwa metody `drawBackground`
