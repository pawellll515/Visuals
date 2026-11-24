package pl.pvphud.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import pl.pvphud.ModConfig;
import pl.pvphud.gui.ConfigScreen;

/** Pamięta ostatniego gracza, na którego patrzyłeś / którego uderzyłeś. */
public final class TargetTracker {
    private TargetTracker() {}

    private static PlayerEntity target;
    private static long lastMs;

    public static void hit(PlayerEntity p) {
        target = p;
        lastMs = System.currentTimeMillis();
    }

    public static PlayerEntity lookedAt(MinecraftClient mc) {
        if (mc.crosshairTarget instanceof EntityHitResult r
                && r.getEntity() instanceof PlayerEntity p
                && p != mc.player) {
            return p;
        }
        return null;
    }

    /** Gracz, którego panel ma być teraz pokazany (albo null). */
    public static PlayerEntity current(MinecraftClient mc) {
        ModConfig k = ModConfig.I;
        // przy otwartym menu pokazujemy podgląd na sobie
        if (mc.currentScreen instanceof ConfigScreen) return mc.player;

        PlayerEntity looked = lookedAt(mc);
        if (k.targetMode == 1 && looked != null) {
            target = looked;
            lastMs = System.currentTimeMillis();
        }
        if (target == null) return null;
        if (target.isRemoved() || !target.isAlive()) return null;
        if (System.currentTimeMillis() - lastMs > (long) (k.targetTime * 1000f)) return null;
        return target;
    }
}
