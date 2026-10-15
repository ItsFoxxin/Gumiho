package dev.gumiho.client.module.combat;

import dev.gumiho.client.config.CombatConfigMigration;
import dev.gumiho.client.config.GumihoConfig;

public final class ShieldWindowRegressionTest {
    private static int assertions;
    private static final double CLEAR = Double.POSITIVE_INFINITY;

    public static void run() {
        ShieldAttackWindow window = new ShieldAttackWindow();
        check(!window.open(0) && window.allowsAutoAttack(0), "idle does not change normal melee");
        check(!window.automatic(0, false, CLEAR), "invalid target/cooldown cannot open shield");
        for (double eta : new double[] {0, 1, 5, 7, -1, Double.NaN, Double.NEGATIVE_INFINITY})
            check(!window.automatic(0, true, eta), "unsafe or unknown impact prevents automatic gap");
        check(window.automatic(0, true, 7.01), "clear reblock margin allows automatic gap");
        check(window.open(0) && window.allowsAutoAttack(0), "one automatic hit allowed");
        check(!window.automatic(0, true, CLEAR), "existing gap is not extended");
        window.attacked(0);
        check(!window.allowsAutoAttack(0), "second producer blocked after hit");
        check(!window.open(1), "successful hit reblocks next tick");
        window.raised(1);
        for (int t = 1; t < 7; t++)
            check(!window.automatic(t, true, CLEAR), "shield gets startup plus active tick before another gap");
        check(window.automatic(7, true, CLEAR), "next attack after recovery");
        check(window.open(8), "cancelled hit can retry once");
        check(!window.open(9), "lost target/slot switch does not leave shield down");

        window.reset();
        window.manual(0); // input event before the next client tick
        check(window.open(1) && !window.allowsAutoAttack(1), "manual click reserved through native tick processing");
        check(!window.automatic(1, true, CLEAR), "manual click wins over automation");
        check(!window.open(2), "manual opening bounded even with no target");
        window.raised(2);
        window.manual(3);
        check(window.open(3), "new manual press overrides reblock recovery");
        check(!window.allowsAutoAttack(3), "manual override never also attacks automatically");
        window.reset();
        check(window.automatic(0, true, CLEAR), "disable/world reset clears pending timers");
        window.manual(0);
        check(!window.allowsAutoAttack(0), "manual click takes over an automatic gap");
        // A held key does not send repeated PRESS events; no renewed request is made.
        for (int tick = 2; tick < 40; tick++) check(!window.open(tick), "held click cannot renew manual gap");
        for (int delay : new int[] {1, 2, 5, 13, 20, 34}) simulateCrowd(delay);

        GumihoConfig config = new GumihoConfig();
        check(config.shieldAttackWindows, "attack windows enabled by default without enabling auto shield");
        config.shieldAttackWindows = false;
        CombatConfigMigration.normalize(config);
        check(!config.shieldAttackWindows, "saved opt-out survives normalization");
        check(!config.modules.containsKey("auto_shield"), "no new automation enabled");
        System.out.println("PASS: " + assertions + " shield attack-window assertions.");
    }

    private static void simulateCrowd(int cooldown) {
        ShieldAttackWindow window = new ShieldAttackWindow();
        AttackGate gate = new AttackGate();
        Object player = new Object();
        boolean raised = true;
        int lastHit = -1000, hits = 0, lastRaised = 0;
        window.raised(0);
        for (int tick = 0; tick < 300; tick++) {
            float charge = Math.min(1F, (tick - lastHit) / (float) cooldown);
            if (!window.open(tick)) {
                if (!raised) { raised = true; window.raised(tick); lastRaised = tick; }
                if (window.automatic(tick, gate.ready(player, tick, charge), CLEAR)) {
                    check(tick - lastRaised >= 6, "crowd still gets functional reblock time");
                    raised = false;
                }
            }
            if (window.open(tick) && window.allowsAutoAttack(tick) && gate.ready(player, tick, charge)) {
                check(charge >= 1, "only full-damage cooldown");
                check(tick - lastHit >= cooldown, "weapon interval respected");
                lastHit = tick; hits++;
                gate.record(player, tick);
                window.attacked(tick);
                check(!gate.ready(player, tick, 1) && !window.allowsAutoAttack(tick), "one hit across producers");
            }
        }
        check(hits > 2, "continuous close guard does not starve attacks");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
