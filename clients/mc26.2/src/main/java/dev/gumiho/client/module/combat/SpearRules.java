package dev.gumiho.client.module.combat;

/** Pure checks, kept separate from native item/component adapters. */
public final class SpearRules {
    public static boolean inBand(double distance, double minimum, double maximum) {
        return Double.isFinite(distance) && Double.isFinite(minimum) && Double.isFinite(maximum)
                && minimum >= 0 && maximum >= minimum && distance >= minimum && distance <= maximum;
    }
    public static boolean safeJab(int eligibleHits, int protectedHits, boolean enabled,
                                  boolean usingItem, float charge, boolean nativeReady) {
        return enabled && !usingItem && nativeReady && Float.isFinite(charge) && charge >= 1
                && eligibleHits > 0 && protectedHits == 0;
    }
    public static String thrustState(int used, int delay, int duration, double speed, double relativeSpeed,
                                     double minimumSpeed, double minimumRelativeSpeed) {
        if (used < 0 || delay < 0 || duration < 0 || !Double.isFinite(minimumSpeed)
                || !Double.isFinite(minimumRelativeSpeed)) return "UNKNOWN";
        if (used < delay) return "WINDUP";
        if (used - delay > duration) return "EXPIRED";
        if (!Double.isFinite(speed) || !Double.isFinite(relativeSpeed)
                || speed < minimumSpeed || relativeSpeed < minimumRelativeSpeed) return "TOO SLOW";
        return "READY";
    }
    private SpearRules() { }
}
