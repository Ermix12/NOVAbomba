package pl.novabomba;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Prymitywy rysowania: zaokrąglone prostokąty, obrys, neon-glow, tekst. `mul` = globalna przezroczystość (fade). */
public final class Draw {
    public static float mul = 1f;
    private Draw() {}

    public static int argb(int rgb, float a) {
        int al = (int) (Mth.clamp(a * mul, 0f, 1f) * 255f + 0.5f);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static void rect(GuiGraphics g, int x, int y, int w, int h, int rgb, float a) {
        if (a * mul < 0.004f || w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, argb(rgb, a));
    }

    private static int inset(int i, int h, int r) {
        if (r <= 0) return 0;
        int j = i < r ? i : (i >= h - r ? h - 1 - i : -1);
        if (j < 0) return 0;
        double dy = r - j - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    public static void round(GuiGraphics g, int x, int y, int w, int h, int r, int rgb, float a) {
        if (a * mul < 0.004f || w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        int c = argb(rgb, a);
        for (int i = 0; i < r; i++) {
            int in = inset(i, h, r);
            g.fill(x + in, y + i, x + w - in, y + i + 1, c);
            int k = h - 1 - i;
            g.fill(x + in, y + k, x + w - in, y + k + 1, c);
        }
        if (h - 2 * r > 0) g.fill(x, y + r, x + w, y + h - r, c);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int r, int rgb, float a) {
        if (a * mul < 0.004f || w < 4 || h < 4) return;
        r = Math.min(r, Math.min(w, h) / 2);
        int c = argb(rgb, a);
        int ri = Math.max(0, r - 1);
        for (int i = 0; i < h; i++) {
            int io = inset(i, h, r);
            if (i == 0 || i == h - 1) { g.fill(x + io, y + i, x + w - io, y + i + 1, c); continue; }
            int ii = 1 + inset(i - 1, h - 2, ri);
            int le = Math.max(io + 1, ii);
            g.fill(x + io, y + i, x + le, y + i + 1, c);
            g.fill(x + w - le, y + i, x + w - io, y + i + 1, c);
        }
    }

    /** neonowa poświata wokół prostokąta */
    public static void glow(GuiGraphics g, int x, int y, int w, int h, int r, int rgb, float a, int layers) {
        for (int i = layers; i >= 1; i--) round(g, x - i, y - i, w + 2 * i, h + 2 * i, r + i, rgb, a / layers);
    }

    public static void vGradient(GuiGraphics g, int x, int y, int w, int h, int rgb1, float a1, int rgb2, float a2) {
        g.fillGradient(x, y, x + w, y + h, argb(rgb1, a1), argb(rgb2, a2));
    }

    // ---- tekst ----
    public static int w(String s) { return Minecraft.getInstance().font.width(s); }
    public static int wB(String s) { return Minecraft.getInstance().font.width(Component.literal(s).withStyle(ChatFormatting.BOLD)); }

    public static void text(GuiGraphics g, String s, int x, int y, int rgb, float a) {
        int c = argb(rgb, a);
        if ((c >>> 24) < 6) return;
        g.drawString(Minecraft.getInstance().font, s, x, y, c, false);
    }

    public static void textB(GuiGraphics g, String s, int x, int y, int rgb, float a) {
        int c = argb(rgb, a);
        if ((c >>> 24) < 6) return;
        g.drawString(Minecraft.getInstance().font, Component.literal(s).withStyle(ChatFormatting.BOLD), x, y, c, false);
    }

    public static void textC(GuiGraphics g, String s, int cx, int y, int rgb, float a) { text(g, s, cx - w(s) / 2, y, rgb, a); }
    public static void textR(GuiGraphics g, String s, int rx, int y, int rgb, float a) { text(g, s, rx - w(s), y, rgb, a); }

    public static String f(String fmt, Object... a) { return String.format(Locale.ROOT, fmt, a); }
    public static String hex(int rgb) { return String.format("#%06X", rgb & 0xFFFFFF); }
}
