package pl.novabomba;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Główne GUI: Gracze / Atak / Historia / Ustawienia / Motywy / Profil. */
public class NovaScreen extends Screen {
    private static final int W = 560, H = 340, CX = 144, CY = 62, CW = 404, CH = 266;
    private static final String[] TABS = {"Gracze", "Atak", "Historia ataków", "Ustawienia", "Motywy", "Profil"};
    private static int tab = 0;

    private static final class Hit {
        int x, y, w, h; Runnable left, right; DoubleConsumer drag;
        Hit(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    private float open;
    private boolean closing, blurOn;
    private GuiGraphics g;
    private Config c;
    private final List<Hit> hits = new ArrayList<>();
    private Hit active;
    private double ox, oy;
    private float sc = 1f, tabY = -1f;
    private int mx, my, scroll;
    private String listening;
    private final Map<String, Float> anim = new HashMap<>();

    public NovaScreen() { super(Component.literal("NOVABOMBA")); }

    // ------------------------------------------------------------ cykl życia
    @Override protected void init() {
        c = Config.get();
        try {
            if (c.blur && minecraft.gameRenderer.currentEffect() == null) {
                minecraft.gameRenderer.loadEffect(new ResourceLocation("shaders/post/blur.json"));
                blurOn = minecraft.gameRenderer.currentEffect() != null;
            }
        } catch (Exception ignored) { }
    }

    @Override public void removed() {
        if (blurOn) { minecraft.gameRenderer.shutdownEffect(); blurOn = false; }
        Config.dirty();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        if (!Config.get().animations) { minecraft.setScreen(null); return; }
        closing = true;
    }

    @Override public void tick() { if (closing && open <= 0.02f) minecraft.setScreen(null); }

    // ------------------------------------------------------------ render
    @Override public void render(GuiGraphics gg, int mouseX, int mouseY, float pt) {
        g = gg; c = Config.get();
        open = Anim.linear(open, closing ? 0f : 1f, 6f);
        float e = Anim.ease(open);
        Draw.mul = e;
        try {
            Draw.rect(g, 0, 0, width, height, 0x000000, 0.35f);
            g.fillGradient(0, height / 2, width, height, 0, Draw.argb(c.colors[1], 0.12f));

            float fit = Math.min((width - 10f) / W, (height - 10f) / H);
            sc = Math.min(c.guiScale, fit) * (0.92f + 0.08f * e);
            ox = width / 2.0 + c.guiX - W * sc / 2.0;
            oy = height / 2.0 + c.guiY - H * sc / 2.0;
            mx = (int) ((mouseX - ox) / sc);
            my = (int) ((mouseY - oy) / sc);

            hits.clear();
            g.pose().pushPose();
            g.pose().translate((float) ox, (float) oy, 0f);
            g.pose().scale(sc, sc, 1f);
            drawWindow();
            g.pose().popPose();
        } finally { Draw.mul = 1f; }
    }

    private int col(int i) { return c.colors[i]; }
    private boolean in(int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void hit(int x, int y, int w, int h, Runnable l) { Hit t = new Hit(x, y, w, h); t.left = l; hits.add(t); }
    private void hitR(int x, int y, int w, int h, Runnable l, Runnable r) { Hit t = new Hit(x, y, w, h); t.left = l; t.right = r; hits.add(t); }
    private void hitDrag(int x, int y, int w, int h, DoubleConsumer d) { Hit t = new Hit(x, y, w, h); t.drag = d; hits.add(t); }
    private void label(String s, int x, int y, float a) { Draw.text(g, s, x, y, col(4), a); }
    private float animV(String k, float target, float speed) {
        float v = Anim.approach(anim.getOrDefault(k, target), target, speed);
        anim.put(k, v);
        return v;
    }

    private void drawWindow() {
        int acc = col(1);
        Draw.glow(g, 0, 0, W, H, 14, acc, 0.12f, 8);
        Draw.round(g, 0, 0, W, H, 14, col(0), c.panelAlpha);
        Draw.vGradient(g, 14, 1, W - 28, 70, 0xFFFFFF, Theme.shine(), 0xFFFFFF, 0f);
        Draw.outline(g, 0, 0, W, H, 14, 0xFFFFFF, 0.15f);

        // --- logo ---
        Draw.glow(g, 16, 11, 24, 24, 7, acc, 0.25f, 4);
        Draw.round(g, 16, 11, 24, 24, 7, acc, 1f);
        Draw.textB(g, "N", 16 + 12 - Draw.wB("N") / 2, 19, 0xFFFFFF, 1f);
        logo(48, 14, 1.6f);
        Draw.textR(g, "PvP Client  |  1.20 Forge/Fabric", W - 46, 19, col(4), 0.4f);
        // przycisk zamknięcia
        boolean hc = in(W - 34, 12, 22, 22);
        Draw.round(g, W - 34, 12, 22, 22, 7, hc ? acc : 0xFFFFFF, hc ? 0.9f : 0.08f);
        Draw.textC(g, "x", W - 23, 19, 0xFFFFFF, 1f);
        hit(W - 34, 12, 22, 22, this::onClose);
        Draw.rect(g, 14, 46, W - 28, 1, 0xFFFFFF, 0.08f);
        Draw.rect(g, 14, 46, 130, 1, acc, 0.9f);

        // --- pasek boczny ---
        float ty = 58 + tab * 32;
        if (tabY < 0) tabY = ty;
        tabY = Anim.approach(tabY, ty, 16f);
        Draw.glow(g, 12, Math.round(tabY), 122, 28, 8, acc, 0.22f, 5);
        Draw.round(g, 12, Math.round(tabY), 122, 28, 8, acc, 0.92f);
        for (int i = 0; i < TABS.length; i++) {
            int y = 58 + i * 32;
            boolean act = tab == i, hov = in(12, y, 122, 28);
            if (!act && hov) Draw.round(g, 12, y, 122, 28, 8, 0xFFFFFF, 0.07f);
            Draw.round(g, 22, y + 10, 8, 8, 3, 0xFFFFFF, act ? 1f : 0.35f);
            Draw.text(g, TABS[i], 38, y + 10, act ? 0xFFFFFF : col(4), act ? 1f : (hov ? 0.9f : 0.65f));
            final int idx = i;
            hit(12, y, 122, 28, () -> { tab = idx; scroll = 0; listening = null; });
        }
        label("Menu: [" + keyName(c.keyOpen) + "]", 16, H - 38, 0.4f);
        AbstractClientPlayer tp = Targets.targetPlayer();
        label("Cel: " + (tp != null ? tp.getName().getString() : "brak"), 16, H - 26, 0.55f);

        // --- zawartość ---
        Draw.round(g, CX - 6, CY - 6, CW + 12, CH + 12, 10, 0x000000, 0.20f);
        Minecraft mc = Minecraft.getInstance();
        switch (tab) {
            case 0 -> tabPlayers(mc);
            case 1 -> tabAttack(mc);
            case 2 -> tabHistory();
            case 3 -> tabSettings();
            case 4 -> tabThemes();
            default -> tabProfile(mc);
        }
    }

    private void logo(int x, int y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(scale, scale, 1f);
        String s = "NOVABOMBA";
        int cx = 0;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            int rgb = Draw.mix(col(1), 0xFFFFFF, i / (float) (s.length() - 1) * 0.55f);
            Draw.textB(g, ch, cx + 1, 1, col(1), 0.35f);
            Draw.textB(g, ch, cx, 0, rgb, 1f);
            cx += Draw.wB(ch) + 1;
        }
        g.pose().popPose();
    }

    // ------------------------------------------------------------ kontrolki
    private void button(int x, int y, int w, int h, String s, boolean accent, Runnable r) {
        boolean hov = in(x, y, w, h);
        if (accent) {
            Draw.glow(g, x, y, w, h, 6, col(1), hov ? 0.30f : 0.16f, 4);
            Draw.round(g, x, y, w, h, 6, col(1), hov ? 1f : 0.88f);
        } else {
            Draw.round(g, x, y, w, h, 6, 0xFFFFFF, hov ? 0.15f : 0.07f);
            Draw.outline(g, x, y, w, h, 6, 0xFFFFFF, 0.12f);
        }
        Draw.textC(g, s, x + w / 2, y + (h - 8) / 2, accent ? 0xFFFFFF : col(4), 1f);
        hit(x, y, w, h, r);
    }

    private void toggle(int x, int y, int w, String s, boolean val, Consumer<Boolean> set) {
        boolean hov = in(x, y, w, 16);
        label(s, x, y + 4, hov ? 1f : 0.82f);
        float k = animV("t" + x + "_" + y + s, val ? 1f : 0f, 16f);
        int tx = x + w - 26;
        Draw.round(g, tx, y + 2, 24, 12, 6, 0xFFFFFF, 0.14f);
        Draw.round(g, tx, y + 2, 24, 12, 6, col(1), 0.95f * k);
        Draw.round(g, tx + 2 + Math.round(12 * k), y + 4, 8, 8, 4, 0xFFFFFF, 1f);
        hit(x, y, w, 16, () -> { set.accept(!val); Config.dirty(); });
    }

    private void slider(int x, int y, int w, String s, float min, float max, float step, float val,
                        Consumer<Float> set, String fmt) {
        label(s, x, y, 0.85f);
        String v = Draw.f(fmt, val);
        Draw.text(g, v, x + w - Draw.w(v), y, col(1), 1f);
        int ty = y + 13;
        float t = Mth.clamp((val - min) / (max - min), 0f, 1f);
        Draw.round(g, x, ty, w, 4, 2, 0xFFFFFF, 0.14f);
        Draw.round(g, x, ty, Math.max(4, Math.round(w * t)), 4, 2, col(1), 0.95f);
        Draw.round(g, x + Math.round((w - 8) * t), ty - 2, 8, 8, 4, 0xFFFFFF, 1f);
        hitDrag(x, ty - 5, w, 14, lx -> {
            float tt = (float) Mth.clamp((lx - x - 4) / (double) (w - 8), 0.0, 1.0);
            float nv = Math.round((min + tt * (max - min)) / step) * step;
            set.accept(Mth.clamp(nv, min, max));
            Config.dirty();
        });
    }

    private void keybind(int x, int y, int w, String s, String id, int key) {
        label(s, x, y + 5, 0.85f);
        boolean lis = id.equals(listening);
        String t = lis ? "[ naciśnij klawisz ]" : keyName(key);
        int bw = 96, bx = x + w - bw;
        Draw.round(g, bx, y, bw, 17, 5, lis ? col(1) : 0xFFFFFF, lis ? 0.5f : (in(bx, y, bw, 17) ? 0.15f : 0.08f));
        Draw.outline(g, bx, y, bw, 17, 5, lis ? col(1) : 0xFFFFFF, lis ? 0.9f : 0.12f);
        Draw.textC(g, t, bx + bw / 2, y + 5, col(4), 1f);
        hit(bx, y, bw, 17, () -> listening = lis ? null : id);
    }

    static String keyName(int k) {
        if (k <= 0) return "BRAK";
        return InputConstants.Type.KEYSYM.getOrCreate(k).getDisplayName().getString().toUpperCase();
    }

    private void scrollbar(int total, int rows, int top, int height) {
        if (total <= rows) return;
        int bh = Math.max(14, height * rows / total);
        int by = top + (height - bh) * scroll / Math.max(1, total - rows);
        Draw.round(g, CX + CW + 1, top, 3, height, 1, 0xFFFFFF, 0.08f);
        Draw.round(g, CX + CW + 1, by, 3, bh, 1, col(1), 0.9f);
    }

    // ------------------------------------------------------------ zakładka: GRACZE
    private void tabPlayers(Minecraft mc) {
        List<AbstractClientPlayer> list = Targets.nearby;
        label("W promieniu 40 m: " + list.size() + "    Podświetleni: " + Targets.marked.size(), CX, CY + 3, 0.8f);
        button(CX + CW - 100, CY - 1, 100, 16, "Wyczyść wszystko", false, Targets::clearAll);
        int top = CY + 22, rows = (CH - 22 - 14) / 26;
        scroll = Mth.clamp(scroll, 0, Math.max(0, list.size() - rows));
        if (list.isEmpty()) Draw.textC(g, "Brak graczy w pobliżu", CX + CW / 2, top + 50, col(4), 0.5f);
        for (int i = 0; i < rows && i + scroll < list.size(); i++) {
            AbstractClientPlayer p = list.get(i + scroll);
            int y = top + i * 26, w = CW - 6, h = 24;
            UUID id = p.getUUID();
            boolean isT = Targets.isTarget(p), isM = Targets.marked.contains(id), hov = in(CX, y, w, h);
            Draw.round(g, CX, y, w, h, 7, isT ? col(1) : 0xFFFFFF, isT ? 0.28f : (isM ? 0.14f : (hov ? 0.10f : 0.05f)));
            if (isT) Draw.outline(g, CX, y, w, h, 7, col(1), 0.9f);
            else if (isM) Draw.outline(g, CX, y, w, h, 7, col(1), 0.45f);

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, Draw.mul);
            PlayerFaceRenderer.draw(g, p.getSkinTextureLocation(), CX + 4, y + 3, 18);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            float hp = p.getHealth(), mhp = Math.max(1f, p.getMaxHealth()), fr = Mth.clamp(hp / mhp, 0f, 1f);
            if (isT) Draw.textB(g, p.getName().getString(), CX + 28, y + 3, col(4), 1f);
            else Draw.text(g, p.getName().getString(), CX + 28, y + 3, col(4), 0.95f);
            Draw.round(g, CX + 28, y + 15, 96, 4, 2, 0xFFFFFF, 0.12f);
            Draw.round(g, CX + 28, y + 15, Math.max(2, Math.round(96 * fr)), 4, 2,
                    Draw.mix(col(5), 0xFF2020, fr < 0.3f ? (0.3f - fr) / 0.3f * 0.6f : 0f), 1f);
            Draw.text(g, Draw.f("%.1f/%.1f HP", hp, mhp), CX + 132, y + 8, col(4), 0.7f);
            Draw.textR(g, Draw.f("%.1f m", mc.player.distanceTo(p)), CX + w - 70, y + 8, col(4), 0.9f);
            if (isT || isM) {
                Draw.round(g, CX + w - 62, y + 5, 56, 14, 7, col(1), isT ? 0.95f : 0.45f);
                Draw.textC(g, isT ? "CEL" : "PODŚW.", CX + w - 34, y + 8, 0xFFFFFF, 1f);
            }
            hitR(CX, y, w, h, () -> Targets.setTarget(id), () -> Targets.toggleMark(id));
        }
        scrollbar(list.size(), rows, top, rows * 26);
        label("LPM - ustaw cel   |   PPM - podświetl / odznacz (wielu graczy)   |   kółko - przewijanie", CX, CY + CH - 10, 0.4f);
    }

