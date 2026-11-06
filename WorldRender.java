package pl.novabomba;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Tracery, ESP Box i Glow rysowane w świecie + rzutowanie 3D→2D dla NameTagów i pasków HP. */
public final class WorldRender {
    private static final Matrix4f MV = new Matrix4f(), PR = new Matrix4f();
    private static Vec3 cam = Vec3.ZERO;
    private static boolean ok;

    public static void render(PoseStack ps, Camera camera, float pt) {
        Minecraft mc = Minecraft.getInstance();
        Config c = Config.get();
        if (mc.level == null || mc.player == null) return;
        Matrix4f m = ps.last().pose();
        MV.set(m);
        PR.set(RenderSystem.getProjectionMatrix());
        cam = camera.getPosition();
        ok = true;
        if (!c.modPlayers || Targets.nearby.isEmpty()) return;
        if (!(c.espBox || c.tracer || c.glow)) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2f);

        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 260.0);
        Vector3f look = camera.getLookVector();
        Vec3 start = new Vec3(look.x() * 0.6, look.y() * 0.6 - 0.12, look.z() * 0.6);

        for (AbstractClientPlayer p : Targets.nearby) {
            boolean mark = Targets.isMarked(p);
            if (!mark && !c.showAll) continue;
            float k = mark ? 1f : 0.35f;
            Vec3 pos = p.getPosition(pt).subtract(cam);
            double hw = p.getBbWidth() / 2.0, h = p.getBbHeight();
            double x0 = pos.x - hw, x1 = pos.x + hw, y0 = pos.y, y1 = pos.y + h, z0 = pos.z - hw, z1 = pos.z + hw;

            if (c.espBox) {
                int col = c.colors[3];
                fill(m, x0, y0, z0, x1, y1, z1, col, (0.10f + 0.08f * pulse) * k);
                box(m, x0, y0, z0, x1, y1, z1, col, 0.95f * k);
            }
            if (c.glow && mark) {
                int gcol = Draw.mix(c.colors[3], 0xFFFFFF, 0.25f);
                for (int i = 1; i <= 3; i++) {
                    double e = 0.045 * i;
                    box(m, x0 - e, y0 - e, z0 - e, x1 + e, y1 + e, z1 + e, gcol, (0.38f - 0.10f * i) * (0.6f + 0.4f * pulse));
                }
                fill(m, x0 - 0.1, y0 - 0.1, z0 - 0.1, x1 + 0.1, y1 + 0.1, z1 + 0.1, gcol, 0.05f * pulse);
            }
            if (c.tracer && mark) {
                int col = c.colors[2];
                line(m, start, new Vec3(pos.x, pos.y + h * 0.5, pos.z), col, Targets.isTarget(p) ? 1f : 0.7f);
            }
        }

        RenderSystem.lineWidth(1f);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** rzutowanie punktu świata na współrzędne GUI (null = za kamerą) */
    public static float[] project(Vec3 w) {
        if (!ok) return null;
        Minecraft mc = Minecraft.getInstance();
        Vector4f v = new Vector4f((float) (w.x - cam.x), (float) (w.y - cam.y), (float) (w.z - cam.z), 1f);
        v.mul(MV);
        v.mul(PR);
        if (v.w <= 0.01f) return null;
        float nx = v.x / v.w, ny = v.y / v.w;
        return new float[]{(nx * 0.5f + 0.5f) * mc.getWindow().getGuiScaledWidth(),
                (0.5f - ny * 0.5f) * mc.getWindow().getGuiScaledHeight()};
    }

    // ---------------- prymitywy ----------------
    private static void v(BufferBuilder bb, Matrix4f m, double x, double y, double z, float r, float g, float b, float a) {
        bb.vertex(m, (float) x, (float) y, (float) z).color(r, g, b, a).endVertex();
    }

    private static void line(Matrix4f m, Vec3 a, Vec3 b, int rgb, float alpha) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, bl = (rgb & 255) / 255f;
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        v(bb, m, a.x, a.y, a.z, r, g, bl, alpha * 0.15f);
        v(bb, m, b.x, b.y, b.z, r, g, bl, alpha);
        BufferUploader.drawWithShader(bb.end());
    }

    private static void box(Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1, int rgb, float a) {
        if (a <= 0.003f) return;
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        double[][] c = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1},
                {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] e = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] ed : e) {
            v(bb, m, c[ed[0]][0], c[ed[0]][1], c[ed[0]][2], r, g, b, a);
            v(bb, m, c[ed[1]][0], c[ed[1]][1], c[ed[1]][2], r, g, b, a);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void fill(Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1, int rgb, float a) {
        if (a <= 0.003f) return;
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        quad(bb, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a); // dół
        quad(bb, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a); // góra
        quad(bb, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        quad(bb, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(bb, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        quad(bb, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
        BufferUploader.drawWithShader(bb.end());
    }

    private static void quad(BufferBuilder bb, Matrix4f m, double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz, float r, float g, float b, float a) {
        v(bb, m, ax, ay, az, r, g, b, a);
        v(bb, m, bx, by, bz, r, g, b, a);
        v(bb, m, cx, cy, cz, r, g, b, a);
        v(bb, m, dx, dy, dz, r, g, b, a);
    }
}
