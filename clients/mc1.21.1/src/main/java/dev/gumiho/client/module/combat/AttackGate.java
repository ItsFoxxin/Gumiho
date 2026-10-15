package dev.gumiho.client.module.combat;

/** One shared gate for all automated melee producers; session is the local player identity. */
public final class AttackGate {
    private Object lastSession;
    private int lastTick = Integer.MIN_VALUE;
    public boolean ready(Object session, int tick, float charge) {
        return session != null && Float.isFinite(charge) && charge >= 1.0F
                && (session != lastSession || tick != lastTick);
    }
    public void record(Object session, int tick) {
        lastSession = session;
        lastTick = tick;
    }
}