    // ------------------------------------------------------------ zakładka: ATAK
    private void tabAttack(Minecraft mc) {
        int x = CX, w = 190, y = CY;
        toggle(x, y, w, "AutoAttack", c.autoAttack, v -> c.autoAttack = v); y += 22;
        toggle(x, y, w, "MultiTarget", c.multiTarget, v -> c.multiTarget = v); y += 22;
        toggle(x, y, w, "Priorytet najbliższego", c.nearestPriority, v -> c.nearestPriority = v); y += 22;
        toggle(x, y, w, "Cooldown vanilla (1.9+)", c.vanillaCooldown, v -> c.vanillaCooldown = v); y += 32;
        slider(x, y, w, "Zasięg ataku", 1f, 6f, 0.5f, c.attackRange, v -> c.attackRange = v, "%.1f bl."); y += 36;
        slider(x, y, w, "Cooldown ataku", 0f, 2000f, 50f, c.attackCooldownMs, v -> c.attackCooldownMs = Math.round(v), "%.0f ms"); y += 36;
        slider(x, y, w, "Czas namierzenia (0 = bez limitu)", 0f, 120f, 5f, c.lockSeconds, v -> c.lockSeconds = Math.round(v), "%.0f s");

        int rx = CX + 212, rw = CW - 212;
        Draw.round(g, rx, CY, rw, 150, 9, 0xFFFFFF, 0.05f);
        Draw.outline(g, rx, CY, rw, 150, 9, 0xFFFFFF, 0.1f);
        label("WYBRANY CEL", rx + 10, CY + 8, 0.5f);
        AbstractClientPlayer t = Targets.targetPlayer();
        if (t != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, Draw.mul);
            PlayerFaceRenderer.draw(g, t.getSkinTextureLocation(), rx + 10, CY + 24, 32);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            Draw.textB(g, t.getName().getString(), rx + 50, CY + 26, col(4), 1f);
            double r = Combat.reach(mc, t);
            Draw.text(g, Draw.f("%.1f m  |  %.1f HP", mc.player.distanceTo(t), t.getHealth()), rx + 50, CY + 40, col(4), 0.7f);
            boolean ok = r <= c.attackRange;
            Draw.text(g, ok ? "W ZASIĘGU" : "POZA ZASIĘGIEM", rx + 10, CY + 66, ok ? 0x4DFF88 : 0xFF4D5E, 1f);
        } else Draw.text(g, "Kliknij gracza w zakładce Gracze", rx + 10, CY + 30, col(4), 0.5f);
        button(rx + 10, CY + 84, rw - 20, 34, "ATAKUJ CEL", true, Combat::attackSelected);
        Draw.textC(g, Combat.status, rx + rw / 2, CY + 128, col(4), 0.7f);
        label("Skróty: atak [" + keyName(c.keyAttack) + "]  AutoAttack [" + keyName(c.keyAuto) + "]", rx, CY + 160, 0.45f);
        label("Zasięg liczony do hitboxa gracza.", rx, CY + 174, 0.35f);
    }

