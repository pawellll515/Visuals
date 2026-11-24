package pl.pvphud.util;

import net.minecraft.entity.effect.StatusEffectInstance;

public final class Fmt {
    private Fmt() {}

    /** 1m21s -> "1m", 45s -> "45s", 2h5m -> "2h". */
    public static String duration(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int sec = Math.max(0, e.getDuration() / 20);
        if (sec >= 3600) return (sec / 3600) + "h";
        if (sec >= 60) return (sec / 60) + "m";
        return sec + "s";
    }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static String roman(int n) {
        return n >= 0 && n < ROMAN.length ? ROMAN[n] : String.valueOf(n);
    }
}
