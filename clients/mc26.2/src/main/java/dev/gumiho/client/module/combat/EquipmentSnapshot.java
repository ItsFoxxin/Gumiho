package dev.gumiho.client.module.combat;

/** Detect automation-driven swaps after the native player tick has already run. */
public final class EquipmentSnapshot {
    private Object player, item;
    private int tick, slot;
    public void capture(Object player, int tick, int slot, Object item) {
        this.player = player; this.tick = tick; this.slot = slot; this.item = item;
    }
    public boolean unchanged(Object player, int tick, int slot, Object item) {
        return player != null && this.player == player && this.tick == tick && this.slot == slot && this.item == item;
    }
}
