package dev.gumiho.client.module.utility;

/** Pure 1.21.1 tick integration: position advances before drag/gravity/acceleration. */
public final class TrajectoryPhysics {
    private TrajectoryPhysics() { }
    public enum Kind {
        ARROW(0.05, 0.99, 0.6, 0xFFFFD56A), TRIDENT(0.05, 0.99, 0.99, 0xFF59DDD5),
        THROWABLE(0.03, 0.99, 0.8, 0xFF9BE59D), POTION(0.05, 0.99, 0.8, 0xFFD395FA),
        EXPERIENCE(0.07, 0.99, 0.8, 0xFFB0F050), FIREBALL(0, 0.95, 0.8, 0xFFFF7A18),
        DANGEROUS_SKULL(0, 0.73, 0.8, 0xFFEE779A), WIND(0, 1, 1, 0xFFB5EDFF), FIREWORK(0, 1, 1, 0xFFFF99DC);
        public final double gravity, airDrag, waterDrag;
        public final int color;
        Kind(double gravity, double airDrag, double waterDrag, int color) {
            this.gravity = gravity; this.airDrag = airDrag; this.waterDrag = waterDrag; this.color = color;
        }
        public boolean accelerating() { return this == FIREBALL || this == DANGEROUS_SKULL || this == WIND; }
    }
    public record State(double x, double y, double z, double vx, double vy, double vz) {
        public boolean finite() {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                    && Double.isFinite(vx) && Double.isFinite(vy) && Double.isFinite(vz);
        }
    }
    public static State step(State s, Kind kind, boolean water, boolean noGravity, double acceleration) {
        if (s == null || kind == null || !s.finite() || !Double.isFinite(acceleration)) return null;
        double drag = water ? kind.waterDrag : kind.airDrag;
        double speed = Math.sqrt(s.vx * s.vx + s.vy * s.vy + s.vz * s.vz);
        double factor = kind.accelerating() && speed > 1e-10 ? 1 + acceleration / speed : 1;
        double gravity = noGravity ? 0 : kind.gravity;
        State next = new State(s.x + s.vx, s.y + s.vy, s.z + s.vz,
                s.vx * factor * drag, s.vy * factor * drag - gravity, s.vz * factor * drag);
        return next.finite() ? next : null;
    }
}
