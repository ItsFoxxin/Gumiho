package dev.gumiho.client.gui;

/** HUD display size never mutates the user's preferred anchor coordinates. */
public final class HudSizing {
    private HudSizing() { }
    public static double scale(double value) {
        return Double.isFinite(value) ? Math.max(0.5,Math.min(1.25,value)) : 1;
    }
    public static int extent(int pixels,double scale) {
        return (int)Math.ceil(Math.max(0,pixels)*scale(scale));
    }
    public static int compactHeight(int titleHeight,int rowHeight,int rows,int maximum) {
        return titleHeight+rowHeight*Math.max(1,Math.min(maximum,rows));
    }
}
