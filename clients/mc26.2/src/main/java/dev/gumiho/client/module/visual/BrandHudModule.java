package dev.gumiho.client.module.visual;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.module.HudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BrandHudModule extends HudModule {
    private static final int ORANGE = 0xFFFF7A18;
    private static final int CREAM = 0xFFFFE8C2;

    public BrandHudModule() {
        super(
                "brand_hud",
                "Brand HUD",
                "브랜드 HUD",
                "Show the GUMIHO name and version",
                "구미호 이름과 버전을 표시합니다",
                true,
                6,
                6
        );
    }

    @Override
    public void renderHud(GuiGraphicsExtractor context, Minecraft client) {
        context.fill(x(), y(), x() + hudWidth(client), y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        context.fill(x(), y(), x() + 3, y() + hudHeight(), ORANGE);
        context.text(client.font, "GUMIHO | 구미호", x() + 8, y() + 4, CREAM);
        context.text(client.font, dev.gumiho.client.BuildInfo.LABEL, x() + 8, y() + 14, 0xFFFFA45B);
    }

    @Override
    public int hudWidth(Minecraft client) {
        return Math.max(122, Math.max(client.font.width("GUMIHO | 구미호"),
                client.font.width(dev.gumiho.client.BuildInfo.LABEL)) + 16);
    }

    @Override
    public int hudHeight() {
        return 26;
    }
}
