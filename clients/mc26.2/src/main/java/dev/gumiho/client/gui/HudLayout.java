package dev.gumiho.client.gui;

/** Pure, non-persistent layout rules: resizing never destroys a saved position. */
public final class HudLayout {
    public static int visibleOrigin(int preferred, int extent, int viewport) {
        int limit = (int)Math.max(0L, (long)viewport - Math.max(0, extent));
        return Math.max(0, Math.min(preferred, limit));
    }
    public static int footerColumns(int available) { return available >= 452 ? 4 : 2; }
    public static int footerHeight(int available) { return footerColumns(available) == 4 ? 28 : 54; }
    private HudLayout() { }
}
