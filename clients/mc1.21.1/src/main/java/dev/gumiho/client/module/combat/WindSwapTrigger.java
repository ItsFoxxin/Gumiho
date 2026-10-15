package dev.gumiho.client.module.combat;

/** One-shot, short-lived confirmation of an attempted throw in the same session/slot. */
public final class WindSwapTrigger {
    private Object player;
    private int tick, slot;
    public void arm(Object player, int tick, int slot) { this.player = player; this.tick = tick; this.slot = slot; }
    public boolean consume(Object player, int tick, int slot, boolean cooldownStarted) {
        if (this.player == null) return false;
        if (this.player != player || this.slot != slot || tick < this.tick || tick - this.tick > 2) {
            reset(); return false;
        }
        if (!cooldownStarted) return false;
        reset(); return true;
    }
    public void reset() { player = null; }
}