    // ------------------------------------------------------------ zakładka: HISTORIA
    private void tabHistory() {
        List<Stats.Entry> hs = Stats.history;
        label("Zapisanych ataków: " + hs.size(), CX, CY + 3, 0.8f);
        button(CX + CW - 110, CY - 1, 110, 16, "Wyczyść historię", false, Stats::clearHistory);
        Draw.round(g, CX, CY + 20, CW - 6, 14, 4, 0xFFFFFF, 0.07f);
        label("Gracz", CX + 8, CY + 23, 0.6f);
        label("Data i godzina", CX + 120, CY + 23, 0.6f);
        label("Zadane", CX + 290, CY + 23, 0.6f);
        label("Otrzymane", CX + 340, CY + 23, 0.6f);
        int top = CY + 38, rows = (CH - 42) / 20;
        scroll = Mth.clamp(scroll, 0, Math.max(0, hs.size() - rows));
        if (hs.isEmpty()) Draw.textC(g, "Brak zapisanych ataków", CX + CW / 2, top + 60, col(4), 0.5f);
        for (int i = 0; i < rows && i + scroll < hs.size(); i++) {
            Stats.Entry e = hs.get(i + scroll);
            int y = top + i * 20;
            Draw.round(g, CX, y, CW - 6, 18, 5, 0xFFFFFF, i % 2 == 0 ? 0.05f : 0.02f);
            Draw.text(g, e.nick, CX + 8, y + 5, col(4), 1f);
            Draw.text(g, Stats.date(e.time), CX + 120, y + 5, col(4), 0.7f);
            Draw.text(g, Draw.f("%.1f", e.dealt), CX + 290, y + 5, col(1), 1f);
            Draw.text(g, Draw.f("%.1f", e.received), CX + 340, y + 5, 0xFF9A9A, 1f);
        }
        scrollbar(hs.size(), rows, top, rows * 20);
    }

