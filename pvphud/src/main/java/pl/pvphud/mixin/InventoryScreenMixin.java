package pl.pvphud.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.pvphud.render.ImageManager;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    /** Po narysowaniu panelu vanilla, a przed slotami/itemami, kładziemy własny obraz. */
    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void pvphud$drawCustomBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        ImageManager.drawOnInventory(context, x, y, backgroundWidth, backgroundHeight);
    }
}
