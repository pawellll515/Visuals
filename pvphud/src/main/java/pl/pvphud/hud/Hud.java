package pl.pvphud.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import pl.pvphud.ModConfig;
import pl.pvphud.gui.ConfigScreen;
import pl.pvphud.hud.Cells.Cell;
import pl.pvphud.util.Gfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Hud {
    private Hud() {}

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Maks. efektów w jednym rzędzie (poziomo) / kolumnie (pionowo) w panelu celu. */
    private static final int FX_PER_LINE = 6;

    /** Kolejność, w jakiej efekty pojawiły się u gracza (najstarszy pierwszy). */
    private static final List<RegistryEntry<StatusEffect>> ORDER = new ArrayList<>();

    public static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ModConfig k = ModConfig.I;
        if (!k.hudEnabled || mc.player == null || mc.options.hudHidden) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ConfigScreen)) return;

        renderTarget(c, mc, k);
        renderSelf(c, mc, k);
    }

    // ------------------------------------------------------------------ TARGET

    private static void renderTarget(DrawContext c, MinecraftClient mc, ModConfig k) {
        if (!k.targetEnabled) return;
        PlayerEntity t = TargetTracker.current(mc);
        if (t == null) return;
        TextRenderer tr = mc.textRenderer;

        List<Cell> eq = new ArrayList<>();
        List<Cell> fx = new ArrayList<>();
        if (k.targetShowHands) {
            addIfNotEmpty(eq, t.getMainHandStack());
            addIfNotEmpty(eq, t.getOffHandStack());
        }
        if (k.targetShowArmor) {
            for (EquipmentSlot s : ARMOR) addIfNotEmpty(eq, t.getEquippedStack(s));
        }
        if (k.targetShowEffects) {
            int n = 0;
            for (StatusEffectInstance e : t.getStatusEffects()) {
                if (n++ >= k.targetMaxEffects) break;
                fx.add(Cell.effect(e));
            }
        }

        int chEq = Cells.cellH(false);  // itemy przeciwnika: sama ikona
        int chFx = Cells.cellH(true);   // efekty: ikona + czas
        int pad = 5;
        int W = Cells.W;
        boolean vert = k.targetVertical;

        String name = t.getName().getString();
        float hp = t.getHealth();
        float max = Math.max(1f, t.getMaxHealth());
        float abs = t.getAbsorptionAmount();
        float div = k.targetHearts ? 2f : 1f;
        String hpText = fmt1(hp / div) + (abs > 0 ? "+" + fmt1(abs / div) : "");
        boolean hpNum = k.targetShowHealth && k.targetHealthStyle != 0;
        boolean hpBar = k.targetShowHealth && k.targetHealthStyle != 1;

        int headW = tr.getWidth(name) + (hpNum ? tr.getWidth(hpText) + 10 : 0);
        int headH = 10 + (hpBar ? 6 : 0);

        // rozmiary bloków: itemy w jednej linii, efekty zawijane co FX_PER_LINE
        int ne = eq.size(), nf = fx.size();
        int eqW = ne == 0 ? 0 : (vert ? W : ne * W);
        int eqH = ne == 0 ? 0 : (vert ? ne * chEq : chEq);
        int fxAcross = nf == 0 ? 0 : (vert ? (nf + FX_PER_LINE - 1) / FX_PER_LINE : Math.min(nf, FX_PER_LINE));
        int fxDown = nf == 0 ? 0 : (vert ? Math.min(nf, FX_PER_LINE) : (nf + FX_PER_LINE - 1) / FX_PER_LINE);
        int fxW = fxAcross * W;
        int fxH = fxDown * chFx;

        int w, h;
        if (!vert) {
            w = Math.max(Math.max(headW, Math.max(eqW, fxW)), 96) + pad * 2;
            h = pad + headH + 2;
            if (ne > 0) h += eqH + 2;
            if (nf > 0) h += fxH + 2;
            h += pad - 2;
        } else {
            int cols = eqW + fxW + (ne > 0 && nf > 0 ? 4 : 0);
            w = Math.max(Math.max(headW, cols), 96) + pad * 2;
            int rows = Math.max(eqH, fxH);
            h = pad + headH + 2 + (rows > 0 ? rows + 2 : 0) + pad - 2;
        }

        float sc = k.targetScale;
        int px = (int) (c.getScaledWindowWidth() * k.targetX - w * sc / 2f);
        int py = (int) (c.getScaledWindowHeight() * k.targetY - h * sc / 2f);

        c.getMatrices().push();
        c.getMatrices().translate(px, py, 0);
        c.getMatrices().scale(sc, sc, 1f);

        Gfx.rounded(c, 0, 0, w, h, k.targetRadius, k.targetBg);
        Gfx.ring(c, 0, 0, w, h, k.targetRadius, k.targetBorder);

        int y = pad;
        c.drawTextWithShadow(tr, name, pad, y, k.targetText);
        if (hpNum) {
            int col = 0xFF000000 | MathHelper.hsvToRgb(MathHelper.clamp(hp / max, 0f, 1f) / 3f, 0.8f, 1f);
            c.drawTextWithShadow(tr, hpText, w - pad - tr.getWidth(hpText), y, col);
        }
        y += 10;
        if (hpBar) {
            int bw = w - pad * 2;
            float ratio = MathHelper.clamp(hp / max, 0f, 1f);
            int col = 0xFF000000 | MathHelper.hsvToRgb(ratio / 3f, 0.8f, 1f);
            c.fill(pad, y, pad + bw, y + 4, 0x80000000);
            c.fill(pad, y, pad + (int) (bw * ratio), y + 4, col);
            if (abs > 0) {
                float ar = MathHelper.clamp(abs / max, 0f, 1f);
                c.fill(pad, y, pad + (int) (bw * ar), y + 1, 0xFFFFD700);
            }
            y += 6;
        }
        y += 2;

        if (!vert) {
            if (ne > 0) {
                drawGrid(c, tr, eq, pad, y, Integer.MAX_VALUE, false, chEq, -1, k.targetText);
                y += eqH + 2;
            }
            if (nf > 0) {
                drawGrid(c, tr, fx, pad, y, FX_PER_LINE, false, chFx, 0, k.targetText);
            }
        } else {
            int x = pad;
            if (ne > 0) {
                drawGrid(c, tr, eq, x, y, Integer.MAX_VALUE, true, chEq, -1, k.targetText);
                x += eqW + 4;
            }
            if (nf > 0) {
                drawGrid(c, tr, fx, x, y, FX_PER_LINE, true, chFx, 0, k.targetText);
            }
        }
        c.getMatrices().pop();
    }

    // -------------------------------------------------------------------- SELF

    private static void renderSelf(DrawContext c, MinecraftClient mc, ModConfig k) {
        PlayerEntity p = mc.player;
        TextRenderer tr = mc.textRenderer;

        if (k.effectsHud.enabled) {
            List<Cell> cells = new ArrayList<>();
            int n = 0;
            for (StatusEffectInstance e : orderedEffects(p)) {
                if (n++ >= k.effectsHud.max) break;
                cells.add(Cell.effect(e));
            }
            // efekty: nowe dochodzą od strony "do środka ekranu" (zob. drawElem)
            drawElem(c, tr, k.effectsHud, cells, true, k.statusDurStyle, true);
        }

        if (k.armorHud.enabled) {
            List<Cell> cells = new ArrayList<>();
            for (EquipmentSlot s : ARMOR) addIfNotEmpty(cells, p.getEquippedStack(s));
            drawElem(c, tr, k.armorHud, cells, k.statusDurStyle != 0, k.statusDurStyle, false);
        }

        if (k.itemHud.enabled) {
            List<Cell> cells = new ArrayList<>();
            addHeld(cells, p, p.getMainHandStack(), k);
            addHeld(cells, p, p.getOffHandStack(), k);
            boolean num = k.statusDurStyle != 0 || k.itemCountInsteadOfDura;
            drawElem(c, tr, k.itemHud, cells, num, k.statusDurStyle, false);
        }
    }

    /** Efekty w stałej kolejności: najstarszy pierwszy, nowe na końcu (gra sama potrafi zmieniać kolejność). */
    private static List<StatusEffectInstance> orderedEffects(PlayerEntity p) {
        ORDER.removeIf(e -> p.getStatusEffect(e) == null);
        for (StatusEffectInstance i : p.getStatusEffects()) {
            if (!ORDER.contains(i.getEffectType())) ORDER.add(i.getEffectType());
        }
        List<StatusEffectInstance> out = new ArrayList<>();
        for (RegistryEntry<StatusEffect> e : ORDER) {
            StatusEffectInstance inst = p.getStatusEffect(e);
            if (inst != null) out.add(inst);
        }
        return out;
    }

    private static void addHeld(List<Cell> out, PlayerEntity p, ItemStack s, ModConfig k) {
        if (s.isEmpty()) return;
        if (!s.isDamageable() && k.itemCountInsteadOfDura) {
            out.add(Cell.item(s, String.valueOf(countInInventory(p, s))));
        } else {
            out.add(Cell.item(s));
        }
    }

    /** Liczy takie same itemy w głównym ekwipunku i offhandzie (bez pancerza). */
    private static int countInInventory(PlayerEntity p, ItemStack ref) {
        PlayerInventory inv = p.getInventory();
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (i >= 36 && i <= 39) continue; // sloty pancerza
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && ItemStack.areItemsEqual(s, ref)) total += s.getCount();
        }
        return total;
    }

    /**
     * Rysuje element własnego HUD-u.
     * growInward = true (efekty): przy układzie poziomym element jest zakotwiczony przy krawędzi bliższej brzegowi ekranu.
     * Lewa połowa ekranu: x = lewa krawędź, nowe efekty dochodzą z prawej.
     * Prawa połowa ekranu: x = prawa krawędź, nowe efekty dochodzą z lewej (najstarszy zostaje na swoim miejscu).
     */
    private static void drawElem(DrawContext c, TextRenderer tr, ModConfig.Elem e, List<Cell> cells,
                                 boolean withNumber, int style, boolean growInward) {
        if (cells.isEmpty()) return;
        ModConfig k = ModConfig.I;
        int ch = Cells.cellH(withNumber);
        int n = cells.size();
        int w = (e.vertical ? Cells.W : n * Cells.W) + 8;
        int h = (e.vertical ? n * ch : ch) + 8;

        boolean rightSide = growInward && !e.vertical && e.x >= 0.5f;
        List<Cell> toDraw = cells;
        if (rightSide) {
            toDraw = new ArrayList<>(cells);
            Collections.reverse(toDraw); // najnowszy po lewej, najstarszy przy prawej krawędzi
        }

        int px = (int) (c.getScaledWindowWidth() * e.x);
        if (rightSide) px -= (int) (w * e.scale);
        int py = (int) (c.getScaledWindowHeight() * e.y);

        c.getMatrices().push();
        c.getMatrices().translate(px, py, 0);
        c.getMatrices().scale(e.scale, e.scale, 1f);
        if (e.bg) {
            Gfx.rounded(c, 0, 0, w, h, k.targetRadius, k.targetBg);
            Gfx.ring(c, 0, 0, w, h, k.targetRadius, k.targetBorder);
        }
        drawGrid(c, tr, toDraw, 4, 4, Integer.MAX_VALUE, e.vertical, ch, style, k.targetText);
        c.getMatrices().pop();
    }

    // ----------------------------------------------------------------- HELPERS

    /**
     * Układa komórki w siatce. perLine = ile komórek w jednej linii.
     * Poziomo: linia = rząd, kolejne linie schodzą w dół. Pionowo: linia = kolumna, kolejne linie idą w prawo.
     */
    private static void drawGrid(DrawContext c, TextRenderer tr, List<Cell> cells, int x, int y, int perLine,
                                 boolean vertical, int ch, int style, int textColor) {
        for (int i = 0; i < cells.size(); i++) {
            int line = i / perLine;
            int idx = i % perLine;
            int cx = vertical ? x + line * Cells.W : x + idx * Cells.W;
            int cy = vertical ? y + idx * ch : y + line * ch;
            Cells.draw(c, tr, cells.get(i), cx, cy, style, textColor);
        }
    }

    private static void addIfNotEmpty(List<Cell> list, ItemStack s) {
        if (!s.isEmpty()) list.add(Cell.item(s));
    }

    private static String fmt1(float v) {
        return Math.abs(v - Math.round(v)) < 0.05f ? String.valueOf(Math.round(v)) : String.format("%.1f", v);
    }
}
