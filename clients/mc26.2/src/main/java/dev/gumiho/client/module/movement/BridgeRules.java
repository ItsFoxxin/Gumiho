package dev.gumiho.client.module.movement;

/** Conservative, version-independent building controls. */
public final class BridgeRules {
    private BridgeRules() { }
    public enum Mode {
        BRIDGE("Bridge | 다리"), TOWER("Tower | 기둥"), WALL("Wall | 벽");
        public final String label;
        Mode(String label) { this.label = label; }
    }
    public static Mode mode(String value) {
        for (Mode mode : Mode.values()) if (mode.name().equals(value)) return mode;
        return Mode.BRIDGE;
    }
    public static int delay(int value) { return Math.max(4, Math.min(12, value)); }
    public static boolean tower(boolean jumpHeld, boolean grounded, double verticalSpeed) {
        return jumpHeld && !grounded && Double.isFinite(verticalSpeed) && verticalSpeed > 0;
    }
}
