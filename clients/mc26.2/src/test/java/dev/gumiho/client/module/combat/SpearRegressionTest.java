package dev.gumiho.client.module.combat;

public final class SpearRegressionTest {
    private static int assertions;
    public static void run() {
        for (int flags = 0; flags < 8; flags++) {
            boolean enabled = (flags & 1) != 0, using = (flags & 2) != 0, nativeReady = (flags & 4) != 0;
            for (int hits = 0; hits <= 3; hits++) for (int protectedHits = 0; protectedHits <= 2; protectedHits++) {
                for (float charge : new float[] {0, .5f, .99f, 1, Float.NaN, Float.POSITIVE_INFINITY}) {
                    boolean expected = enabled && !using && nativeReady && hits > 0 && protectedHits == 0 && charge == 1;
                    check(SpearRules.safeJab(hits, protectedHits, enabled, using, charge, nativeReady) == expected, "jab gating");
                }
            }
        }
        check(!SpearRules.inBand(1.99, 2, 4.5), "minimum reach");
        check(SpearRules.inBand(2, 2, 4.5), "inclusive minimum");
        check(SpearRules.inBand(4.5, 2, 4.5), "inclusive maximum");
        check(!SpearRules.inBand(4.51, 2, 4.5), "maximum reach");
        check(!SpearRules.inBand(Double.NaN, 2, 4.5), "invalid distance");
        check(!SpearRules.inBand(3, 4.5, 2), "invalid band");
        check(!SpearRules.inBand(3, 2, Double.POSITIVE_INFINITY), "unbounded reach rejected");
        check(SpearRules.thrustState(9,10,20,9,9,4,4).equals("WINDUP"), "windup");
        check(SpearRules.thrustState(10,10,20,4,4,4,4).equals("READY"), "delay complete");
        check(SpearRules.thrustState(30,10,20,4,4,4,4).equals("READY"), "last valid tick");
        check(SpearRules.thrustState(31,10,20,4,4,4,4).equals("EXPIRED"), "expired damage window");
        check(SpearRules.thrustState(15,10,20,3,5,4,4).equals("TOO SLOW"), "minimum speed");
        check(SpearRules.thrustState(15,10,20,5,3,4,4).equals("TOO SLOW"), "relative speed");
        check(SpearRules.thrustState(15,10,20,Double.NaN,5,4,4).equals("TOO SLOW"), "invalid velocity");
        check(SpearRules.thrustState(-1,10,20,5,5,4,4).equals("UNKNOWN"), "invalid elapsed ticks");
        check(SpearRules.thrustState(15,10,20,5,5,Double.NaN,4).equals("UNKNOWN"), "invalid condition");
        System.out.println("Spear regression: " + assertions + " assertions passed");
    }
    private static void check(boolean result, String message) {
        assertions++;
        if (!result) throw new AssertionError(message);
    }
}
