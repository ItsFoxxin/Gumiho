package dev.gumiho.client.gui;

/** Pause at both ends so a long English/Korean description remains readable. */
public final class DescriptionScroll {
    public static int offset(int textWidth, int viewport, long elapsedMillis) {
        int overflow = Math.max(0, textWidth - Math.max(0, viewport));
        if (overflow == 0 || elapsedMillis <= 1000) return 0;
        double travel = overflow * 50.0;
        double phase = elapsedMillis % (travel * 2 + 2000);
        if (phase < 1000) return 0;
        if (phase < 1000 + travel) return (int)((phase - 1000) / 50);
        if (phase < 2000 + travel) return overflow;
        return Math.max(0, overflow - (int)((phase - 2000 - travel) / 50));
    }
    private DescriptionScroll() { }
}
