package pl.pvphud.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int pvphud$getX();

    @Accessor("y")
    int pvphud$getY();

    @Accessor("backgroundWidth")
    int pvphud$getBackgroundWidth();

    @Accessor("backgroundHeight")
    int pvphud$getBackgroundHeight();
}
