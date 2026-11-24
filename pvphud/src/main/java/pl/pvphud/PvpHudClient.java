package pl.pvphud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import pl.pvphud.gui.ConfigScreen;
import pl.pvphud.hud.Hud;
import pl.pvphud.hud.TargetTracker;
import pl.pvphud.render.WorldVisuals;

public class PvpHudClient implements ClientModInitializer {
    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvphud.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.pvphud"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenu.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ConfigScreen(null));
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof PlayerEntity p && p != MinecraftClient.getInstance().player) {
                TargetTracker.hit(p);
            }
            return ActionResult.PASS;
        });

        WorldVisuals.init();
    }
}
