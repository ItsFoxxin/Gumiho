package dev.gumiho.client.module.movement;

import dev.gumiho.client.config.GumihoConfig;

public final class BridgeRegressionTest {
    private static int count;
    public static void run() {
        check(BridgeRules.mode(null) == BridgeRules.Mode.BRIDGE, "null mode");
        check(BridgeRules.mode("bad") == BridgeRules.Mode.BRIDGE, "invalid mode");
        for (BridgeRules.Mode mode : BridgeRules.Mode.values())
            check(BridgeRules.mode(mode.name()) == mode, "mode round trip");
        GumihoConfig oldDefaults = new GumihoConfig();
        check(BridgeRules.mode(oldDefaults.bridgeMode) == BridgeRules.Mode.BRIDGE, "old installs keep flat bridging");
        check(oldDefaults.bridgePlaceDelay == 4, "safe default");
        for (int value = -100; value <= 100; value++)
            check(BridgeRules.delay(value) >= 4 && BridgeRules.delay(value) <= 12, "bounded attempt rate");
        check(BridgeRules.delay(Integer.MIN_VALUE) == 4, "minimum");
        check(BridgeRules.delay(Integer.MAX_VALUE) == 12, "maximum");
        for (boolean jump : new boolean[]{false,true}) {
            for (boolean grounded : new boolean[]{false,true}) {
                for (double speed : new double[]{-.4, 0, .1, Double.NaN, Double.POSITIVE_INFINITY}) {
                    check(BridgeRules.tower(jump, grounded, speed)
                            == (jump && !grounded && speed == .1), "tower requires manual ascending jump");
                }
            }
        }
        System.out.println("Bridge regression: " + count + " assertions passed");
    }
    private static void check(boolean value, String label) { count++; if (!value) throw new AssertionError(label); }
}