    // ------------------------------------------------------------ zakładka: USTAWIENIA
    private void tabSettings() {
        int x = CX, w = 190, y = CY, s = 17;
        label("MODUŁY", x, y - 4, 0.45f); y += 8;
        toggle(x, y, w, "Moduł Gracze / wizualny", c.modPlayers, v -> c.modPlayers = v); y += s;
        toggle(x, y, w, "Moduł Atak", c.modAttack, v -> c.modAttack = v); y += s;
        toggle(x, y, w, "Moduł Historia", c.modHistory, v -> c.modHistory = v); y += s;
        toggle(x, y, w, "Tracery", c.tracer, v -> c.tracer = v); y += s;
        toggle(x, y, w, "ESP Box", c.espBox, v -> c.espBox = v); y += s;
        toggle(x, y, w, "NameTag", c.nameTag, v -> c.nameTag = v); y += s;
        toggle(x, y, w, "Pasek HP", c.hpBar, v -> c.hpBar = v); y += s;
        toggle(x, y, w, "Glow", c.glow, v -> c.glow = v); y += s;
        toggle(x, y, w, "Widget celu", c.targetWidget, v -> c.targetWidget = v); y += s;
        toggle(x, y, w, "  Widget: odległość", c.widgetDistance, v -> c.widgetDistance = v); y += s;
        toggle(x, y, w, "  Widget: ping celu", c.widgetPing, v -> c.widgetPing = v); y += s;
        toggle(x, y, w, "  Widget: HP / max HP", c.widgetHp, v -> c.widgetHp = v); y += s;
        toggle(x, y, w, "Pokaż wszystkich w 40 m", c.showAll, v -> c.showAll = v); y += s;
        toggle(x, y, w, "Rozmycie tła (blur)", c.blur, v -> c.blur = v); y += s;
        toggle(x, y, w, "Animacje", c.animations, v -> c.animations = v);

        int rx = CX + 206, rw = CW - 206;
        y = CY;
        slider(rx, y, rw, "Rozmiar GUI", 0.7f, 1.5f, 0.05f, c.guiScale, v -> c.guiScale = v, "%.2fx"); y += 28;
        slider(rx, y, rw, "Pozycja GUI X", -300f, 300f, 5f, c.guiX, v -> c.guiX = Math.round(v), "%.0f"); y += 28;
        slider(rx, y, rw, "Pozycja GUI Y", -200f, 200f, 5f, c.guiY, v -> c.guiY = Math.round(v), "%.0f"); y += 28;
        slider(rx, y, rw, "Szybkość animacji", 0.25f, 3f, 0.25f, c.animSpeed, v -> c.animSpeed = v, "%.2fx"); y += 34;
        label("SKRÓTY KLAWISZOWE", rx, y, 0.45f);
        button(rx + rw - 58, y - 4, 58, 15, "Reset GUI", false, () -> { c.guiX = 0; c.guiY = 0; c.guiScale = 1f; Config.dirty(); });
        y += 14;
        keybind(rx, y, rw, "Otwórz menu", "open", c.keyOpen); y += 22;
        keybind(rx, y, rw, "AutoAttack", "auto", c.keyAuto); y += 22;
        keybind(rx, y, rw, "Atakuj cel", "attack", c.keyAttack); y += 22;
        keybind(rx, y, rw, "Wyczyść cel", "clear", c.keyClear); y += 22;
        keybind(rx, y, rw, "ESP wł./wył.", "esp", c.keyEsp);
        label("DEL / BACKSPACE = usuń skrót", rx, y + 24, 0.35f);
    }

