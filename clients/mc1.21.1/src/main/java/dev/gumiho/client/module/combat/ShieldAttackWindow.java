package dev.gumiho.client.module.combat;

/** Bounded openings in OUR shield use, not a replacement for vanilla attack cooldown. */
public final class ShieldAttackWindow {
    public static final int OPEN_TICKS = 2;
    public static final int SHIELD_STARTUP_TICKS = 5;
    private long until = Long.MIN_VALUE;
    private long autoAfter = Long.MIN_VALUE;
    private boolean manual;
    private boolean spent;

    public boolean open(long tick) { return tick < until; }

    public void manual(long tick) {
        until = tick + OPEN_TICKS;
        manual = true;
        spent = false;
    }

    public boolean automatic(long tick, boolean ready, double impactEta) {
        // Include the time needed to raise the shield again. Manual clicks explicitly override this.
        boolean safe = impactEta == Double.POSITIVE_INFINITY
                || Double.isFinite(impactEta) && impactEta > OPEN_TICKS + SHIELD_STARTUP_TICKS;
        if (open(tick) || tick < autoAfter || !ready || !safe) return false;
        until = tick + OPEN_TICKS;
        manual = false;
        spent = false;
        return true;
    }

    public boolean allowsAutoAttack(long tick) { return !open(tick) || !manual && !spent; }

    public void attacked(long tick) {
        if (open(tick)) {
            spent = true;
            // Reevaluate defense on the next tick after a successful hit.
            until = Math.min(until, tick + 1);
        }
    }

    public void raised(long tick) {
        // Let a re-raised shield become active before opening another automatic gap.
        autoAfter = tick + SHIELD_STARTUP_TICKS + 1;
    }

    public void reset() {
        until = autoAfter = Long.MIN_VALUE;
        manual = spent = false;
    }
}
