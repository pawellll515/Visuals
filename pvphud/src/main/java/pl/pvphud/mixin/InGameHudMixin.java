package pl.pvphud.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.pvphud.ModConfig;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    /** Ukrywa vanilla ikony efektów w prawym górnym rogu. require = 0: gdyby metoda się nie zgadzała, gra się nie wywali. */
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void pvphud$hideVanillaEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ModConfig k = ModConfig.I;
        if (k.hudEnabled && k.hideVanillaEffects) ci.cancel();
    }
}
