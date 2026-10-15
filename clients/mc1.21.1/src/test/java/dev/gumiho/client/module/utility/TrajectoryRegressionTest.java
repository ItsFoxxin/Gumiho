package dev.gumiho.client.module.utility;

import dev.gumiho.client.config.CombatConfigMigration;
import dev.gumiho.client.config.GumihoConfig;
import dev.gumiho.client.module.utility.TrajectoryPhysics.Kind;
import dev.gumiho.client.module.utility.TrajectoryPhysics.State;

/** Pure physics checks; collision/rendering still require a Minecraft test world. */
public final class TrajectoryRegressionTest {
    private static int assertions;
    public static void run() {
        assertions = 0;
        State launch = new State(20, 80, -30, 3, 0, 0);
        for (Kind kind : Kind.values()) {
            for (boolean water : new boolean[] {false, true}) {
                double drag = water ? kind.waterDrag : kind.airDrag;
                State next = TrajectoryPhysics.step(launch, kind, water, false, 0);
                close(next.x(), 23); close(next.y(), 80); close(next.z(), -30);
                close(next.vx(), 3 * drag); close(next.vy(), -kind.gravity); close(next.vz(), 0);
                State noGravity = TrajectoryPhysics.step(launch, kind, water, true, 0);
                close(noGravity.vy(), 0);
                State moving = new State(0, 0, 0, 1, 2, 3);
                for (int i = 0; i < 80; i++) {
                    moving = TrajectoryPhysics.step(moving, kind, water, false, kind.accelerating() ? 0.1 : 0);
                    check(moving != null && moving.finite(), "finite bounded-horizon prediction: " + kind);
                }
            }
        }
        // Independently specified expected vanilla profiles, not only enum self-comparisons.
        close(TrajectoryPhysics.step(launch, Kind.ARROW, false, false, 0).vx(), 2.97);
        close(TrajectoryPhysics.step(launch, Kind.ARROW, true, false, 0).vx(), 1.8);
        close(TrajectoryPhysics.step(launch, Kind.TRIDENT, true, false, 0).vx(), 2.97);
        close(TrajectoryPhysics.step(launch, Kind.THROWABLE, true, false, 0).vx(), 2.4);
        close(TrajectoryPhysics.step(launch, Kind.THROWABLE, false, false, 0).vy(), -0.03);
        close(TrajectoryPhysics.step(launch, Kind.POTION, false, false, 0).vy(), -0.05);
        close(TrajectoryPhysics.step(launch, Kind.EXPERIENCE, false, false, 0).vy(), -0.07);
        State arrow1 = TrajectoryPhysics.step(launch, Kind.ARROW, false, false, 0);
        State arrow2 = TrajectoryPhysics.step(arrow1, Kind.ARROW, false, false, 0);
        close(arrow2.x(), 25.97); close(arrow2.y(), 79.95); close(arrow2.vy(), -0.0995);
        State diagonal = new State(0, 0, 0, 3, 4, 0);
        State fire = TrajectoryPhysics.step(diagonal, Kind.FIREBALL, false, false, 0.1);
        close(fire.x(), 3); close(fire.y(), 4);
        close(fire.vx(), 3.06 * 0.95); close(fire.vy(), 4.08 * 0.95);
        close(TrajectoryPhysics.step(launch, Kind.DANGEROUS_SKULL, false, false, 0.1).vx(), 3.1 * 0.73);
        close(TrajectoryPhysics.step(launch, Kind.FIREBALL, true, false, 0.1).vx(), 3.1 * 0.8);
        State wind = launch;
        for (int i = 0; i < 40; i++) wind = TrajectoryPhysics.step(wind, Kind.WIND, false, false, 0);
        close(wind.x(), 140); close(wind.y(), 80); close(wind.vx(), 3);
        State zero = new State(0, 0, 0, 0, 0, 0);
        check(TrajectoryPhysics.step(zero, Kind.FIREBALL, false, false, 0.1).finite(), "stationary normalization safe");
        check(TrajectoryPhysics.step(null, Kind.ARROW, false, false, 0) == null, "null state rejected");
        check(TrajectoryPhysics.step(launch, null, false, false, 0) == null, "unknown model rejected");
        check(TrajectoryPhysics.step(launch, Kind.WIND, false, false, Double.NaN) == null, "NaN acceleration rejected");
        check(TrajectoryPhysics.step(new State(Double.NaN, 0, 0, 0, 0, 0), Kind.WIND, false, false, 0) == null, "NaN position rejected");
        check(TrajectoryPhysics.step(new State(0, 0, 0, Double.POSITIVE_INFINITY, 0, 0), Kind.WIND, false, false, 0) == null, "infinite velocity rejected");
        check(TrajectoryPhysics.step(new State(Double.MAX_VALUE, 0, 0, Double.MAX_VALUE, 0, 0), Kind.WIND, false, false, 0) == null, "overflow rejected");
        GumihoConfig c = new GumihoConfig();
        check(!c.modules.getOrDefault("projectile_paths", false), "new module defaults off");
        close(c.projectilePathRange, 64); close(c.projectilePathTicks, 40); close(c.projectilePathLimit, 12);
        c.projectilePathRange = Double.NaN; c.projectilePathTicks = 10000; c.projectilePathLimit = -1;
        CombatConfigMigration.normalize(c);
        close(c.projectilePathRange, 64); close(c.projectilePathTicks, 80); close(c.projectilePathLimit, 1);
        c.projectilePathRange = 1; c.projectilePathTicks = -1; c.projectilePathLimit = 100;
        CombatConfigMigration.normalize(c);
        close(c.projectilePathRange, 16); close(c.projectilePathTicks, 10); close(c.projectilePathLimit, 24);
        System.out.println("PASS: " + assertions + " projectile physics and setting assertions.");
    }
    private static void close(double actual, double expected) {
        check(Math.abs(actual - expected) < 1e-8, "expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
