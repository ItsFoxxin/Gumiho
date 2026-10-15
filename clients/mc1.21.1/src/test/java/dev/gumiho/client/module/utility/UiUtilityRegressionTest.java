package dev.gumiho.client.module.utility;
import dev.gumiho.client.gui.HudLayout;
import dev.gumiho.client.gui.CombatSection;
import dev.gumiho.client.gui.CombatOptionHelp;
import dev.gumiho.client.gui.DescriptionScroll;
public final class UiUtilityRegressionTest {
    private static int count;
    public static void run() {
        for (int viewport : new int[] {0,180,240,360,480,1080}) {
            for (int extent : new int[] {0,28,126,176,480}) {
                for (int preferred : new int[] {-100,0,6,178,272,500,Integer.MAX_VALUE}) {
                    int result = HudLayout.visibleOrigin(preferred, extent, viewport);
                    check(result >= 0 && result <= Math.max(0,viewport-extent), "HUD remains reachable");
                    if (preferred >= 0 && preferred+(long)extent <= viewport) check(result == preferred, "valid position unchanged");
                }
            }
        }
        check(HudLayout.visibleOrigin(272,28,240)==212,"network HUD recovery");
        check(HudLayout.visibleOrigin(272,28,480)==272,"saved position restores at larger size");
        check(HudLayout.footerColumns(451)==2,"narrow footer");
        check(HudLayout.footerColumns(452)==4,"wide footer");
        check(HudLayout.footerHeight(281)==54,"two-row reservation");
        check(NetworkStatus.latency(true,true,0).equals("Local | 로컬"),"single-player is not 0ms ping");
        check(NetworkStatus.latency(false,false,50).startsWith("Offline"),"disconnected");
        check(NetworkStatus.latency(true,false,null).contains("--"),"missing ping");
        check(NetworkStatus.latency(true,false,-1).contains("--"),"invalid ping");
        check(NetworkStatus.latency(true,false,0).equals("Ping 0 ms"),"actual zero ping valid");
        check(NetworkStatus.highLatency(true,false,151),"high ping");
        check(!NetworkStatus.highLatency(true,true,999),"local has no high-ping warning");
        check(!NetworkStatus.highLatency(false,false,999),"offline has no stale warning");
        check(!NetworkStatus.highLatency(true,false,null),"unknown is not high");
        check(CombatSection.of("mace_smash")==CombatSection.MACE,"mace group");
        check(CombatSection.of("wind_mace_swap")==CombatSection.MACE,"mace support group");
        check(CombatSection.of("spear_assist")==CombatSection.SPEAR,"spear group");
        check(CombatSection.of("bow_aim_assist")==CombatSection.RANGED,"ranged group");
        check(CombatSection.of("auto_shield")==CombatSection.DEFENSE,"shield group");
        check(CombatSection.of("target_hud")==CombatSection.AWARENESS,"awareness group");
        for (long t=0; t<20000; t+=17) {
            int offset=DescriptionScroll.offset(210,100,t);
            check(offset>=0 && offset<=110,"scroll clipped to description");
            check(DescriptionScroll.offset(50,100,t)==0,"short description stays still");
        }
        check(DescriptionScroll.offset(210,100,1000)==0,"initial reading pause");
        check(DescriptionScroll.offset(210,100,6500)==110,"end reached");
        check(!CombatOptionHelp.text("Hostile Mobs | 한국어").startsWith("Adjust this"),"help: Hostile Mobs");
        check(!CombatOptionHelp.text("Marked Enemies | 한국어").startsWith("Adjust this"),"help: Marked Enemies");
        check(!CombatOptionHelp.text("Passive Mobs | 한국어").startsWith("Adjust this"),"help: Passive Mobs");
        check(!CombatOptionHelp.text("Invisible Targets | 한국어").startsWith("Adjust this"),"help: Invisible Targets");
        check(!CombatOptionHelp.text("Priority | 한국어").startsWith("Adjust this"),"help: Priority");
        check(!CombatOptionHelp.text("Target Range | 한국어").startsWith("Adjust this"),"help: Target Range");
        check(!CombatOptionHelp.text("Full FOV | 한국어").startsWith("Adjust this"),"help: Full FOV");
        check(!CombatOptionHelp.text("Melee Range | 한국어").startsWith("Adjust this"),"help: Melee Range");
        check(!CombatOptionHelp.text("Melee Aim Speed | 한국어").startsWith("Adjust this"),"help: Melee Aim Speed");
        check(!CombatOptionHelp.text("Aim Ease | 한국어").startsWith("Adjust this"),"help: Aim Ease");
        check(!CombatOptionHelp.text("Weapons Only | 한국어").startsWith("Adjust this"),"help: Weapons Only");
        check(!CombatOptionHelp.text("Wait Hurt Animation | 한국어").startsWith("Adjust this"),"help: Wait Hurt Animation");
        check(!CombatOptionHelp.text("Avoid Raised Shield | 한국어").startsWith("Adjust this"),"help: Avoid Raised Shield");
        check(!CombatOptionHelp.text("Detailed Target HUD | 한국어").startsWith("Adjust this"),"help: Detailed Target HUD");
        check(!CombatOptionHelp.text("Shield Attack Windows | 한국어").startsWith("Adjust this"),"help: Shield Attack Windows");
        check(!CombatOptionHelp.text("Mace Minimum Fall | 한국어").startsWith("Adjust this"),"help: Mace Minimum Fall");
        check(!CombatOptionHelp.text("Mace Lock Range | 한국어").startsWith("Adjust this"),"help: Mace Lock Range");
        check(!CombatOptionHelp.text("Landing Guide | 한국어").startsWith("Adjust this"),"help: Landing Guide");
        check(!CombatOptionHelp.text("Mace Air Correction | 한국어").startsWith("Adjust this"),"help: Mace Air Correction");
        check(!CombatOptionHelp.text("Reset Radius | 한국어").startsWith("Adjust this"),"help: Reset Radius");
        check(!CombatOptionHelp.text("Shield Lead | 한국어").startsWith("Adjust this"),"help: Shield Lead");
        check(!CombatOptionHelp.text("Shield Projectiles | 한국어").startsWith("Adjust this"),"help: Shield Projectiles");
        check(!CombatOptionHelp.text("React to Swings | 한국어").startsWith("Adjust this"),"help: React to Swings");
        check(!CombatOptionHelp.text("Close Melee Guard | 한국어").startsWith("Adjust this"),"help: Close Melee Guard");
        check(!CombatOptionHelp.text("Explosion Guard | 한국어").startsWith("Adjust this"),"help: Explosion Guard");
        System.out.println("UI/utility regression: "+count+" assertions passed");
    }
    private static void check(boolean result,String message) {
        count++; if(!result) throw new AssertionError(message);
    }
}
