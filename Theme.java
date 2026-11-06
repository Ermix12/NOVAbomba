package pl.novabomba;

/** Gotowe motywy. Kolory: 0=GUI 1=przyciski(akcent) 2=tracery 3=ESP 4=tekst 5=paski HP */
public final class Theme {
    public final String name;
    public final int[] colors;
    public final float alpha, shine;

    private Theme(String n, int[] c, float a, float s) { name = n; colors = c; alpha = a; shine = s; }

    public static final Theme MIDNIGHT = new Theme("Midnight",
            new int[]{0x0B0A12, 0xFF2E63, 0xFF3D71, 0xFF2E63, 0xF4F4F8, 0xFF4D6D}, 0.93f, 0.05f);
    public static final Theme GLASS = new Theme("Glass",
            new int[]{0x1B1626, 0xFF4D8D, 0xFF5C9A, 0xFF4D8D, 0xFFFFFF, 0xFF6FA5}, 0.55f, 0.12f);
    public static final Theme FROST = new Theme("Frost",
            new int[]{0x172233, 0xFF5470, 0xFF6B86, 0xFF5470, 0xEAF4FF, 0xFF7A93}, 0.78f, 0.16f);

    public static final String CUSTOM = "Custom";
    public static final String[] NAMES = {"Midnight", "Glass", "Frost", "Custom"};
    public static final int[] CUSTOM_DEFAULT = {0x120E1A, 0xE0245E, 0xFF2D55, 0xFF2D55, 0xFFFFFF, 0xFF3B6B};
    public static final String[] COLOR_LABELS = {"Kolor GUI", "Kolor przycisków", "Kolor tracerów",
            "Kolor ESP Boxów", "Kolor tekstu", "Kolor pasków HP"};

    private static Theme find(String n) {
        if (MIDNIGHT.name.equals(n)) return MIDNIGHT;
        if (GLASS.name.equals(n)) return GLASS;
        if (FROST.name.equals(n)) return FROST;
        return null;
    }

    public static float shine() {
        Theme t = find(Config.get().theme);
        return t != null ? t.shine : 0.08f;
    }

    public static void apply(String name) {
        Config c = Config.get();
        Theme t = find(name);
        if (t != null) {
            c.theme = t.name;
            c.colors = t.colors.clone();
            c.panelAlpha = t.alpha;
        } else {
            c.theme = CUSTOM;
            c.colors = c.custom.clone();
            c.panelAlpha = c.customAlpha;
        }
        Config.dirty();
    }
}
