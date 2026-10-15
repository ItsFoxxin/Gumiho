package dev.gumiho.client.module.visual;

import dev.gumiho.client.gui.HudTheme;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.utility.HudValues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class CompassHudModule extends HudModule {
    public CompassHudModule() {
        super("compass_hud", "Compass HUD", "나침반 HUD",
                "Cardinal direction and north-based bearing; no player or base tracking",
                "방향과 북쪽 기준 방위각 표시. 플레이어나 기지를 추적하지 않습니다",
                false, 178, 6);
    }
    @Override public void renderHud(GuiGraphicsExtractor g,Minecraft c) {
        String value = c.player == null ? "N | 북  0°" : HudValues.heading(c.player.getYRot()) + "  " + HudValues.bearing(c.player.getYRot()) + "°";
        g.fill(x(),y(),x()+hudWidth(c),y()+hudHeight(),HudTheme.PANEL_BACKGROUND);
        g.fill(x(),y(),x()+3,y()+hudHeight(),0xFFFF7A18);
        g.text(c.font,value,x()+7,y()+5,0xFFFFE8C2,false);
    }
    @Override public int hudWidth(Minecraft c) { return 132; }
    @Override public int hudHeight() { return 20; }
}
