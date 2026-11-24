package pl.pvphud.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import pl.pvphud.ModConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Własna tekstura ekwipunku. Wrzuć obraz (png/jpg/bmp/gif) do config/pvphud/ jako
 * "inventory.png" (albo .jpg/.jpeg/.bmp/.gif) i kliknij "Wczytaj ponownie" w menu.
 */
public final class ImageManager {
    private ImageManager() {}

    private static final Identifier ID = Identifier.of("pvphud", "inventory_bg");
    private static final String[] NAMES = {"inventory.png", "inventory.jpg", "inventory.jpeg", "inventory.bmp", "inventory.gif"};

    private static boolean loaded;
    private static boolean dirty = true;
    public static String status = "";

    public static void reload() {
        dirty = true;
    }

    private static void doLoad() {
        dirty = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (loaded) {
            mc.getTextureManager().destroyTexture(ID);
            loaded = false;
        }
        try {
            Path found = null;
            for (String n : NAMES) {
                Path p = ModConfig.dir().resolve(n);
                if (Files.exists(p)) {
                    found = p;
                    break;
                }
            }
            if (found == null) {
                status = "Brak pliku inventory.png w folderze config/pvphud";
                return;
            }
            NativeImage img;
            if (found.getFileName().toString().endsWith(".png")) {
                try (var in = Files.newInputStream(found)) {
                    img = NativeImage.read(in);
                }
            } else {
                BufferedImage bi = ImageIO.read(found.toFile());
                if (bi == null) {
                    status = "Nie można odczytać obrazu";
                    return;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(bi, "png", out);
                img = NativeImage.read(new ByteArrayInputStream(out.toByteArray()));
            }
            mc.getTextureManager().registerTexture(ID, new NativeImageBackedTexture(img));
            loaded = true;
            status = "Wczytano: " + found.getFileName();
        } catch (Exception e) {
            status = "Błąd: " + e.getMessage();
        }
    }

    /** Rysuje obraz na panelu ekwipunku (px,py,pw,ph = pozycja i rozmiar panelu vanilla). */
    public static void drawOnInventory(DrawContext c, int px, int py, int pw, int ph) {
        ModConfig k = ModConfig.I;
        if (!k.invEnabled) return;
        if (dirty) doLoad();
        if (!loaded) return;

        int w = Math.max(1, (int) (pw * k.invScale));
        int h = Math.max(1, (int) (ph * k.invScale));
        int x = px + (pw - w) / 2 + (int) k.invOffX;
        int y = py + (ph - h) / 2 + (int) k.invOffY;
        int color = (((int) (Math.max(0f, Math.min(1f, k.invAlpha)) * 255)) << 24) | 0xFFFFFF;

        c.drawTexture(RenderLayer::getGuiTextured, ID, x, y, 0f, 0f, w, h, w, h, color);
    }
}
