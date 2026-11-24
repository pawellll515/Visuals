package pl.pvphud.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import pl.pvphud.util.Fmt;

/** Pojedyncza "komórka" HUD: item (z durability / licznikiem) albo efekt (ikona + czas). */
public final class Cells {
    private Cells() {}

    public static final int W = 22;

    public static final class Cell {
        public ItemStack stack;
        public StatusEffectInstance eff;
        public String override;     // tekst zamiast durability (np. liczba totemów)

        public static Cell item(ItemStack s) {
            Cell c = new Cell();
            c.stack = s;
            return c;
        }

        public static Cell item(ItemStack s, String override) {
            Cell c = item(s);
            c.override = override;
            return c;
        }

        public static Cell effect(StatusEffectInstance e) {
            Cell c = new Cell();
            c.eff = e;
            return c;
        }
    }

    public static int cellH(boolean withNumber) {
        return withNumber ? 27 : 18;
    }

    /** style: 0 = pasek vanilla, 1 = liczba, 2 = oba */
    public static void draw(DrawContext c, TextRenderer tr, Cell cell, int x, int y, int style, int textColor) {
        if (cell.eff != null) {
            drawEffect(c, tr, cell.eff, x, y, textColor);
            return;
        }
        ItemStack s = cell.stack;
        if (s == null || s.isEmpty()) return;

        c.drawItem(s, x + 3, y + 1);

        // style < 0: brak wytrzymałości i liczników (np. itemy przeciwnika)
        if (style < 0) return;

        // pasek i tekst muszą być NAD itemem (item jest rysowany z przesunięciem Z), tak jak w vanilla
        c.getMatrices().push();
        c.getMatrices().translate(0f, 0f, 200f);

        boolean dmg = s.isDamageable();
        if (dmg && style != 1 && s.isItemBarVisible()) {
            int bx = x + 5, by = y + 14;
            c.fill(bx, by, bx + 13, by + 2, 0xFF000000);
            c.fill(bx, by, bx + s.getItemBarStep(), by + 1, 0xFF000000 | s.getItemBarColor());
        }

        String t = null;
        int col = textColor;
        if (cell.override != null) {
            t = cell.override;
        } else if (dmg && style != 0) {
            int left = s.getMaxDamage() - s.getDamage();
            t = String.valueOf(left);
            float ratio = left / (float) Math.max(1, s.getMaxDamage());
            col = 0xFF000000 | MathHelper.hsvToRgb(Math.max(0f, ratio) / 3f, 0.85f, 1f);
        }
        if (t != null) {
            c.drawCenteredTextWithShadow(tr, t, x + W / 2, y + 19, col);
        }
        c.getMatrices().pop();
    }

    private static void drawEffect(DrawContext c, TextRenderer tr, StatusEffectInstance e, int x, int y, int textColor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(e.getEffectType());
        c.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, x + 3, y + 1, 16, 16);

        if (e.getAmplifier() > 0) {
            String lvl = Fmt.roman(e.getAmplifier() + 1);
            c.getMatrices().push();
            c.getMatrices().translate(0f, 0f, 200f);
            c.drawTextWithShadow(tr, lvl, x + W - tr.getWidth(lvl), y, 0xFFFFFFFF);
            c.getMatrices().pop();
        }
        c.drawCenteredTextWithShadow(tr, Fmt.duration(e), x + W / 2, y + 19, textColor);
    }
}
