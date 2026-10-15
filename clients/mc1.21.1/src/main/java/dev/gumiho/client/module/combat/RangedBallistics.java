package dev.gumiho.client.module.combat;

/** Bounded, low-arc intercept solver for vanilla airborne arrows/tridents (.99 drag, .05 gravity). */
public final class RangedBallistics {
    private RangedBallistics() { }
    public record Vector(double x, double y, double z) {
        public Vector add(Vector b) { return new Vector(x + b.x, y + b.y, z + b.z); }
        public Vector scale(double s) { return new Vector(x * s, y * s, z * s); }
        public double length() { return Math.sqrt(x * x + y * y + z * z); }
        public boolean finite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    }
    public record Solution(Vector launch, double ticks) { }
    // Integrates full ticks, then a linear fraction of the next tick, matching position-before-gravity.
    public static double dragSum(double t) {
        int whole = (int)t;
        return (1 - Math.pow(0.99, whole)) / 0.01 + (t - whole) * Math.pow(0.99, whole);
    }
    public static double gravitySum(double t) {
        int whole = (int)t;
        double sum = (1 - Math.pow(0.99, whole)) / 0.01;
        return (whole - sum) / 0.01 + (t - whole) * sum;
    }
    public static Solution solve(Vector offset, Vector targetVelocity, Vector inherited, double speed) {
        if (offset == null || targetVelocity == null || inherited == null || !offset.finite()
                || !targetVelocity.finite() || !inherited.finite() || !Double.isFinite(speed)
                || speed < 0.3 || speed > 4 || offset.length() < 0.01
                || targetVelocity.length() > 4 || inherited.length() > 4) return null;
        double previous = 0.001;
        if (required(offset, targetVelocity, inherited, previous).length() <= speed) return null;
        for (double t = 0.25; t <= 80; t += 0.25) {
            if (required(offset, targetVelocity, inherited, t).length() <= speed) {
                double low = previous, high = t;
                for (int i = 0; i < 28; i++) {
                    double mid = (low + high) / 2;
                    if (required(offset, targetVelocity, inherited, mid).length() > speed) low = mid;
                    else high = mid;
                }
                double time = (low + high) / 2;
                Vector launch = required(offset, targetVelocity, inherited, time);
                return launch.finite() ? new Solution(launch, time) : null;
            }
            previous = t;
        }
        return null; // Outside the reachable low-arc envelope/horizon.
    }
    private static Vector required(Vector offset, Vector velocity, Vector inherited, double ticks) {
        double sum = dragSum(ticks);
        Vector destination = offset.add(velocity.scale(ticks));
        return new Vector(destination.x / sum - inherited.x,
                (destination.y + 0.05 * gravitySum(ticks)) / sum - inherited.y,
                destination.z / sum - inherited.z);
    }
    /** Exactly one controller drives aim even when both target-category toggles are on. */
    public static boolean drives(boolean playerModule, boolean mobEnabled, boolean playerEnabled) {
        return playerModule ? playerEnabled && !mobEnabled : mobEnabled;
    }
}
