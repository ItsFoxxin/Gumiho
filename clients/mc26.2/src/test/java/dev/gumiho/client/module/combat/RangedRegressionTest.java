package dev.gumiho.client.module.combat;

import dev.gumiho.client.config.CombatConfigMigration;
import dev.gumiho.client.config.GumihoConfig;
import dev.gumiho.client.module.utility.TrajectoryPhysics;
import dev.gumiho.client.module.combat.RangedBallistics.Vector;

public final class RangedRegressionTest {
    private static int assertions;
    public static void run() {
        assertions = 0;
        Vector zero = new Vector(0, 0, 0);
        for (double speed : new double[] {2.5, 3, 3.15}) {
            for (double distance : new double[] {8, 16, 32, 48}) {
                for (double height : new double[] {-4, 0, 4}) {
                    for (double motion : new double[] {-0.12, 0, 0.12}) {
                        for (double inheritedX : new double[] {0, 0.15}) {
                            Vector offset = new Vector(distance, height, 5);
                            Vector target = new Vector(motion, 0, 0.08);
                            Vector inherited = new Vector(inheritedX, inheritedX == 0 ? 0 : -0.2, 0);
                            var solved = RangedBallistics.solve(offset, target, inherited, speed);
                            check(solved != null, "reachable intercept");
                            close(solved.launch().length(), speed, 1e-6);
                            Vector hit = forward(solved.launch().add(inherited), solved.ticks());
                            Vector expected = offset.add(target.scale(solved.ticks()));
                            close(hit.x(), expected.x(), 1e-6);
                            close(hit.y(), expected.y(), 1e-6);
                            close(hit.z(), expected.z(), 1e-6);
                            check(solved.ticks() > 0 && solved.ticks() <= 80, "bounded flight time");
                        }
                    }
                }
            }
        }
        check(RangedBallistics.solve(new Vector(96, 0, 0), zero, zero, 0.3) == null, "weak bow cannot reach distant target");
        check(RangedBallistics.solve(new Vector(10000, 0, 0), zero, zero, 3) == null, "unreachable target rejected");
        check(RangedBallistics.solve(zero, zero, zero, 3) == null, "zero separation rejected");
        check(RangedBallistics.solve(new Vector(Double.NaN, 0, 0), zero, zero, 3) == null, "nonfinite target rejected");
        check(RangedBallistics.solve(new Vector(10, 0, 0), zero, zero, Double.NaN) == null, "nonfinite speed rejected");
        check(RangedBallistics.solve(new Vector(10, 0, 0), new Vector(5, 0, 0), zero, 3) == null, "unsupported target velocity rejected");
        check(RangedBallistics.solve(new Vector(10, 0, 0), zero, new Vector(0, -5, 0), 3) == null, "unsupported shooter velocity rejected");
        check(RangedBallistics.solve(null, zero, zero, 3) == null, "missing target rejected");
        for (boolean mobs : new boolean[] {false, true}) for (boolean players : new boolean[] {false, true}) {
            int drivers = (RangedBallistics.drives(false, mobs, players) ? 1 : 0)
                    + (RangedBallistics.drives(true, mobs, players) ? 1 : 0);
            check(drivers == (mobs || players ? 1 : 0), "exactly one driver, never two fighting controllers");
        }
        for (var kind : RangedWeaponRules.Kind.values()) {
            check(!RangedWeaponRules.canAim(kind, false, true, true, true, 1, false, true, false), "disabled weapon");
            check(!RangedWeaponRules.canAim(kind, true, true, true, true, 1, true, true, false), "Riptide/firework excluded");
            check(!RangedWeaponRules.canAim(kind, true, true, true, true, 1, false, false, false), "no ammo/damaged trident");
            check(!RangedWeaponRules.canAim(kind, true, true, true, true, 1, false, true, true), "weapon cooldown");
            check(!RangedWeaponRules.canAim(kind, true, true, true, true, Double.NaN, false, true, false), "invalid charge");
        }
        check(!RangedWeaponRules.canAim(RangedWeaponRules.Kind.BOW, true, false, false, true, 1, false, true, false), "idle bow never aims");
        check(!RangedWeaponRules.canAim(RangedWeaponRules.Kind.BOW, true, true, false, false, 0.09, false, true, false), "underdrawn bow");
        check(RangedWeaponRules.canAim(RangedWeaponRules.Kind.BOW, true, true, false, false, 0.5, false, true, false), "drawing bow aims");
        check(!RangedWeaponRules.canAim(RangedWeaponRules.Kind.TRIDENT, true, true, false, false, 0.9, false, true, false), "trident waits threshold");
        check(RangedWeaponRules.canAim(RangedWeaponRules.Kind.TRIDENT, true, true, false, false, 1, false, true, false), "charged thrown trident");
        check(!RangedWeaponRules.canAim(RangedWeaponRules.Kind.CROSSBOW, true, false, true, false, 1, false, true, false), "loaded crossbow requires hold key");
        check(RangedWeaponRules.canAim(RangedWeaponRules.Kind.CROSSBOW, true, false, true, true, 1, false, true, false), "loaded held-aim crossbow");
        check(RangedWeaponRules.canAim(RangedWeaponRules.Kind.CROSSBOW, true, true, false, false, 0.5, false, true, false), "loading crossbow");
        check(!RangedWeaponRules.canAim(RangedWeaponRules.Kind.CROSSBOW, true, false, false, true, 0, false, true, false), "unloaded idle crossbow");
        GumihoConfig c = new GumihoConfig();
        c.modules.put("bow_aim_assist", true); c.modules.put("player_bow_aim", false);
        c.hudX.put("bow_status_hud", 47); c.hudY.put("bow_status_hud", 91);
        c.bowAimRange = 53; c.bowAimSmoothness = 7;
        CombatConfigMigration.normalize(c);
        check(c.rangedBows && !c.rangedCrossbows && !c.rangedTridents, "no newly enabled weapon automation on upgrade");
        check(c.rangedLead && c.rangedCheckPath, "prediction checks default on");
        check(c.modules.get("bow_aim_assist") && !c.modules.get("player_bow_aim"), "target toggles preserved");
        check(c.hudX.get("bow_status_hud") == 47 && c.hudY.get("bow_status_hud") == 91, "HUD position preserved");
        close(c.bowAimRange, 53, 0.001); close(c.bowAimSmoothness, 7, 0.001);
        c.bowAimRange = Double.NaN; c.bowAimSmoothness = Double.POSITIVE_INFINITY;
        c.rangedFov = -1; c.rangedAimResponse = 3;
        CombatConfigMigration.normalize(c);
        close(c.bowAimRange, 64, 0.001); close(c.bowAimSmoothness, 3.5, 0.001);
        close(c.rangedFov, 10, 0.001); close(c.rangedAimResponse, 0.4, 0.001);
        System.out.println("PASS: " + assertions + " ranged intercept, controller and compatibility assertions.");
    }
    private static Vector forward(Vector launch, double time) {
        var state = new TrajectoryPhysics.State(0, 0, 0, launch.x(), launch.y(), launch.z());
        for (int tick = 0; tick < (int)time; tick++) state = TrajectoryPhysics.step(state, TrajectoryPhysics.Kind.ARROW, false, false, 0);
        double fraction = time - (int)time;
        return new Vector(state.x() + state.vx() * fraction, state.y() + state.vy() * fraction, state.z() + state.vz() * fraction);
    }
    private static void close(double actual, double expected, double tolerance) {
        check(Math.abs(actual - expected) < tolerance, "expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }
}
