package dev.gumiho.client.module.utility;
import java.util.Locale;
/** Game-independent display math. */
public final class HudValues {
    public static String duration(int ticks) {
        if (ticks == -1) return "∞";
        long seconds = (Math.max(0L, ticks) + 19) / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
    public static int durability(int damage, int maximum) {
        if (maximum <= 0) return -1;
        return (int)Math.max(0, Math.min(100, 100L * (maximum - (long)damage) / maximum));
    }
    public static String heading(double yaw) {
        if (!Double.isFinite(yaw)) return "--";
        double normalized = ((yaw % 360) + 360) % 360;
        return new String[] {"S | 남", "SW | 남서", "W | 서", "NW | 북서", "N | 북", "NE | 북동", "E | 동", "SE | 남동"}
                [(int)Math.floor((normalized + 22.5) / 45) % 8];
    }
    public static int bearing(double yaw) {
        if (!Double.isFinite(yaw)) return 0;
        return (int)Math.round(((yaw + 180) % 360 + 360) % 360) % 360;
    }
    public static String cooldown(float fraction) {
        if (!Float.isFinite(fraction) || fraction < 0) return "--";
        if (fraction == 0) return "Ready | 준비";
        return (int)Math.ceil(Math.min(1, fraction)*100) + "% left | 남음";
    }
    private HudValues() { }
}
