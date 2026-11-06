package pl.novabomba;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Atak ręczny + AutoAttack / MultiTarget / priorytet najbliższego / cooldown. */
public final class Combat {
    public static String status = "Gotowy";
    private static long lastAttack;

    /** odległość od oczu gracza do najbliższego punktu hitboxa */
    public static double reach(Minecraft mc, Entity e) {
        Vec3 eye = mc.player.getEyePosition();
        AABB b = e.getBoundingBox();
        double dx = Math.max(Math.max(b.minX - eye.x, 0), eye.x - b.maxX);
        double dy = Math.max(Math.max(b.minY - eye.y, 0), eye.y - b.maxY);
        double dz = Math.max(Math.max(b.minZ - eye.z, 0), eye.z - b.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean ready(Minecraft mc, Config c) {
        if (System.currentTimeMillis() - lastAttack < c.attackCooldownMs) return false;
        return !c.vanillaCooldown || mc.player.getAttackStrengthScale(0.5f) >= 0.9f;
    }

    private static List<AbstractClientPlayer> candidates(Minecraft mc, Config c) {
        List<AbstractClientPlayer> l = new ArrayList<>();
        for (AbstractClientPlayer p : Targets.nearby)
            if (Targets.isMarked(p) && !p.isDeadOrDying() && reach(mc, p) <= c.attackRange) l.add(p);
        if (c.nearestPriority) l.sort(Comparator.comparingDouble(p -> reach(mc, p)));
        else l.sort(Comparator.comparingInt((AbstractClientPlayer p) -> Targets.isTarget(p) ? 0 : 1)
                .thenComparingDouble(p -> reach(mc, p)));
        return l;
    }

    public static void tick(Minecraft mc) {
        Config c = Config.get();
        if (!c.autoAttack || !c.modAttack || mc.screen != null || mc.player.isDeadOrDying()) return;
        if (!ready(mc, c)) return;
        List<AbstractClientPlayer> l = candidates(mc, c);
        if (!l.isEmpty()) strike(mc, c, l);
    }

    private static void strike(Minecraft mc, Config c, List<AbstractClientPlayer> l) {
        List<AbstractClientPlayer> hit = c.multiTarget ? l : l.subList(0, 1);
        for (AbstractClientPlayer p : hit) {
            mc.gameMode.attack(mc.player, p);
            Stats.logAttack(p);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastAttack = System.currentTimeMillis();
        status = "Zaatakowano: " + hit.get(0).getName().getString() + (hit.size() > 1 ? " +" + (hit.size() - 1) : "");
    }

    /** Przycisk „Atakuj cel” / skrót klawiszowy */
    public static void attackSelected() {
        Minecraft mc = Minecraft.getInstance();
        Config c = Config.get();
        if (mc.player == null || mc.gameMode == null) return;
        if (!c.modAttack) { status = "Moduł Atak jest wyłączony"; return; }
        AbstractClientPlayer t = Targets.targetPlayer();
        if (t == null) { status = "Brak zaznaczonego celu"; return; }
        double r = reach(mc, t);
        if (r > c.attackRange) { status = Draw.f("Cel poza zasięgiem (%.1f m)", r); return; }
        strike(mc, c, Collections.singletonList(t));
    }
}
