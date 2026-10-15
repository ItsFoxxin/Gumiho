package dev.gumiho.client.module.combat;

/** Standard-air approximation; no position/velocity edits and no fall-damage promises. */
public final class MaceFlight {
    private boolean initialized, wasGround;
    private double previousY;
    public boolean newCycle(boolean ground, double verticalSpeed) {
        boolean reset = !initialized || ground != wasGround || !ground && previousY < -0.08 && verticalSpeed > 0.15;
        initialized = true; wasGround = ground; previousY = verticalSpeed;
        return reset;
    }
    public void reset() { initialized = false; }
    public record Landing(double x, double z, double ticks) { }
    public static Landing atHeight(double x, double y, double z, double vx, double vy, double vz, double floor) {
        if (!Double.isFinite(x+y+z+vx+vy+vz+floor) || y < floor || Math.abs(vy) > 8) return null;
        for (int tick = 0; tick < 80; tick++) {
            double nx = x + vx, ny = y + vy, nz = z + vz;
            if (vy < 0 && ny <= floor) {
                double fraction = Math.max(0, Math.min(1, (y - floor) / -vy));
                return new Landing(x + vx * fraction, z + vz * fraction, tick + fraction);
            }
            x = nx; y = ny; z = nz; vx *= 0.91; vz *= 0.91; vy = (vy - 0.08) * 0.98;
        }
        return null;
    }
    public static double score(double error, double ticks, double radius, boolean ground, double distance, double reach) {
        if (!Double.isFinite(error+ticks+radius+distance+reach) || error < 0 || ticks < 0
                || (ground ? distance > reach : error > radius)) return Double.POSITIVE_INFINITY;
        return ground ? distance : error * 5 + ticks * 0.025;
    }
    public static double correctionRadius(double ticks, double configured) {
        return Math.min(configured, 0.65 + Math.max(0, ticks - 3) * 0.08);
    }
    public record Steering(float forward, float left) { }
    public static Steering steering(double dx, double dz, double yaw) {
        double distance = Math.hypot(dx, dz);
        if (!Double.isFinite(distance+yaw) || distance < 0.25) return new Steering(0, 0);
        double angle = Math.toRadians(yaw), strength = Math.min(0.65, distance / 3);
        return new Steering((float)((-Math.sin(angle)*dx+Math.cos(angle)*dz)/distance*strength),
                (float)((Math.cos(angle)*dx+Math.sin(angle)*dz)/distance*strength));
    }
}
