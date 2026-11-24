package pl.pvphud.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import pl.pvphud.ModConfig;
import pl.pvphud.render.ImageManager;
import pl.pvphud.util.Gfx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Przezroczyste, zaokrąglone menu na środku ekranu z zakładkami. Otwiera się prawym Shiftem. */
public class ConfigScreen extends Screen {

    private static final int ROW_H = 22;
    private static final int CTRL_W = 112;

    private final Screen parent;
    private final List<Tab> tabs = new ArrayList<>();
    private int tab;
    private double scroll;
    private Row active;
    private int px, py, pw, ph, cx, cy, cw, ch;

    record Tab(String name, List<Row> rows) {}

    public ConfigScreen(Screen parent) {
        super(Text.literal("PvP HUD"));
        this.parent = parent;
        buildTabs();
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 430);
        ph = Math.min(height - 16, 270);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx = px + 108;
        cy = py + 28;
        cw = pw - 118;
        ch = ph - 38;
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float d) {
        // brak rozmycia i przyciemnienia - menu jest przezroczyste, widzisz grę pod spodem
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        ModConfig k = ModConfig.I;
        int panel = (k.menuColor & 0xFFFFFF) | ((int) (MathHelper.clamp(k.menuOpacity, 0.1f, 1f) * 255) << 24);
        Gfx.rounded(c, px, py, pw, ph, k.menuRadius, panel);
        Gfx.ring(c, px, py, pw, ph, k.menuRadius, k.accent);
        c.drawTextWithShadow(textRenderer, "PvP HUD", px + 14, py + 10, 0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer, "[Prawy Shift] zamknij", px + pw - 12 - textRenderer.getWidth("[Prawy Shift] zamknij"),
                py + 10, 0xFF9AA0B0);

        // zakładki
        for (int i = 0; i < tabs.size(); i++) {
            int ty = cy + i * 21, tx = px + 10, tw = 88;
            boolean sel = i == tab;
            boolean hov = mx >= tx && mx < tx + tw && my >= ty && my < ty + 18;
            int bg = sel ? Gfx.withAlpha(k.accent, 0.85f) : hov ? 0x40FFFFFF : 0x22FFFFFF;
            Gfx.rounded(c, tx, ty, tw, 18, 5, bg);
            c.drawTextWithShadow(textRenderer, tabs.get(i).name(), tx + 7, ty + 5, 0xFFFFFFFF);
        }

        // zawartość
        List<Row> rows = tabs.get(tab).rows();
        int total = 0;
        for (Row r : rows) total += r.h();
        scroll = MathHelper.clamp(scroll, 0, Math.max(0, total - ch));

        c.enableScissor(cx, cy, cx + cw, cy + ch);
        int y = cy - (int) scroll;
        int rw = cw - 8;
        for (Row r : rows) {
            r.draw(c, textRenderer, cx, y, rw, mx, my);
            y += r.h();
        }
        c.disableScissor();

        if (total > ch) { // pasek przewijania
            int bh = Math.max(16, ch * ch / total);
            int by = cy + (int) ((ch - bh) * (scroll / (total - ch)));
            Gfx.rounded(c, cx + cw - 4, by, 3, bh, 1, 0x80FFFFFF);
        }
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < tabs.size(); i++) {
            int ty = cy + i * 21, tx = px + 10;
            if (mx >= tx && mx < tx + 88 && my >= ty && my < ty + 18) {
                tab = i;
                scroll = 0;
                return true;
            }
        }
        if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ch) {
            int y = cy - (int) scroll;
            for (Row r : tabs.get(tab).rows()) {
                if (my >= y && my < y + r.h()) {
                    if (r.click(this, cx, y, cw - 8, mx, my, button)) {
                        active = r;
                        return true;
                    }
                }
                y += r.h();
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (active != null) {
            active.drag(cx, cw - 8, mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        active = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= v * 20;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 344 /* GLFW_KEY_RIGHT_SHIFT */) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        ModConfig.save();
        client.setScreen(parent);
    }

    // ------------------------------------------------------------------ rows

    interface Row {
        default int h() {
            return ROW_H;
        }

        void draw(DrawContext c, TextRenderer tr, int x, int y, int w, int mx, int my);

        default boolean click(ConfigScreen s, int x, int y, int w, double mx, double my, int button) {
            return false;
        }

        default void drag(int x, int w, double mx) {
        }
    }

    private static void label(DrawContext c, TextRenderer tr, String t, int x, int y) {
        c.drawTextWithShadow(tr, t, x + 2, y + 7, 0xFFE6E8EE);
    }

    private static boolean onCtrl(double mx, int x, int w) {
        return mx >= x + w - CTRL_W;
    }

    private record Header(String text) implements Row {
        public int h() {
            return 20;
        }

        public void draw(DrawContext c, TextRenderer tr, int x, int y, int w, int mx, int my) {
            c.drawTextWithShadow(tr, text, x + 2, y + 9, ModConfig.I.accent);
            c.fill(x + 2, y + 19, x + w, y + 20, 0x33FFFFFF);
        }
    }

    private record Info(String text) implements Row {
        public void draw(DrawContext c, TextRenderer tr, int x, int y, int w, int mx, int my) {
            c.drawTextWithShadow(tr, text, x + 2, y + 7, 0xFF9AA0B0);
        }
    }

    private record Toggle(String name, BooleanSupplier get, Consumer<Boolean> set) implements Row {
        public void draw(DrawContext c, TextRenderer tr, int x, int y, int w, int mx, int my) {
            label(c, tr, name, x, y);
            boolean on = get.getAsBoolean();
            int bx = x + w - 30, by = y + 5;
            Gfx.rounded(c, bx, by, 28, 12, 6, on ? Gfx.withAlpha(ModConfig.I.accent, 1f) : 0xFF3A3D48);
            int kx = on ? bx + 16 : bx + 2;
            Gfx.rounded(c, kx, by + 2, 10, 8, 4, 0xFFFFFFFF);
        }

        public boolean click(ConfigScreen s, int x, int y, int w, double mx, double my, int b) {
            set.accept(!get.getAsBoolean());
            return false; // nie przejmuje przeciągania
        }
    }

    private record Slider(String name, double min, double max, double step, DoubleSupplier get, DoubleConsumer set,
                          String fmt) implements Row {
        public void draw(DrawContext c, TextRenderer tr, int x, int y, int w, int mx, int my) {
            label(c, tr, name, x, y);
            int sx = x + w - CTRL_W, sw = CTRL_W;
            Gfx.rounded(c, sx, y + 4, sw, 14, 5, 0xFF2B2E38);
            double t = (get.getAsDouble() - min) / (max - min);
            int fw = (int) (sw * MathHelper.clamp(t, 0, 1));
            if (fw > 1) Gfx.rounded(c, sx, y + 4, Math.max(fw, 6), 14, 5, ModConfig.I.accent);
            String v = String.format(fmt, get.getAsDouble());
            c.drawCenteredTextWithShadow(tr, v, sx + sw / 2, y + 8, 0xFFFFFFFF);
        }

        public boolean click(ConfigScreen s, int x, int y, int w, double mx, double my, int b) {
            if (!onCtrl(mx, x, w)) return false;
            drag(x, w, mx);
            return true;
        }

        public void drag(int x, int w, double mx) {
            double t = MathHelper.clamp((mx - (x + w - CTRL_W)) / CTRL_W, 0, 1);
            double v = min + t * (max - min);
            v = Math.round(v / step) * step;
            set.accept(MathHelper.clamp(v, min, max));
        }
    }

    private record Cycle(String name, String[] options, IntSupplier get, IntConsumer set) implements Row {
        public void draw(DrawContext c, TextRenderer tr, int x, int y, int w, int mx, int my) {
            label(c, tr, name, x, y);
            int sx = x + w - CTRL_W;
            Gfx.rounded(c, sx, y + 3, CTRL_W, 16, 5, 0xFF2B2E38);
            Gfx.ring(c, sx, y + 3, CTRL_W, 16, 5, 0x55FFFFFF);
            c.drawCenteredTextWithShadow(tr, options[get.getAsInt() % options.length], sx + CTRL_W / 2, y + 7, 0xFFFFFFFF);
        }

        public boolean click(ConfigScreen s, int x, int y, int w, double mx, double my, int b) {
            if (!onCtrl(mx, x, w)) return false;
            int d = b == 1 ? options.length - 1 : 1;
            set.accept((get.getAsInt() + d) % options.length);
            return false;
        }
    }

    private record ColorRow(String name, IntSupplier get, IntConsumer set) implements Row {
        public void draw(DrawContext c, TextRenderer tr, int x, int y, int w, int mx, int my) {
            label(c, tr, name, x, y);
            int sx = x + w - CTRL_W;
            Gfx.rounded(c, sx, y + 3, CTRL_W, 16, 5, 0xFF2B2E38);
            c.fill(sx + 4, y + 6, sx + 28, y + 16, 0xFF555555);
            c.fill(sx + 4, y + 6, sx + 28, y + 16, get.getAsInt());
            c.drawTextWithShadow(tr, String.format("#%06X", get.getAsInt() & 0xFFFFFF), sx + 34, y + 7, 0xFFFFFFFF);
        }

        public boolean click(ConfigScreen s, int x, int y, int w, double mx, double my, int b) {
            if (!onCtrl(mx, x, w)) return false;
            s.client.setScreen(new ColorPickerScreen(s, name, get, set));
            return false;
        }
    }

    private record Action(String name, String button, Runnable run) implements Row {
        public void draw(DrawContext c, TextRenderer tr, int x, int y, int w, int mx, int my) {
            label(c, tr, name, x, y);
            int sx = x + w - CTRL_W;
            boolean hov = mx >= sx && mx < sx + CTRL_W && my >= y + 3 && my < y + 19;
            Gfx.rounded(c, sx, y + 3, CTRL_W, 16, 5, hov ? Gfx.withAlpha(ModConfig.I.accent, 1f) : 0xFF3A3D48);
            c.drawCenteredTextWithShadow(tr, button, sx + CTRL_W / 2, y + 7, 0xFFFFFFFF);
        }

        public boolean click(ConfigScreen s, int x, int y, int w, double mx, double my, int b) {
            if (onCtrl(mx, x, w)) run.run();
            return false;
        }
    }

    // ------------------------------------------------------------ definicje

    private static Row toggle(String n, BooleanSupplier g, Consumer<Boolean> s) {
        return new Toggle(n, g, s);
    }

    private static Row slider(String n, double min, double max, double step, String fmt, DoubleSupplier g, DoubleConsumer s) {
        return new Slider(n, min, max, step, g, s, fmt);
    }

    private static Row color(String n, IntSupplier g, IntConsumer s) {
        return new ColorRow(n, g, s);
    }

    private void elemRows(List<Row> r, String title, ModConfig.Elem e, boolean hasMax) {
        r.add(new Header(title));
        r.add(toggle("Włączone", () -> e.enabled, v -> e.enabled = v));
        r.add(toggle("Układ pionowy", () -> e.vertical, v -> e.vertical = v));
        r.add(toggle("Tło i ramka", () -> e.bg, v -> e.bg = v));
        r.add(slider("Pozycja X", 0, 1, 0.005, "%.3f", () -> e.x, v -> e.x = (float) v));
        r.add(slider("Pozycja Y", 0, 1, 0.005, "%.3f", () -> e.y, v -> e.y = (float) v));
        r.add(slider("Skala", 0.5, 3, 0.05, "%.2fx", () -> e.scale, v -> e.scale = (float) v));
        if (hasMax) r.add(slider("Maks. efektów", 1, 16, 1, "%.0f", () -> e.max, v -> e.max = (int) v));
    }

    private void buildTabs() {
        final String[] bar3 = {"Pasek (vanilla)", "Liczba", "Pasek + liczba"};
        final String[] hp3 = {"Pasek", "Liczba", "Pasek + liczba"};
        final String[] mode2 = {"Tylko uderzenie", "Tylko patrzenie", "Uderzenie lub patrzenie"};

        // --- Target HUD
        List<Row> t = new ArrayList<>();
        t.add(new Header("Panel celu"));
        t.add(toggle("Włączony", () -> ModConfig.I.targetEnabled, v -> ModConfig.I.targetEnabled = v));
        t.add(new Cycle("Kiedy pokazać", mode2, () -> ModConfig.I.targetMode, v -> ModConfig.I.targetMode = v));
        t.add(slider("Czas pokazania", 1, 15, 0.5, "%.1f s", () -> ModConfig.I.targetTime, v -> ModConfig.I.targetTime = (float) v));
        t.add(toggle("Układ pionowy", () -> ModConfig.I.targetVertical, v -> ModConfig.I.targetVertical = v));
        t.add(slider("Pozycja X", 0, 1, 0.005, "%.3f", () -> ModConfig.I.targetX, v -> ModConfig.I.targetX = (float) v));
        t.add(slider("Pozycja Y", 0, 1, 0.005, "%.3f", () -> ModConfig.I.targetY, v -> ModConfig.I.targetY = (float) v));
        t.add(slider("Skala", 0.5, 3, 0.05, "%.2fx", () -> ModConfig.I.targetScale, v -> ModConfig.I.targetScale = (float) v));
        t.add(new Header("Zawartość"));
        t.add(toggle("Zdrowie", () -> ModConfig.I.targetShowHealth, v -> ModConfig.I.targetShowHealth = v));
        t.add(toggle("Serca zamiast HP", () -> ModConfig.I.targetHearts, v -> ModConfig.I.targetHearts = v));
        t.add(new Cycle("Styl zdrowia", hp3, () -> ModConfig.I.targetHealthStyle, v -> ModConfig.I.targetHealthStyle = v));
        t.add(toggle("Itemy w rękach", () -> ModConfig.I.targetShowHands, v -> ModConfig.I.targetShowHands = v));
        t.add(toggle("Zbroja", () -> ModConfig.I.targetShowArmor, v -> ModConfig.I.targetShowArmor = v));
        t.add(new Header("Wygląd"));
        t.add(color("Tło", () -> ModConfig.I.targetBg, v -> ModConfig.I.targetBg = v));
        t.add(color("Ramka", () -> ModConfig.I.targetBorder, v -> ModConfig.I.targetBorder = v));
        t.add(color("Tekst", () -> ModConfig.I.targetText, v -> ModConfig.I.targetText = v));
        t.add(slider("Zaokrąglenie", 0, 12, 1, "%.0f px", () -> ModConfig.I.targetRadius, v -> ModConfig.I.targetRadius = (int) v));
        tabs.add(new Tab("Panel celu", t));

        // --- Status HUD
        List<Row> s = new ArrayList<>();
        s.add(new Header("Ogólne"));
        s.add(toggle("Cały HUD moda", () -> ModConfig.I.hudEnabled, v -> ModConfig.I.hudEnabled = v));
        s.add(toggle("Ukryj vanilla efekty", () -> ModConfig.I.hideVanillaEffects, v -> ModConfig.I.hideVanillaEffects = v));
        s.add(new Cycle("Wytrzymałość", bar3, () -> ModConfig.I.statusDurStyle, v -> ModConfig.I.statusDurStyle = v));
        s.add(toggle("Liczba itemów zamiast dura", () -> ModConfig.I.itemCountInsteadOfDura, v -> ModConfig.I.itemCountInsteadOfDura = v));
        s.add(new Info("np. totem: pokaże ile totemów masz w ekwipunku"));
        elemRows(s, "Effect status (twoje efekty)", ModConfig.I.effectsHud, true);
        elemRows(s, "Armor status (twoja zbroja)", ModConfig.I.armorHud, false);
        elemRows(s, "Item status (ręka / offhand)", ModConfig.I.itemHud, false);
        tabs.add(new Tab("Status HUD", s));

        // --- Hitboxy
        List<Row> h = new ArrayList<>();
        h.add(new Header("Hitboxy graczy"));
        h.add(toggle("Włączone", () -> ModConfig.I.hitboxEnabled, v -> ModConfig.I.hitboxEnabled = v));
        h.add(color("Kolor", () -> ModConfig.I.hitboxColor, v -> ModConfig.I.hitboxColor = v));
        h.add(slider("Grubość", 0.5, 10, 0.5, "%.1f", () -> ModConfig.I.hitboxThickness, v -> ModConfig.I.hitboxThickness = (float) v));
        h.add(slider("Zasięg", 8, 128, 4, "%.0f m", () -> ModConfig.I.hitboxRange, v -> ModConfig.I.hitboxRange = (float) v));
        h.add(toggle("Inny kolor gdy patrzę", () -> ModConfig.I.hitboxLookEnabled, v -> ModConfig.I.hitboxLookEnabled = v));
        h.add(color("Kolor (patrzę)", () -> ModConfig.I.hitboxLookColor, v -> ModConfig.I.hitboxLookColor = v));
        h.add(new Info("Hitboxy nie są widoczne przez ściany."));
        tabs.add(new Tab("Hitboxy", h));

        // --- Marker
        List<Row> m = new ArrayList<>();
        m.add(new Header("Marker na celu"));
        m.add(toggle("Włączony", () -> ModConfig.I.markerEnabled, v -> ModConfig.I.markerEnabled = v));
        m.add(color("Kolor", () -> ModConfig.I.markerColor, v -> ModConfig.I.markerColor = v));
        m.add(slider("Rozmiar", 2, 30, 1, "%.0f", () -> ModConfig.I.markerSize, v -> ModConfig.I.markerSize = (float) v));
        m.add(slider("Prędkość obrotu", 0.1, 5, 0.1, "%.1fx", () -> ModConfig.I.markerSpeed, v -> ModConfig.I.markerSpeed = (float) v));
        m.add(new Info("Obracający się kwadrat w środku hitboxu gracza,"));
        m.add(new Info("na którego patrzysz."));
        tabs.add(new Tab("Marker", m));

        // --- Ekwipunek
        List<Row> i = new ArrayList<>();
        i.add(new Header("Tekstura ekwipunku"));
        i.add(toggle("Włączona", () -> ModConfig.I.invEnabled, v -> ModConfig.I.invEnabled = v));
        i.add(slider("Rozmiar", 0.2, 3, 0.05, "%.2fx", () -> ModConfig.I.invScale, v -> ModConfig.I.invScale = (float) v));
        i.add(slider("Przezroczystość", 0, 1, 0.01, "%.2f", () -> ModConfig.I.invAlpha, v -> ModConfig.I.invAlpha = (float) v));
        i.add(slider("Przesunięcie X", -150, 150, 1, "%.0f", () -> ModConfig.I.invOffX, v -> ModConfig.I.invOffX = (float) v));
        i.add(slider("Przesunięcie Y", -150, 150, 1, "%.0f", () -> ModConfig.I.invOffY, v -> ModConfig.I.invOffY = (float) v));
        i.add(new Action("Folder z obrazem", "Otwórz folder", () -> Util.getOperatingSystem().open(ModConfig.dir().toFile())));
        i.add(new Action("Odśwież obraz", "Wczytaj ponownie", ImageManager::reload));
        i.add(new Info("Wrzuć obraz jako inventory.png (lub .jpg/.bmp/.gif)"));
        i.add(new Info("do folderu config/pvphud i kliknij Wczytaj ponownie."));
        i.add(new Info("Zmiany widać od razu po otwarciu ekwipunku (E)."));
        tabs.add(new Tab("Ekwipunek", i));

        // --- Wygląd menu
        List<Row> a = new ArrayList<>();
        a.add(new Header("Wygląd menu"));
        a.add(color("Kolor akcentu", () -> ModConfig.I.accent, v -> ModConfig.I.accent = v | 0xFF000000));
        a.add(color("Kolor panelu", () -> ModConfig.I.menuColor, v -> ModConfig.I.menuColor = v | 0xFF000000));
        a.add(slider("Przezroczystość menu", 0.1, 1, 0.01, "%.2f", () -> ModConfig.I.menuOpacity, v -> ModConfig.I.menuOpacity = (float) v));
        a.add(slider("Zaokrąglenie", 0, 16, 1, "%.0f px", () -> ModConfig.I.menuRadius, v -> ModConfig.I.menuRadius = (int) v));
        tabs.add(new Tab("Wygląd menu", a));
    }
}
