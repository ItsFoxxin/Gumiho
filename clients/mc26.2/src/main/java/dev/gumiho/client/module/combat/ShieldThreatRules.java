package dev.gumiho.client.module.combat;

/** Defensive classification deliberately does not depend on offensive target or auto-attack toggles. */
public final class ShieldThreatRules {
    private ShieldThreatRules() { }
    public static boolean projectile(boolean arrow, int pierce, boolean noPhysics, boolean supportedOther) {
        return arrow ? pierce == 0 && !noPhysics : supportedOther;
    }
    public static boolean frontal(double dot) { return Double.isFinite(dot) && dot > 0.01; }
    public static boolean closeThreat(double distance, double range, boolean attacker, boolean visible, double facingDot) {
        return Double.isFinite(distance + range + facingDot) && distance >= 0 && distance <= Math.min(3.5, range)
                && attacker && visible && facingDot > 0.3;
    }
}
