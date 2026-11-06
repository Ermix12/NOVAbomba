package pl.novabomba;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

/** FPS, zabójstwa/śmierci, czas gry oraz historia ataków (zapisywana do config/novabomba_history.json). */
public final class Stats {
    public static class Entry {
        public String nick;
        public long time;
        public float dealt, received;
        transient int age = 100000;
        transient UUID id;
    }

    public static final List<Entry> history = new ArrayList<>(); // najnowsze na początku
    public static int fps;
    public static long sessionTicks;

    private static int frames;
    private static long fpsT = System.nanoTime();
    private static final Map<UUID, Float> lastHp = new HashMap<>();
    private static final Set<UUID> counted = new HashSet<>();
    private static float myLastHp = -1f;
    private static boolean wasDead, histDirty, histLoaded;
    private static int histTicks;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public static String date(long t) { return FMT.format(new Date(t)); }

    public static void frame() {
        frames++;
        long n = System.nanoTime();
        if (n - fpsT >= 1_000_000_000L) { fps = frames; frames = 0; fpsT = n; }
    }

    public static void logAttack(Player t) {
        if (!Config.get().modHistory) return;
        Entry e = new Entry();
        e.nick = t.getName().getString();
        e.time = System.currentTimeMillis();
        e.id = t.getUUID();
        e.age = 0;
        history.add(0, e);
        while (history.size() > 500) history.remove(history.size() - 1);
        histDirty = true;
    }

    public static void clearHistory() { history.clear(); histDirty = true; }

    public static void tick(Minecraft mc) {
        if (!histLoaded) loadHistory();
        Config c = Config.get();
        sessionTicks++;
        if (sessionTicks % 20 == 0) { c.playSeconds++; if (sessionTicks % 200 == 0) Config.dirty(); }

        for (int i = 0, n = Math.min(40, history.size()); i < n; i++) history.get(i).age++;

        // otrzymane obrażenia – przypisane do ostatniego ataku z ostatnich 2 s
        float hp = mc.player.getHealth();
        if (myLastHp > 0 && myLastHp - hp > 0.01f && !history.isEmpty() && history.get(0).age <= 40) {
            history.get(0).received += myLastHp - hp;
            histDirty = true;
        }
        myLastHp = hp;

        boolean dead = mc.player.isDeadOrDying();
        if (dead && !wasDead) { c.deaths++; Config.dirty(); }
        wasDead = dead;

        // zadane obrażenia i zabójstwa
        for (AbstractClientPlayer p : Targets.nearby) {
            UUID id = p.getUUID();
            float h = p.getHealth();
            Float prev = lastHp.get(id);
            if (prev != null && prev - h > 0.01f) {
                for (int i = 0, n = Math.min(40, history.size()); i < n; i++) {
                    Entry e = history.get(i);
                    if (e.id != null && e.id.equals(id) && e.age <= 40) { e.dealt += prev - h; histDirty = true; break; }
                }
            }
            lastHp.put(id, h);
            if (p.isDeadOrDying() || h <= 0f) {
                if (!counted.contains(id) && recentAttack(id)) { c.kills++; counted.add(id); Config.dirty(); }
            } else counted.remove(id);
        }
    }

    private static boolean recentAttack(UUID id) {
        for (int i = 0, n = Math.min(40, history.size()); i < n; i++) {
            Entry e = history.get(i);
            if (e.id != null && e.id.equals(id) && e.age <= 100) return true;
        }
        return false;
    }

    // ---- zapis historii ----
    public static void tickSaveHistory() {
        if (histDirty && ++histTicks >= 100) { saveHistory(); histDirty = false; histTicks = 0; }
    }

    public static void saveHistory() {
        try {
            Path p = Config.file("novabomba_history.json");
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(history));
        } catch (Exception e) { NovaBomba.LOG("Zapis historii nieudany: " + e); }
    }

    public static void loadHistory() {
        histLoaded = true;
        try {
            Path p = Config.file("novabomba_history.json");
            if (Files.exists(p)) {
                List<Entry> l = GSON.fromJson(Files.readString(p), new TypeToken<List<Entry>>() {}.getType());
                if (l != null) history.addAll(l);
            }
        } catch (Exception e) { NovaBomba.LOG("Odczyt historii nieudany: " + e); }
    }
}
