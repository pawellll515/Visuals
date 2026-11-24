package pl.pvphud.util;

import net.minecraft.client.gui.DrawContext;

/** Proste rysowanie zaokrąglonych prostokątów (bez shaderów, tylko fill). */
public final class Gfx {
    private Gfx() {}

    private static int inset(int j, int h, int r) {
        if (r <= 0) return 0;
        int d = Math.min(j, h - 1 - j);
        if (d >= r) return 0;
        double dy = r - d - 0.5;
        return (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    public static void rounded(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int j = 0; j < r; j++) {
            int in = inset(j, h, r);
            c.fill(x + in, y + j, x + w - in, y + j + 1, color);
            c.fill(x + in, y + h - 1 - j, x + w - in, y + h - j, color);
        }
        c.fill(x, y + r, x + w, y + h - r, color);
    }

    private static void ringRow(DrawContext c, int x, int y, int w, int h, int r, int j, int color) {
        int o = inset(j, h, r);
        if (j == 0 || j == h - 1) {
            c.fill(x + o, y + j, x + w - o, y + j + 1, color);
            return;
        }
        int ii = 1 + inset(j - 1, h - 2, Math.max(0, r - 1));
        if (ii <= o) ii = o + 1;
        c.fill(x + o, y + j, x + ii, y + j + 1, color);
        c.fill(x + w - ii, y + j, x + w - o, y + j + 1, color);
    }

    /** Zaokrąglona ramka o grubości 1 px. */
    public static void ring(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int j = 0; j < r; j++) {
            ringRow(c, x, y, w, h, r, j, color);
            ringRow(c, x, y, w, h, r, h - 1 - j, color);
        }
        c.fill(x, y + r, x + 1, y + h - r, color);
        c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        if (r == 0) {
            c.fill(x, y, x + w, y + 1, color);
            c.fill(x, y + h - 1, x + w, y + h, color);
        }
    }

    public static int withAlpha(int argb, float mul) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, mul)));
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
