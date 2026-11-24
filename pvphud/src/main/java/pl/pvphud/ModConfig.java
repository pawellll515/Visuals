package pl.pvphud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cała konfiguracja moda. Zapisywana do config/pvphud.json. Kolory są w formacie ARGB. */
public class ModConfig {

    public static class Elem {
        public boolean enabled = true;
        public float x, y;          // pozycja jako ułamek ekranu (0..1), lewy górny róg
        public float scale = 1f;
        public boolean vertical;
        public boolean bg;          // tło za elementem
        public int max = 8;         // max liczba pozycji (efekty)

        public Elem() {}
        public Elem(float x, float y, boolean vertical) {
            this.x = x;
            this.y = y;
            this.vertical = vertical;
        }
    }

    public static ModConfig I = new ModConfig();

    public boolean hudEnabled = true;

    // ---- Target HUD ----
    public boolean targetEnabled = true;
    public int targetMode = 2;              // 0 = po uderzeniu, 1 = gdy patrzę, 2 = uderzenie + patrzenie
    public float targetTime = 4f;           // sekundy
    public float targetX = 0.5f, targetY = 0.62f; // środek panelu
    public float targetScale = 1f;
    public boolean targetVertical = false;
    public boolean targetShowHealth = true;
    public boolean targetHearts = true;     // true = serca (1 serce = 2 HP), false = HP
    public int targetHealthStyle = 2;       // 0 pasek, 1 liczba, 2 oba
    public boolean targetShowHands = true;
    public boolean targetShowArmor = true;
    public boolean targetShowEffects = true;
    public int targetDurStyle = 2;          // 0 pasek vanilla, 1 liczba, 2 oba
    public int targetMaxEffects = 6;
    public int targetBg = 0xB0101018;
    public int targetBorder = 0xFF5865F2;
    public int targetText = 0xFFFFFFFF;
    public int targetRadius = 6;

    // ---- Status HUD (twój własny) ----
    public Elem effectsHud = new Elem(0.012f, 0.25f, true);
    public Elem armorHud = new Elem(0.012f, 0.62f, true);
    public Elem itemHud = new Elem(0.74f, 0.88f, false);
    public int statusDurStyle = 2;
    public boolean itemCountInsteadOfDura = true;

    // ---- Marker na celu ----
    public boolean markerEnabled = true;
    public int markerColor = 0xFFFF4D4D;
    public float markerSize = 8f;           // połowa boku, w setnych bloku
    public float markerSpeed = 1f;

    // ---- Hitboxy ----
    public boolean hitboxEnabled = false;
    public int hitboxColor = 0xFFFFFFFF;
    public float hitboxThickness = 2f;
    public float hitboxRange = 48f;
    public boolean hitboxLookEnabled = true;
    public int hitboxLookColor = 0xFFFF4D4D;

    // ---- Tekstura ekwipunku ----
    public boolean invEnabled = false;
    public float invScale = 1f;
    public float invAlpha = 1f;
    public float invOffX = 0f, invOffY = 0f;

    // ---- Wygląd menu ----
    public int menuColor = 0xFF14141C;
    public float menuOpacity = 0.6f;
    public int menuRadius = 8;
    public int accent = 0xFF5865F2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("pvphud.json");
    }

    public static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("pvphud");
        try {
            Files.createDirectories(d);
        } catch (IOException ignored) {
        }
        return d;
    }

    public static void load() {
        try {
            if (Files.exists(path())) {
                ModConfig c = GSON.fromJson(Files.readString(path()), ModConfig.class);
                if (c != null) I = c;
            }
        } catch (Exception e) {
            System.err.println("[pvphud] nie udało się wczytać configu: " + e);
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(I));
        } catch (IOException e) {
            System.err.println("[pvphud] nie udało się zapisać configu: " + e);
        }
    }
}
