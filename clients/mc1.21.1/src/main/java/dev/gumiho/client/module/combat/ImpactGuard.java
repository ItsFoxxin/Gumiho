package dev.gumiho.client.module.combat;

/** Hold only through a short predicted impact; no idle threat or periodic cycling. */
public final class ImpactGuard {
    private int until = Integer.MIN_VALUE;
    public boolean update(int tick, double impactTicks, int leadTicks, boolean newMeleeSwing) {
        if (Double.isFinite(impactTicks) && impactTicks >= 0 && impactTicks <= leadTicks)
            until = Math.max(until, tick + (int)Math.ceil(impactTicks) + 1);
        if (newMeleeSwing) until = Math.max(until, tick + 6);
        return tick < until;
    }
    public void reset() { until = Integer.MIN_VALUE; }
}
