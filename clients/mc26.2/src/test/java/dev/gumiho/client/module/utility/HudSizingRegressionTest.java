package dev.gumiho.client.module.utility;

import dev.gumiho.client.gui.HudSizing;
import dev.gumiho.client.gui.HudLayout;

public final class HudSizingRegressionTest {
    private static int count;
    public static void run() {
        check(HudSizing.scale(Double.NaN)==1,"invalid default");
        check(HudSizing.scale(.1)==.5,"minimum");
        check(HudSizing.scale(3)==1.25,"maximum");
        for(int width=0;width<=512;width++) {
            for(double scale:new double[]{-.1,.5,.65,1,1.25,Double.NaN,Double.POSITIVE_INFINITY}) {
                int extent=HudSizing.extent(width,scale);
                check(extent>=Math.ceil(width*.5)&&extent<=Math.ceil(width*1.25),"size bounds");
                int preferred=420;
                int visible=HudLayout.visibleOrigin(preferred,extent,480);
                check(visible>=0&&visible<=Math.max(0,480-extent),"scaled viewport clamp");
                check(HudLayout.visibleOrigin(preferred,extent,preferred+extent+20)==preferred,"roomier viewport restores anchor");
            }
        }
        check(HudSizing.compactHeight(18,11,0,6)==29,"empty panel compact");
        check(HudSizing.compactHeight(18,11,2,6)==40,"two rows");
        check(HudSizing.compactHeight(19,11,50,5)==74,"tracker maximum");
        System.out.println("HUD sizing regression: "+count+" assertions passed");
    }
    private static void check(boolean value,String label) {count++;if(!value)throw new AssertionError(label);}
}
