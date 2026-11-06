package pl.novabomba;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

/** Gracze w promieniu 40 kratek, główny cel i podświetleni gracze. */
public final class Targets {
    public static final double RADIUS = 40.0;
    public static UUID target;
    public static final Set<UUID> marked = new LinkedHashSet<>();
    public static final List<AbstractClientPlayer> nearby = new ArrayList<>();
    private static long lockStart;

    public static void tick(Minecraft mc) {
        nearby.clear();
        for (AbstractClientPlayer p : mc.level.players()) {
            if (p == mc.player || p.isSpectator() || p.isRemoved()) continue;
            if (mc.player.distanceTo(p) <= RADIUS) nearby.add(p);
        }
        nearby.sort(Comparator.comparingDouble((AbstractClientPlayer p) -> mc.player.distanceToSqr(p)));
        marked.removeIf(id -> mc.level.getPlayerByUUID(id) == null);

        if (target != null) {
            boolean here = false;
            for (AbstractClientPlayer p : nearby) if (p.getUUID().equals(target)) { here = true; break; }
            if (!here) target = null;                         // zniknął / wyszedł poza 40 kratek
            else if (Config.get().lockSeconds > 0 && lockRemaining() <= 0) target = null; // koniec namierzania
        }
    }

    public static void setTarget(UUID id) {
        if (id.equals(target)) { target = null; return; }
        target = id;
        lockStart = System.currentTimeMillis();
    }

    public static void toggleMark(UUID id) { if (!marked.remove(id)) marked.add(id); }
    public static void clearAll() { target = null; marked.clear(); }

    public static boolean isTarget(net.minecraft.world.entity.Entity p) { return target != null && target.equals(p.getUUID()); }
    public static boolean isMarked(net.minecraft.world.entity.Entity p) { return isTarget(p) || marked.contains(p.getUUID()); }

    public static AbstractClientPlayer targetPlayer() {
        if (target == null) return null;
        for (AbstractClientPlayer p : nearby) if (p.getUUID().equals(target)) return p;
        return null;
    }

    /** pozostały czas namierzenia w s, -1 = bez limitu */
    public static float lockRemaining() {
        int s = Config.get().lockSeconds;
        if (s <= 0) return -1f;
        return s - (System.currentTimeMillis() - lockStart) / 1000f;
    }

    public static float lockElapsed() { return (System.currentTimeMillis() - lockStart) / 1000f; }
}
