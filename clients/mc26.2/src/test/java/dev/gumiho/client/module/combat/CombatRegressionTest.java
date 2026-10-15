package dev.gumiho.client.module.combat;

import dev.gumiho.client.config.CombatConfigMigration;
import dev.gumiho.client.config.GumihoConfig;

/** No game bootstrap required. Run with scripts/test-combat.ps1. */
public final class CombatRegressionTest {
    private static int assertions;
    public static void main(String[] args) {
        dev.gumiho.client.module.utility.VisualUtilityRegressionTest.run();
        dev.gumiho.client.module.utility.HudSizingRegressionTest.run();
        dev.gumiho.client.module.movement.BridgeRegressionTest.run();
        dev.gumiho.client.module.utility.UiUtilityRegressionTest.run();
        SpearRegressionTest.run();
        dev.gumiho.client.module.utility.TrajectoryRegressionTest.run();
        RangedRegressionTest.run();
        ResetPreviewRegressionTest.run();
        ShieldWindowRegressionTest.run();
        smoothingAndShield();
        EquipmentSnapshot equipment = new EquipmentSnapshot();
        Object session = new Object(), mace = new Object(), windCharge = new Object();
        check(!equipment.unchanged(session, 1, 0, mace), "missing equipment snapshot fails closed");
        equipment.capture(session, 1, 0, windCharge);
        check(!equipment.unchanged(session, 1, 1, mace), "same-tick automatic item swap waits");
        equipment.capture(session, 35, 1, mace);
        check(equipment.unchanged(session, 35, 1, mace), "charged mace on later tick has no delayed reset");
        check(!equipment.unchanged(session, 36, 1, mace), "stale equipment sample rejected");
        check(!equipment.unchanged(new Object(), 35, 1, mace), "old session rejected");
        for (int flags = 0; flags < 8; flags++) {
            boolean hostile = (flags & 1) != 0, enemy = (flags & 2) != 0, passive = (flags & 4) != 0;
            check(CombatRules.allowed(CombatRules.Kind.HOSTILE, hostile, enemy, passive) == hostile, "hostile toggle");
            check(CombatRules.allowed(CombatRules.Kind.ENEMY_PLAYER, hostile, enemy, passive) == enemy, "enemy toggle");
            check(CombatRules.allowed(CombatRules.Kind.PASSIVE, hostile, enemy, passive) == passive, "passive toggle");
            for (CombatRules.Kind kind : new CombatRules.Kind[] {CombatRules.Kind.TEAMMATE,
                    CombatRules.Kind.NEUTRAL_PLAYER, CombatRules.Kind.INVALID}) {
                check(!CombatRules.allowed(kind, hostile, enemy, passive), "protected kind: " + kind);
            }
        }
        var gate = new AttackGate();
        Object player = new Object();
        check(!gate.ready(null, 0, 1), "no session");
        check(!gate.ready(player, 0, 0.99F), "99 percent is not full");
        check(!gate.ready(player, 0, Float.NaN), "NaN charge rejected");
        check(!gate.ready(player, 0, Float.POSITIVE_INFINITY), "infinite charge rejected");
        check(gate.ready(player, 0, 1), "initial full charge");
        gate.record(player, 0);
        check(!gate.ready(player, 0, 1), "second module same tick blocked");
        check(!gate.ready(player, 1, 0), "post-hit reset");
        check(gate.ready(player, 20, 1), "next charge accepted");
        check(gate.ready(new Object(), 0, 1), "reconnect not locked by old tick");
        for (double delay : new double[] {12.5, 20, 20 / 0.6}) simulateAttacks(delay);

        check(!CombatRules.smashReady(1.5, -1, false, 1.6), "strict smash threshold");
        check(!CombatRules.smashReady(4, 1, false, 1.6), "ascending not smash");
        check(!CombatRules.smashReady(4, -1, true, 1.6), "gliding not smash");
        check(!CombatRules.smashReady(4, -1, false, 5), "custom minimum honored");
        check(CombatRules.smashReady(4, -1, false, 1.6), "descending smash");
        check(!CombatRules.smashReady(Double.NaN, -1, false, 1.6), "invalid fall rejected");
        close(CombatRules.baseSmashBonus(1.5), 0);
        close(CombatRules.baseSmashBonus(1.6), 6.4);
        close(CombatRules.baseSmashBonus(3), 12);
        close(CombatRules.baseSmashBonus(8), 22);
        close(CombatRules.baseSmashBonus(9), 23);
        close(CombatRules.baseSmashBonus(Double.POSITIVE_INFINITY), 0);
        check(CombatRules.score("CROSSHAIR", 50, 64, 20, true, false)
                < CombatRules.score("CROSSHAIR", 0, 1, 1, false, false), "pin has priority");
        check(CombatRules.score("HEALTH", 0, 5, 2, false, false)
                < CombatRules.score("HEALTH", 0, 5, 20, false, false), "health priority");

        GumihoConfig c = new GumihoConfig();
        c.modules.put("enemy_intel_hud", true);
        c.modules.put("target_hud", false);
        c.modules.put("auto_eat", true);
        c.hudX.put("enemy_intel_hud", 250);
        c.hudY.put("enemy_intel_hud", 80);
        c.playerGroups.put("friend", "TEAMMATE");
        CombatConfigMigration.normalize(c);
        check(c.schemaVersion == 2, "schema updated");
        check(c.modules.get("target_hud"), "enabled old intel survives merge");
        check(!c.modules.containsKey("enemy_intel_hud"), "retired module removed");
        check(c.hudX.get("target_hud") == 250 && c.hudY.get("target_hud") == 80, "old intel position kept");
        check(c.targetHudDetailed, "intel detailed mode kept");
        check(c.modules.get("auto_eat"), "unrelated toggle unchanged");
        check("TEAMMATE".equals(c.playerGroups.get("friend")), "player list preserved");
        check(c.allowedServers.contains("localhost"), "allowlist preserved");
        check(!c.modules.containsKey("melee_aim") && !c.modules.containsKey("mace_smash"), "new automation not enabled by migration");
        c.targetHudDetailed = false;
        c.hudX.put("target_hud", 100);
        CombatConfigMigration.normalize(c);
        check(!c.targetHudDetailed && c.hudX.get("target_hud") == 100, "migration is idempotent");

        GumihoConfig disabled = new GumihoConfig();
        disabled.modules.put("enemy_intel_hud", false);
        disabled.modules.put("target_hud", false);
        CombatConfigMigration.normalize(disabled);
        check(!disabled.modules.get("target_hud"), "both disabled stays disabled");
        check(!disabled.targetHudDetailed, "compact mode for target-only setups");
        check(disabled.hudY.get("target_hud") == 84, "old default relocated away from mace HUD");
        c.targetRange = Double.NaN;
        c.targetFov = 999;
        c.meleeRange = -2;
        c.meleeAimSpeed = Double.POSITIVE_INFINITY;
        c.maceMinimumFall = -1;
        c.targetPriority = null;
        CombatConfigMigration.normalize(c);
        close(c.targetRange, 64);
        close(c.targetFov, 360);
        close(c.meleeRange, 1);
        close(c.meleeAimSpeed, 6);
        close(c.maceMinimumFall, 1.6);
        check("CROSSHAIR".equals(c.targetPriority), "bad priority repaired");
        check(!CombatRules.smashReady(8, Double.NEGATIVE_INFINITY, false, 1.6), "invalid motion rejected");
        check(!CombatRules.smashReady(8, 0, false, 1.6), "wind-charge apex is not descending");
        check(!CombatRules.smashReady(0, 1.5, false, 1.6), "wind-charge launch is not a smash");
        check(CombatRules.smashReady(8, -1.2, false, 1.6), "post-launch descent is smash eligible");
        check(!CombatRules.smashReady(0, 1.5, false, 1.6), "wind-burst relaunch waits for next descent");
        check(!c.modules.containsKey("auto_shield"), "shield not silently enabled");
        c.meleeAimResponse = Double.NaN;
        c.shieldRange = Double.POSITIVE_INFINITY;
        c.shieldHoldTicks = -99;
        CombatConfigMigration.normalize(c);
        close(c.meleeAimResponse, 0.08);
        close(c.shieldRange, 4.5);
        close(c.shieldHoldTicks, 6);
        GumihoConfig legacyAim = new GumihoConfig();
        legacyAim.schemaVersion = 1; legacyAim.meleeAimSpeed = 3; legacyAim.meleeAimResponse = 0.12;
        CombatConfigMigration.normalize(legacyAim);
        close(legacyAim.meleeAimSpeed, 6); close(legacyAim.meleeAimResponse, 0.08);
        legacyAim.meleeAimSpeed = 3; legacyAim.meleeAimResponse = 0.12;
        CombatConfigMigration.normalize(legacyAim);
        close(legacyAim.meleeAimSpeed, 3); close(legacyAim.meleeAimResponse, 0.12);
        legacyAim.schemaVersion = 1; legacyAim.meleeAimSpeed = 8; legacyAim.meleeAimResponse = 0.2;
        CombatConfigMigration.normalize(legacyAim);
        close(legacyAim.meleeAimSpeed, 8); close(legacyAim.meleeAimResponse, 0.2);
        check(!legacyAim.modules.containsKey("wind_mace_swap"), "swap opt-in only");
        WindSwapTrigger swap = new WindSwapTrigger();
        check(!swap.consume(session, 0, 1, true), "existing cooldown alone never swaps");
        swap.arm(session, 0, 1);
        check(!swap.consume(session, 0, 1, false), "attempt waits for local throw cooldown");
        check(swap.consume(session, 0, 1, true), "same-frame successful use can swap");
        check(!swap.consume(session, 1, 1, true), "only one swap per throw");
        swap.arm(session, 2, 1);
        check(!swap.consume(session, 2, 2, true), "manual slot change cancels");
        swap.arm(session, 4, 1);
        check(!swap.consume(session, 7, 1, true), "cancelled or delayed use expires");
        swap.arm(session, 8, 1);
        check(!swap.consume(new Object(), 8, 1, true), "world/session switch cancels");
        swap.arm(session, 9, 1); swap.reset();
        check(!swap.consume(session, 9, 1, true), "disable cancels pending swap");
        System.out.println("PASS: " + assertions + " combat, smoothing, target stability, impact-guard, mace, and migration assertions.");
    }
    private static void smoothingAndShield() {
        for (double error : new double[] {0, 0.1, -0.1, 10, -179, 179}) {
            double baseline = approach(error, 60);
            for (int fps : new int[] {15, 20, 30, 60, 144, 240}) close(approach(error, fps), baseline);
        }
        close(AimSmoothing.step(90, 10, 60, 0.12), 6);
        close(AimSmoothing.step(90, -1, 60, 0.12), 0);
        close(AimSmoothing.step(Double.NaN, 0.05, 60, 0.12), 0);
        close(AimSmoothing.step(90, Double.NaN, 60, 0.12), 0);
        close(AimSmoothing.step(90, 0.05, 0, 0.12), 0);
        close(AimSmoothing.step(90, 0.05, 60, 0), 0);
        StableTarget<Object> lock = new StableTarget<>();
        Object a = new Object(), b = new Object();
        check(lock.update(a, false, false) == a, "initial target");
        for (int i = 0; i < 20; i++) {
            check(lock.update(b, true, false) == a, "alternating challenger does not jitter");
            check(lock.update(a, true, false) == a, "old target wins again");
        }
        for (int i = 0; i < 3; i++) check(lock.update(b, true, false) == a, "wait for stable challenger");
        check(lock.update(b, true, false) == b, "four stable ticks switch target");
        check(lock.update(a, true, true) == a, "pin overrides hysteresis");
        check(lock.update(null, false, false) == null, "invalid target immediately released");
        lock.update(a, false, false);
        lock.reset();
        check(lock.current() == null, "disable clears lock");
        ImpactGuard shield = new ImpactGuard();
        for (int tick = 0; tick < 100; tick++)
            check(!shield.update(tick, Double.POSITIVE_INFINITY, 7, false), "no incoming impact leaves movement free");
        check(!shield.update(100, 8, 7, false), "far projectile does not prehold");
        check(shield.update(101, 7, 7, false), "raise at configured lead boundary");
        for (int tick = 102; tick <= 108; tick++)
            check(shield.update(tick, Double.POSITIVE_INFINITY, 7, false), "hold through predicted impact");
        check(!shield.update(109, Double.POSITIVE_INFINITY, 7, false), "release one tick after expected impact");
        check(!shield.update(110, Double.NaN, 7, false), "invalid prediction rejected");
        check(!shield.update(111, -1, 7, false), "past impact rejected");
        check(shield.update(112, 1.5, 7, false), "close arrow attempts best-effort block");
        check(!shield.update(115, Double.POSITIVE_INFINITY, 7, false), "fractional ETA expires");
        check(shield.update(116, 6, 7, false), "new projectile guard");
        shield.reset();
        check(!shield.update(116, Double.POSITIVE_INFINITY, 7, false), "manual building/disable cancels shield immediately");
        check(shield.update(120, Double.POSITIVE_INFINITY, 7, true), "optional fresh swing gives bounded pulse");
        check(!shield.update(126, Double.POSITIVE_INFINITY, 7, false), "swing pulse expires without repeat event");
        shield.reset();
        check(!shield.update(0, Double.POSITIVE_INFINITY, 7, false), "new session has no stale deadline");
        GumihoConfig config = new GumihoConfig();
        check(!config.shieldMeleeSwings, "no idle melee holding by default");
        config.shieldLeadTicks = Double.NaN; config.maceLockRange = 999;
        CombatConfigMigration.normalize(config);
        close(config.shieldLeadTicks, 7); close(config.maceLockRange, 64);
        check(!config.modules.containsKey("mace_combo_lock"), "combo aim is opt-in");
    }
    private static double approach(double error, int fps) {
        for (int frame = 0; frame < fps; frame++) {
            double step = AimSmoothing.step(error, 1.0 / fps, 60, 0.12);
            check(Math.abs(step) <= 60.0 / fps + 1e-10, "aim speed cap");
            check(Math.abs(step) <= Math.abs(error) + 1e-10, "aim never overshoots");
            error -= step;
        }
        return error;
    }
    private static void simulateAttacks(double delay) {
        AttackGate gate = new AttackGate();
        Object player = new Object();
        int ticksCharged = 100, previousHit = -1000, hits = 0;
        for (int tick = 0; tick < 200; tick++) {
            float charge = (float) Math.min(1, ticksCharged / delay);
            if (gate.ready(player, tick, charge)) {
                check(tick - previousHit >= Math.ceil(delay), "weapon interval " + delay);
                gate.record(player, tick);
                check(!gate.ready(player, tick, 1), "second producer blocked");
                previousHit = tick;
                ticksCharged = 0; // Minecraft's resetAttackStrengthTicker, called by MeleeActions.
                hits++;
            }
            ticksCharged++;
        }
        check(hits > 1, "repeated attacks after recharge");
    }
    private static void close(double actual, double expected) { check(Math.abs(actual - expected) < 0.00001, "expected " + expected + ", got " + actual); }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
