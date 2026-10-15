package dev.gumiho.client.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Palette/source coverage; single-pass render ownership is tested on ModuleManager separately. */
public final class HudThemeRegressionTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        check((HudTheme.PANEL_BACKGROUND >>> 24)==208,"one shared alpha");
        check((HudTheme.PANEL_BACKGROUND & 0xFFFFFF)==0x120E0B,"one shared surface color");
        check(HudTheme.ORANGE==0xFFFF7A18,"brand orange");
        Path modules=Path.of("src/main/java/dev/gumiho/client/module");
        var panels=new ArrayList<>(List.of(
                "SnapshotHudModule.java", "combat/AttackCooldownHudModule.java",
                "combat/BowStatusHudModule.java", "combat/MaceStatusHudModule.java",
                "combat/PlayerTrackerHudModule.java", "combat/ProjectileWarningHudModule.java",
                "combat/TargetHudModule.java", "utility/NetworkHudModule.java",
                "utility/SurvivalAlertsHudModule.java", "visual/BrandHudModule.java",
                "visual/CompassHudModule.java", "visual/CoordinatesHudModule.java"));
        if(Files.exists(modules.resolve("combat/SpearStatusHudModule.java")))
            panels.add("combat/SpearStatusHudModule.java");
        for(String panel:panels) {
            String source=Files.readString(modules.resolve(panel));
            check(source.contains("HudTheme.PANEL_BACKGROUND"),panel+" shares surface");
            check(source.indexOf("HudTheme.PANEL_BACKGROUND")==source.lastIndexOf("HudTheme.PANEL_BACKGROUND"),
                    panel+" fills background once");
        }
        try(var sources=Files.walk(modules)) {
            for(Path path:sources.filter(p->p.toString().endsWith(".java")).toList()) {
                String source=Files.readString(path);
                if(source.contains("0xD0120E0B") || source.contains("0xE0180D0B"))
                    throw new AssertionError("Hardcoded HUD surface outside theme: "+path);
            }
        }
        check(Files.readString(modules.resolve("utility/SurvivalAlertsHudModule.java"))
                .split("HudTheme.ORANGE",-1).length==3,"Survival heading and stripe orange");
        System.out.println("HUD theme regression: "+checks+" assertions passed ("+panels.size()+" panel renderers)");
    }
    private static void check(boolean result,String label) {checks++;if(!result)throw new AssertionError(label);}
}
