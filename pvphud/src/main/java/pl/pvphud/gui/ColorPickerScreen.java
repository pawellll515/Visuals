package pl.pvphud.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import pl.pvphud.ModConfig;
import pl.pvphud.util.Gfx;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Paleta kolorów jak w Paincie: kwadrat nasycenie/jasność, pasek odcienia, alfa i pole HEX. */
public class ColorPickerScreen extends Screen {
    private static final int S = 120;       // rozmiar kwadratu SV i wysokość paska hue
    private static final int PW = 270, PH = 206;

    private final Screen parent;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final String title;

    private float hue, sat, val;
    private int alpha;
    private int drag;                       // 0 nic, 1 SV, 2 hue, 3 alfa
    private boolean updatingHex;
    private TextFieldWidget hex;
    private int px, py;

    public ColorPickerScreen(Screen parent, String title, IntSupplier getter, IntConsumer setter) {
        super(Text.literal("Kolor"));
        this.parent = parent;
        this.title = title;
        this.getter = getter;
        this.setter = setter;
        int argb = getter.getAsInt();
        alpha = (argb >>> 24) & 0xFF;
        float[] hsv = hsv(argb & 0xFFFFFF);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;

        hex = new TextFieldWidget(textRenderer, px + 176, py + 34, 80, 16, Text.literal("HEX"));
        hex.setMaxLength(9);
        hex.setChangedListener(this::onHexChanged);
        addDrawableChild(hex);
        refreshHex();

        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(px + PW - 76, py + PH - 26, 64, 18).build());
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float d) {
        // przezroczyste tło: gra widoczna pod spodem
    }

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        ModConfig k = ModConfig.I;
        int panel = (k.menuColor & 0xFFFFFF) | (Math.max(96, (int) (k.menuOpacity * 255)) << 24);
        Gfx.rounded(c, px, py, PW, PH, k.menuRadius, panel);
        Gfx.ring(c, px, py, PW, PH, k.menuRadius, k.accent);
        c.drawTextWithShadow(textRenderer, title, px + 12, py + 10, 0xFFFFFFFF);

        int sx = px + 14, sy = py + 34;
        // kwadrat saturation/value
        for (int i = 0; i < S; i++) {
            int top = 0xFF000000 | MathHelper.hsvToRgb(hue, i / (S - 1f), 1f);
            c.fillGradient(sx + i, sy, sx + i + 1, sy + S, top, 0xFF000000);
        }
        int cx = sx + (int) (sat * (S - 1)), cy = sy + (int) ((1 - val) * (S - 1));
        c.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xFF000000);
        c.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF);
        c.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF000000 | rgb());

        // pasek hue
        int hx = px + 144;
        for (int j = 0; j < S; j++) {
            c.fill(hx, sy + j, hx + 14, sy + j + 1, 0xFF000000 | MathHelper.hsvToRgb(j / (S - 1f), 1f, 1f));
        }
        int hy = sy + (int) (hue * (S - 1));
        c.fill(hx - 2, hy - 1, hx + 16, hy + 2, 0xFFFFFFFF);

        // pasek alfa
        int ay = sy + S + 12, aw = 144 - 14 + 14;
        c.fill(sx, ay, sx + aw, ay + 12, 0xFF2A2A2A);
        for (int i = 0; i < aw; i++) {
            int a = (int) (255f * i / (aw - 1f));
            c.fill(sx + i, ay, sx + i + 1, ay + 12, (a << 24) | rgb());
        }
        int ax = sx + (int) (alpha / 255f * (aw - 1));
        c.fill(ax - 1, ay - 2, ax + 2, ay + 14, 0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer, "Przezroczystość " + (int) (alpha / 2.55f) + "%", sx, ay + 16, 0xFFBBBBBB);

        // podgląd
        c.drawTextWithShadow(textRenderer, "HEX", px + 176, py + 24, 0xFFBBBBBB);
        c.fill(px + 176, py + 60, px + 256, py + 100, 0xFF2A2A2A);
        c.fill(px + 176, py + 60, px + 256, py + 100, argb());
        Gfx.ring(c, px + 176, py + 60, 80, 40, 0, 0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer, "R " + ((rgb() >> 16) & 255), px + 176, py + 108, 0xFFFF8888);
        c.drawTextWithShadow(textRenderer, "G " + ((rgb() >> 8) & 255), px + 176, py + 120, 0xFF88FF88);
        c.drawTextWithShadow(textRenderer, "B " + (rgb() & 255), px + 176, py + 132, 0xFF8899FF);

        super.render(c, mx, my, d);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int sx = px + 14, sy = py + 34, hx = px + 144, ay = sy + S + 12;
        if (in(mx, my, sx, sy, S, S)) drag = 1;
        else if (in(mx, my, hx - 2, sy, 18, S)) drag = 2;
        else if (in(mx, my, sx, ay - 2, 144, 16)) drag = 3;
        else drag = 0;
        if (drag != 0) {
            applyDrag(mx, my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag != 0) {
            applyDrag(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        drag = 0;
        return super.mouseReleased(mx, my, button);
    }

    private void applyDrag(double mx, double my) {
        int sx = px + 14, sy = py + 34;
        if (drag == 1) {
            sat = MathHelper.clamp((float) (mx - sx) / (S - 1f), 0f, 1f);
            val = 1f - MathHelper.clamp((float) (my - sy) / (S - 1f), 0f, 1f);
        } else if (drag == 2) {
            hue = MathHelper.clamp((float) (my - sy) / (S - 1f), 0f, 1f);
        } else if (drag == 3) {
            alpha = Math.round(MathHelper.clamp((float) (mx - sx) / 143f, 0f, 1f) * 255f);
        }
        commit();
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ logic

    private int rgb() {
        return MathHelper.hsvToRgb(hue, sat, val) & 0xFFFFFF;
    }

    private int argb() {
        return (alpha << 24) | rgb();
    }

    private void commit() {
        setter.accept(argb());
        refreshHex();
    }

    private void refreshHex() {
        updatingHex = true;
        hex.setText(String.format("#%06X", rgb()));
        updatingHex = false;
    }

    private void onHexChanged(String s) {
        if (updatingHex) return;
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        try {
            if (t.length() == 6) {
                int rgb = Integer.parseInt(t, 16);
                float[] hsv = hsv(rgb);
                hue = hsv[0];
                sat = hsv[1];
                val = hsv[2];
                setter.accept(argb());
            } else if (t.length() == 8) { // RRGGBBAA
                long v = Long.parseLong(t, 16);
                int rgb = (int) (v >> 8) & 0xFFFFFF;
                alpha = (int) (v & 0xFF);
                float[] hsv = hsv(rgb);
                hue = hsv[0];
                sat = hsv[1];
                val = hsv[2];
                setter.accept(argb());
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static float[] hsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = (((g - b) / d) + 6f) % 6f / 6f;
        else if (max == g) h = (((b - r) / d) + 2f) / 6f;
        else h = (((r - g) / d) + 4f) / 6f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    @Override
    public void close() {
        ModConfig.save();
        client.setScreen(parent);
    }
}