    // ------------------------------------------------------------ zakładka: MOTYWY
    private void tabThemes() {
        for (int i = 0; i < Theme.NAMES.length; i++) {
            final String n = Theme.NAMES[i];
            int bx = CX + i * 102;
            boolean act = c.theme.equals(n);
            button(bx, CY, 96, 22, n, act, () -> Theme.apply(n));
        }
        label("Zmiana dowolnego koloru przełącza motyw na Custom.", CX, CY + 28, 0.4f);
        for (int i = 0; i < 6; i++) {
            int y = CY + 44 + i * 26;
            final int idx = i;
            Draw.round(g, CX, y - 3, CW - 6, 24, 6, 0xFFFFFF, 0.04f);
            label(Theme.COLOR_LABELS[i], CX + 6, y + 4, 0.9f);
            int rgb = c.colors[i];
            Draw.round(g, CX + 98, y, 18, 18, 5, rgb, 1f);
            Draw.outline(g, CX + 98, y, 18, 18, 5, 0xFFFFFF, 0.4f);
            for (int ch = 0; ch < 3; ch++) {
                final int shift = 16 - 8 * ch;
                int sx = CX + 124 + ch * 78, sw = 70;
                int val = (rgb >> shift) & 255;
                int chCol = ch == 0 ? 0xFF4D4D : (ch == 1 ? 0x4DFF6A : 0x4D8DFF);
                float t = val / 255f;
                Draw.round(g, sx, y + 7, sw, 4, 2, chCol, 0.25f);
                Draw.round(g, sx, y + 7, Math.max(4, Math.round(sw * t)), 4, 2, chCol, 0.95f);
                Draw.round(g, sx + Math.round((sw - 8) * t), y + 5, 8, 8, 4, 0xFFFFFF, 1f);
                hitDrag(sx, y + 2, sw, 14, lx -> {
                    int nv = Math.round(Mth.clamp((float) ((lx - sx - 4) / (sw - 8)), 0f, 1f) * 255f);
                    int cur = c.colors[idx];
                    Config.setColor(idx, (cur & ~(255 << shift)) | (nv << shift));
                });
            }
            Draw.text(g, Draw.hex(rgb), CX + 360, y + 5, col(4), 0.6f);
        }
        slider(CX, CY + 44 + 6 * 26 + 6, CW - 10, "Przezroczystość GUI (efekt Glass)", 0.2f, 1f, 0.01f,
                c.panelAlpha, Config::setPanelAlpha, "%.2f");
    }

