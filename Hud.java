package pl.novabomba;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** NameTag + pasek HP przy graczu oraz widget namierzonego gracza w prawym górnym rogu. */
public final class Hud {
    // stan widgetu (snapshot – pozwala wyświetlić fade-out po zniknięciu celu)
    private static float fade, slide, hpAnim, ghost;
    private static UUID lastId;
    private static String sName = "";
    private static ResourceLocation sSkin;
    private static float sHp, sMax, sDist, sReach, sRem = -1, sEl;
    private static int sPing;
    private static boolean sDead;

    public static void render(GuiGraphics g, float pt) {
        Anim.frame();
        Stats.frame();
        Minecraft mc = Minecraft.getInstance();
        Config c = Config.get();
        if (mc.player == null || mc.level == null) return;
        Draw.mul = 1f;
        if (c.modPlayers && (c.nameTag || c.hpBar)) drawTags(g, mc, c, pt);
        if (c.modPlayers && c.targetWidget) drawWidget(g, mc, c);
        Draw.mul = 1f;
    }

    // ------------------------------------------------------------------ NameTag + HP
    private static void drawTags(GuiGraphics g, Minecraft mc, Config c, float pt) {
        for (AbstractClientPlayer p : Targets.nearby) {
            boolean mark = Targets.isMarked(p);
            if (!mark && !c.showAll) continue;
            Vec3 pos = p.getPosition(pt);
            double h = p.getBbHeight();
            float[] top = WorldRender.project(new Vec3(pos.x, pos.y + h + 0.15, pos.z));
            float[] bot = WorldRender.project(new Vec3(pos.x, pos.y, pos.z));
            if (top == null || bot == null) continue;
            float k = mark ? 1f : 0.55f;
            int cx = Math.round(top[0]);
            float boxH = bot[1] - top[1];

            if (c.nameTag) {
                String s = p.getName().getString() + "  " + Draw.f("%.1f m", mc.player.distanceTo(p));
                int w = Draw.w(s) + 10, x = cx - w / 2, y = Math.round(top[1]) - 15;
                Draw.glow(g, x, y, w, 12, 5, c.colors[1], 0.10f * k, 3);
                Draw.round(g, x, y, w, 12, 5, c.colors[0], 0.78f * k);
                Draw.outline(g, x, y, w, 12, 5, c.colors[1], 0.65f * k);
                Draw.text(g, s, x + 5, y + 2, c.colors[4], k);
            }
            if (c.hpBar && boxH > 10) {
                float frac = Mth.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0f, 1f);
                int bh = Math.round(boxH), bx = cx - Math.round(boxH * 0.24f) - 7, by = Math.round(top[1]);
                int col = Draw.mix(c.colors[5], 0xFF2020, frac < 0.3f ? (0.3f - frac) / 0.3f * 0.6f : 0f);
                Draw.round(g, bx - 1, by - 1, 5, bh + 2, 2, 0x000000, 0.55f * k);
                int fh = Math.max(1, Math.round(bh * frac));
                Draw.round(g, bx, by + bh - fh, 3, fh, 1, col, 0.95f * k);
            }
        }
    }

    // ------------------------------------------------------------------ Widget celu
    private static void drawWidget(GuiGraphics g, Minecraft mc, Config c) {
        AbstractClientPlayer t = Targets.targetPlayer();
        if (t != null) {
            UUID id = t.getUUID();
            float max = Math.max(1f, t.getMaxHealth());
            float hp = Mth.clamp(t.getHealth(), 0f, max);
            if (!id.equals(lastId)) { lastId = id; hpAnim = ghost = hp / max; } // zmiana celu = natychmiastowa aktualizacja
            sName = t.getName().getString();
            sSkin = t.getSkinTextureLocation();
            sHp = hp; sMax = max;
            sDist = mc.player.distanceTo(t);
            sReach = (float) Combat.reach(mc, t);
            PlayerInfo pi = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(id) : null;
            sPing = pi != null ? pi.getLatency() : 0;
            sRem = Targets.lockRemaining();
            sEl = Targets.lockElapsed();
            sDead = t.isDeadOrDying();
            fade = Anim.approach(fade, 1f, 10f);
            slide = Anim.approach(slide, 1f, 9f);
        } else {
            fade = Anim.approach(fade, 0f, 8f);
            if (fade < 0.02f) { fade = 0f; slide = 0f; lastId = null; }
        }
        if (fade < 0.01f) return;

        float frac = Mth.clamp(sHp / sMax, 0f, 1f);
        hpAnim = Anim.approach(hpAnim, frac, 12f);
        ghost = ghost < hpAnim ? hpAnim : Anim.approach(ghost, hpAnim, 2.5f);

        final int W = 204, H = 66;
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = sw - W - 10 + Math.round((1f - Anim.ease(slide)) * (W + 24)), y = 10;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 400.0);
        int gui = c.colors[0], acc = c.colors[1], txt = c.colors[4];
        Draw.mul = fade;

        Draw.glow(g, x, y, W, H, 10, acc, 0.16f + 0.07f * pulse, 7);
        Draw.round(g, x, y, W, H, 10, gui, Math.max(0.6f, c.panelAlpha));
        Draw.vGradient(g, x + 10, y + 1, W - 20, 22, 0xFFFFFF, Theme.shine(), 0xFFFFFF, 0f);
        Draw.outline(g, x, y, W, H, 10, 0xFFFFFF, 0.14f);

        // avatar
        Draw.round(g, x + 7, y + 7, 36, 36, 7, acc, 0.9f);
        if (sSkin != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, Draw.mul);
            PlayerFaceRenderer.draw(g, sSkin, x + 9, y + 9, 32);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        // nick + status
        Draw.textB(g, sName, x + 50, y + 9, txt, 1f);
        int stCol; String st;
        if (sDead) { st = "MARTWY"; stCol = 0x8A8A8A; }
        else if (sReach <= c.attackRange) { st = "ATAK"; stCol = 0xFF4D5E; }
        else if (sDist <= 12f) { st = "BLISKO"; stCol = 0xFFC94D; }
        else { st = "CEL"; stCol = 0x4DFF88; }
        Draw.glow(g, x + W - 15, y + 11, 6, 6, 3, stCol, 0.35f, 3);
        Draw.round(g, x + W - 15, y + 11, 6, 6, 3, stCol, 1f);
        Draw.textR(g, st, x + W - 20, y + 10, stCol, 0.95f);

        // dystans / ping
        StringBuilder sb = new StringBuilder();
        if (c.widgetDistance) sb.append(Draw.f("%.1f m", sDist));
        if (c.widgetPing) { if (sb.length() > 0) sb.append("  |  "); sb.append(sPing).append(" ms"); }
        if (sb.length() > 0) Draw.text(g, sb.toString(), x + 50, y + 22, txt, 0.6f);

        // pasek HP (płynna animacja + „duch” poprzedniej wartości)
        int bx = x + 50, by = y + 35, bw = W - 58, bh = 8;
        int hpCol = Draw.mix(c.colors[5], 0xFF2020, frac < 0.3f ? (0.3f - frac) / 0.3f * 0.6f : 0f);
        Draw.round(g, bx, by, bw, bh, 4, 0xFFFFFF, 0.12f);
        if (ghost > 0.005f) Draw.round(g, bx, by, Math.max(3, Math.round(bw * ghost)), bh, 4, Draw.mix(hpCol, 0xFFFFFF, 0.55f), 0.45f);
        if (hpAnim > 0.005f) Draw.round(g, bx, by, Math.max(3, Math.round(bw * hpAnim)), bh, 4, hpCol, 1f);

        if (c.widgetHp) Draw.text(g, Draw.f("%.1f / %.1f HP", sHp, sMax), x + 50, y + 48, txt, 0.8f);
        String timer;
        if (sRem >= 0) timer = clock(sRem); else timer = "+" + clock(sEl);
        Draw.textR(g, timer, x + W - 8, y + 48, txt, 0.8f);

        // pasek odliczania namierzenia
        float lf = sRem >= 0 && c.lockSeconds > 0 ? Mth.clamp(sRem / c.lockSeconds, 0f, 1f) : 1f;
        Draw.round(g, x + 10, y + H - 5, Math.max(2, Math.round((W - 20) * lf)), 2, 1, acc, 0.9f);
        Draw.mul = 1f;
    }

    private static String clock(float s) {
        int t = Math.max(0, (int) Math.ceil(s));
        return Draw.f("%02d:%02d", t / 60, t % 60);
    }
}
