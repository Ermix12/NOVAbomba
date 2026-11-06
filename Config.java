package pl.novabomba;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Wszystkie ustawienia – zapisywane automatycznie do config/novabomba.json */
public final class Config {
    // ---- motyw ----
    public String theme = "Midnight";
    public int[] colors = Theme.MIDNIGHT.colors.clone();
    public int[] custom = Theme.CUSTOM_DEFAULT.clone();
    public float panelAlpha = Theme.MIDNIGHT.alpha;
    public float customAlpha = 0.85f;

    // ---- moduły ----
    public boolean modPlayers = true, modAttack = true, modHistory = true;
    public boolean tracer = true, espBox = true, nameTag = true, hpBar = true, glow = true;
    public boolean targetWidget = true, widgetDistance = true, widgetPing = true, widgetHp = true;
    public boolean showAll = false, blur = true, animations = true;

    // ---- atak ----
    public boolean autoAttack = false, multiTarget = false, nearestPriority = true, vanillaCooldown = true;
    public float attackRange = 3f;
    public int attackCooldownMs = 500;
    public int lockSeconds = 30; // 0 = bez limitu

    // ---- GUI ----
    public float guiScale = 1f, animSpeed = 1f;
    public int guiX = 0, guiY = 0;

    // ---- skróty (GLFW) ----
    public int keyOpen = GLFW.GLFW_KEY_RIGHT_SHIFT, keyAuto = GLFW.GLFW_KEY_R, keyAttack = GLFW.GLFW_KEY_G,
            keyClear = GLFW.GLFW_KEY_X, keyEsp = GLFW.GLFW_KEY_H;

    // ---- statystyki ----
    public int kills, deaths;
    public long playSeconds;

    // ---------------------------------------------------------------
    private static Config I;
    private static boolean isDirty, hookAdded;
    private static int dirtyTicks;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config get() { if (I == null) load(); return I; }

    public static Path file(String name) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(name);
    }

    public static void load() {
        Config c = null;
        try {
            Path p = file("novabomba.json");
            if (Files.exists(p)) c = GSON.fromJson(Files.readString(p), Config.class);
        } catch (Exception e) { NovaBomba.LOG("Nie można wczytać configu: " + e); }
        if (c == null) c = new Config();
        c.sanitize();
        I = c;
        if (!hookAdded) {
            hookAdded = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { save(); Stats.saveHistory(); }));
        }
    }

    private void sanitize() {
        if (colors == null || colors.length != 6) colors = Theme.MIDNIGHT.colors.clone();
        if (custom == null || custom.length != 6) custom = Theme.CUSTOM_DEFAULT.clone();
        attackRange = Mth.clamp(attackRange, 1f, 6f);
        guiScale = Mth.clamp(guiScale, 0.7f, 1.5f);
        animSpeed = Mth.clamp(animSpeed, 0.25f, 3f);
        panelAlpha = Mth.clamp(panelAlpha, 0.2f, 1f);
        attackCooldownMs = Mth.clamp(attackCooldownMs, 0, 2000);
        lockSeconds = Mth.clamp(lockSeconds, 0, 120);
        if (theme == null) theme = "Midnight";
    }

    public static void dirty() { isDirty = true; dirtyTicks = 0; }

    /** wołane co tick – zapis z opóźnieniem 1 s po ostatniej zmianie */
    public static void tickSave() {
        if (isDirty && ++dirtyTicks >= 20) { save(); isDirty = false; }
    }

    public static void save() {
        if (I == null) return;
        try {
            Path p = file("novabomba.json");
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(I));
        } catch (Exception e) { NovaBomba.LOG("Nie można zapisać configu: " + e); }
    }

    // pomocnicze – edycja koloru przełącza motyw na Custom
    public static void setColor(int idx, int rgb) {
        Config c = get();
        c.colors[idx] = rgb & 0xFFFFFF;
        c.theme = Theme.CUSTOM;
        c.custom = c.colors.clone();
        dirty();
    }

    public static void setPanelAlpha(float a) {
        Config c = get();
        c.panelAlpha = a;
        c.theme = Theme.CUSTOM;
        c.customAlpha = a;
        c.custom = c.colors.clone();
        dirty();
    }
}