    // ------------------------------------------------------------ zakładka: PROFIL
    private void tabProfile(Minecraft mc) {
        ServerData sd = mc.getCurrentServer();
        String server = sd != null ? sd.ip : (mc.hasSingleplayerServer() ? "Singleplayer" : "Offline");
        PlayerInfo pi = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(mc.player.getUUID()) : null;
        int ping = pi != null ? pi.getLatency() : 0;
        String kd = c.deaths == 0 ? String.valueOf(c.kills) : Draw.f("%.2f", c.kills / (float) c.deaths);
        String[][] cards = {
                {"Nick gracza", mc.player.getName().getString()}, {"Serwer", server},
                {"FPS", String.valueOf(Stats.fps)}, {"Ping", ping + " ms"},
                {"Zabójstwa", String.valueOf(c.kills)}, {"Śmierci", String.valueOf(c.deaths)},
                {"K/D", kd}, {"Czas gry (sesja)", hms(Stats.sessionTicks / 20)},
                {"Czas gry (łącznie)", hms(c.playSeconds)}};
        for (int i = 0; i < cards.length; i++) {
            int x = CX + (i % 2) * 204, y = CY + (i / 2) * 52, w = 196, h = 44;
            Draw.round(g, x, y, w, h, 9, 0xFFFFFF, 0.06f);
            Draw.outline(g, x, y, w, h, 9, 0xFFFFFF, 0.10f);
            Draw.round(g, x + 6, y + 10, 3, h - 20, 1, col(1), 1f);
            int tx = x + 16;
            if (i == 0) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1f, 1f, 1f, Draw.mul);
                PlayerFaceRenderer.draw(g, mc.player.getSkinTextureLocation(), x + 16, y + 8, 28);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                tx = x + 52;
            }
            label(cards[i][0], tx, y + 8, 0.5f);
            g.pose().pushPose();
            g.pose().translate(tx, y + 22, 0f);
            g.pose().scale(1.3f, 1.3f, 1f);
            Draw.textB(g, cards[i][1], 0, 0, col(1), 1f);
            g.pose().popPose();
        }
    }

    private static String hms(long s) {
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? Draw.f("%dh %02dm %02ds", h, m, sec) : Draw.f("%02d:%02d", m, sec);
    }

    // ------------------------------------------------------------ wejście
    @Override public boolean mouseClicked(double mxs, double mys, int button) {
        if (closing) return false;
        listening = null;
        double lx = (mxs - ox) / sc, ly = (mys - oy) / sc;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (lx < h.x || lx >= h.x + h.w || ly < h.y || ly >= h.y + h.h) continue;
            if (button == 0 && h.drag != null) { active = h; h.drag.accept(lx); return true; }
            if (button == 0 && h.left != null) { h.left.run(); return true; }
            if (button == 1 && h.right != null) { h.right.run(); return true; }
        }
        return true;
    }

    @Override public boolean mouseDragged(double mxs, double mys, int button, double dx, double dy) {
        if (active != null) { active.drag.accept((mxs - ox) / sc); return true; }
        return false;
    }

    @Override public boolean mouseReleased(double mxs, double mys, int button) { active = null; return true; }

    @Override public boolean mouseScrolled(double mxs, double mys, double delta) {
        scroll -= (int) Math.signum(delta);
        if (scroll < 0) scroll = 0;
        return true;
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                int k = (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) ? -1 : key;
                switch (listening) {
                    case "open" -> c.keyOpen = k;
                    case "auto" -> c.keyAuto = k;
                    case "attack" -> c.keyAttack = k;
                    case "clear" -> c.keyClear = k;
                    case "esp" -> c.keyEsp = k;
                    default -> { }
                }
                Config.dirty();
            }
            listening = null;
            return true;
        }
        if (key == c.keyOpen) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
