package pl.novabomba;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Wspólny rdzeń – Forge i Fabric wołają tylko te 3 metody. */
public final class NovaBomba {
    public static final String ID = "novabomba";
    private static final boolean[] prev = new boolean[5];

    public static void LOG(String s) { System.out.println("[NOVABOMBA] " + s); }

    public static void tick() {
        Config.tickSave();
        Stats.tickSaveHistory();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Stats.tick(mc);
        Targets.tick(mc);
        Combat.tick(mc);
        handleKeys(mc);
    }

    public static void renderHud(GuiGraphics g, float pt) { Hud.render(g, pt); }

    public static void renderWorld(PoseStack ps, Camera cam, float pt) { WorldRender.render(ps, cam, pt); }

    private static void handleKeys(Minecraft mc) {
        Config c = Config.get();
        int[] keys = {c.keyOpen, c.keyAuto, c.keyAttack, c.keyClear, c.keyEsp};
        long win = mc.getWindow().getWindow();
        for (int i = 0; i < keys.length; i++) {
            boolean down = keys[i] > 0 && mc.screen == null && InputConstants.isKeyDown(win, keys[i]);
            if (down && !prev[i]) fire(mc, c, i);
            prev[i] = down;
        }
    }

    private static void fire(Minecraft mc, Config c, int i) {
        switch (i) {
            case 0 -> mc.setScreen(new NovaScreen());
            case 1 -> { c.autoAttack = !c.autoAttack; Config.dirty(); msg(mc, "AutoAttack: " + (c.autoAttack ? "WŁ." : "WYŁ.")); }
            case 2 -> { Combat.attackSelected(); msg(mc, Combat.status); }
            case 3 -> { Targets.clearAll(); msg(mc, "Cel wyczyszczony"); }
            case 4 -> { c.espBox = !c.espBox; Config.dirty(); msg(mc, "ESP Box: " + (c.espBox ? "WŁ." : "WYŁ.")); }
            default -> { }
        }
    }

    private static void msg(Minecraft mc, String s) {
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("NOVABOMBA » " + s), true);
    }
}
