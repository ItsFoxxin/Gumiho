package dev.gumiho.client.module.combat;

/** Pure policy/math shared by combat code and the standalone regression tests. */
public final class CombatRules {
    private CombatRules() { }
    public enum Kind { ENEMY_PLAYER, TEAMMATE, NEUTRAL_PLAYER, HOSTILE, PASSIVE, INVALID }
    public static boolean allowed(Kind kind, boolean hostile, boolean enemy, boolean passive) {
        return switch (kind) {
            case ENEMY_PLAYER -> enemy;
            case HOSTILE -> hostile;
            case PASSIVE -> passive;
            default -> false;
        };
    }
    public static double score(String priority, double angle, double distance, double health,
                               boolean pinned, boolean preferWeak) {
        double score = switch (priority) {
            case "DISTANCE" -> distance;
            case "HEALTH" -> health + distance * 0.01D;
            default -> angle * 3.0D + distance;
        };
        if (preferWeak && !"HEALTH".equals(priority)) score += health * 0.75D;
        return pinned ? score - 10000.0D : score;
    }
    public static double bounded(double value, double fallback, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
    public static boolean smashReady(double fall, double velocityY, boolean gliding, double minimum) {
        return Double.isFinite(fall) && Double.isFinite(velocityY) && Double.isFinite(minimum)
                && fall > 1.5D && fall >= minimum && velocityY < 0.0D && !gliding;
    }
    /** Unenchanted fall bonus only, not final damage against an armored target. */
    public static double baseSmashBonus(double fall) {
        if (!Double.isFinite(fall) || fall <= 1.5D) return 0;
        if (fall <= 3) return fall * 4;
        if (fall <= 8) return 12 + (fall - 3) * 2;
        return 22 + fall - 8;
    }
}
