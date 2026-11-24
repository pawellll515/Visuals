package pl.pvphud.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pl.pvphud.ModConfig;
import pl.pvphud.hud.TargetTracker;

/**
 * Renderowanie w świecie: własne hitboxy graczy + obracający się marker na celu.
 * Hitboxy respektują głębię (nie widać ich przez ściany), marker rysuje się tylko, gdy
 * faktycznie patrzysz na gracza (crosshair), więc nie daje żadnej przewagi.
 */
public final class WorldVisuals {
    private WorldVisuals() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldVisuals::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ModConfig k = ModConfig.I;
        if (!k.hudEnabled || mc.player == null || mc.world == null) return;

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (ms == null || vcp == null) return;

        float td = ctx.tickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        PlayerEntity looked = TargetTracker.lookedAt(mc);

        if (k.hitboxEnabled) {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getDebugQuads());
            Matrix4f m = ms.peek().getPositionMatrix();
            double range2 = (double) k.hitboxRange * k.hitboxRange;
            float half = k.hitboxThickness * 0.005f;

            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || p.isSpectator()) continue;
                if (p.squaredDistanceTo(mc.player) > range2) continue;

                Vec3d off = p.getLerpedPos(td).subtract(p.getPos());
                Box b = p.getBoundingBox().offset(off).offset(-cam.x, -cam.y, -cam.z);
                int color = (k.hitboxLookEnabled && p == looked) ? k.hitboxLookColor : k.hitboxColor;
                edges(vc, m, b, half, color);
            }
        }

        if (k.markerEnabled && looked != null) {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getGuiOverlay());
            Vec3d pos = looked.getLerpedPos(td).add(0, looked.getHeight() / 2.0, 0);

            ms.push();
            ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
            ms.multiply(ctx.camera().getRotation());
            float angle = (System.nanoTime() / 1.0e9f * 90f * k.markerSpeed) % 360f;
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            Matrix4f m = ms.peek().getPositionMatrix();

            float s = k.markerSize * 0.01f;
            float t = Math.max(s * 0.18f, 0.004f);
            int col = k.markerColor;
            int fill = (col & 0xFFFFFF) | (((col >>> 24) / 4) << 24);

            quad2(vc, m, fill, -s, -s, 0, -s, s, 0, s, s, 0, s, -s, 0);
            // ramka
            quad2(vc, m, col, -s, s - t, 0, -s, s, 0, s, s, 0, s, s - t, 0);
            quad2(vc, m, col, -s, -s, 0, -s, -s + t, 0, s, -s + t, 0, s, -s, 0);
            quad2(vc, m, col, -s, -s, 0, -s, s, 0, -s + t, s, 0, -s + t, -s, 0);
            quad2(vc, m, col, s - t, -s, 0, s - t, s, 0, s, s, 0, s, -s, 0);
            ms.pop();
        }
    }

    private static void edges(VertexConsumer v, Matrix4f m, Box b, float h, int c) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        float[] ys = {y0, y1}, zs = {z0, z1}, xs = {x0, x1};

        for (float y : ys) for (float z : zs) box(v, m, x0 - h, y - h, z - h, x1 + h, y + h, z + h, c);
        for (float x : xs) for (float z : zs) box(v, m, x - h, y0 - h, z - h, x + h, y1 + h, z + h, c);
        for (float x : xs) for (float y : ys) box(v, m, x - h, y - h, z0 - h, x + h, y + h, z1 + h, c);
    }

    private static void box(VertexConsumer v, Matrix4f m,
                            float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        quad(v, m, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(v, m, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(v, m, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(v, m, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(v, m, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(v, m, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    /** Rysuje quad z obu stron (marker jest cienki i bez tego znika przy włączonym culling). */
    private static void quad2(VertexConsumer v, Matrix4f m, int c,
                              float ax, float ay, float az, float bx, float by, float bz,
                              float cx, float cy, float cz, float dx, float dy, float dz) {
        quad(v, m, c, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz);
        quad(v, m, c, dx, dy, dz, cx, cy, cz, bx, by, bz, ax, ay, az);
    }

    private static void quad(VertexConsumer v, Matrix4f m, int c,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        v.vertex(m, ax, ay, az).color(c);
        v.vertex(m, bx, by, bz).color(c);
        v.vertex(m, cx, cy, cz).color(c);
        v.vertex(m, dx, dy, dz).color(c);
    }
}
