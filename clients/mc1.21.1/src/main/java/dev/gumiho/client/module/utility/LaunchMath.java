package dev.gumiho.client.module.utility;

/** Vanilla shootFromRotation direction (pitch offset affects Y before normalization). */
public final class LaunchMath {
    private LaunchMath() { }
    public record Motion(double x, double y, double z) { }
    public static Motion direction(double pitch, double yaw, double offset, double speed) {
        double p = Math.toRadians(pitch), y = Math.toRadians(yaw);
        double dx = -Math.sin(y) * Math.cos(p), dy = -Math.sin(Math.toRadians(pitch + offset)), dz = Math.cos(y) * Math.cos(p);
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(length) || length < 1e-8 || !Double.isFinite(speed) || speed <= 0) return null;
        return new Motion(dx * speed / length, dy * speed / length, dz * speed / length);
    }
}
