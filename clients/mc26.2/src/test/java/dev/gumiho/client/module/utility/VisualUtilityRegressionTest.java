package dev.gumiho.client.module.utility;
public final class VisualUtilityRegressionTest {
    private static int count;
    public static void run() {
        check(HudValues.duration(-1).equals("∞"),"permanent");
        check(HudValues.duration(-2).equals("0:00"),"invalid duration");
        check(HudValues.duration(0).equals("0:00"),"expired");
        check(HudValues.duration(1).equals("0:01"),"last tick visible");
        check(HudValues.duration(1200).equals("1:00"),"minute");
        check(HudValues.duration(1201).equals("1:01"),"ceil remaining");
        check(HudValues.heading(0).startsWith("S"),"MC yaw zero south");
        check(HudValues.heading(90).startsWith("W"),"west");
        check(HudValues.heading(-90).startsWith("E"),"east");
        check(HudValues.heading(180).startsWith("N"),"north");
        check(HudValues.heading(Double.NaN).equals("--"),"invalid yaw");
        for(int yaw=-720;yaw<=720;yaw++) {
            int bearing=HudValues.bearing(yaw);
            check(bearing>=0&&bearing<360,"bearing bounds");
            check(HudValues.heading(yaw).equals(HudValues.heading(yaw+360)),"heading wraps");
        }
        check(HudValues.bearing(180)==0,"north bearing");
        check(HudValues.bearing(0)==180,"south bearing");
        check(HudValues.durability(0,0)==-1,"undamageable");
        check(HudValues.durability(Integer.MIN_VALUE,Integer.MAX_VALUE)==100,"overflow clamp");
        for(int damage=-10;damage<=1100;damage++) {
            int value=HudValues.durability(damage,1000);
            check(value>=0&&value<=100,"durability clamp");
        }
        check(HudValues.durability(750,1000)==25,"warning threshold");
        check(HudValues.cooldown(0).startsWith("Ready"),"ready cooldown");
        check(HudValues.cooldown(.001f).startsWith("1%"),"nonzero never ready");
        check(HudValues.cooldown(2).startsWith("100%"),"oversized cooldown");
        check(HudValues.cooldown(Float.NaN).equals("--"),"unknown cooldown");
        check(HudValues.cooldown(-1).equals("--"),"invalid cooldown");
        System.out.println("Visual/utility regression: "+count+" assertions passed");
    }
    private static void check(boolean value,String label) { count++; if(!value) throw new AssertionError(label); }
}
