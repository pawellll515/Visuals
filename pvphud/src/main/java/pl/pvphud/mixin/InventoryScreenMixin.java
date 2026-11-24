package pl.pvphud.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.pvphud.render.ImageManager;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    /** Po narysowaniu panelu vanilla, a przed slotami/itemami, kładziemy własny obraz. */
    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void pvphud$drawCustomBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        HandledScreenAccessor a = (HandledScreenAccessor) (Object) this;
        ImageManager.drawOnInventory(context, a.pvphud$getX(), a.pvphud$getY(),
                a.pvphud$getBackgroundWidth(), a.pvphud$getBackgroundHeight());
    }
}
