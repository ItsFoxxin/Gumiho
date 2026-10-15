package dev.gumiho.client.module.visual;

import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.gui.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public final class CoordinatesHudModule extends HudModule {
    public CoordinatesHudModule() {
        super(
                "coordinates",
                "Coordinates",
                "좌표",
                "Show your current XYZ position",
                "현재 XYZ 위치를 표시합니다",
                true,
                8,
                36
        );
    }

    @Override
    public void renderHud(GuiGraphics context, Minecraft client) {
        if (client.player == null) {
            return;
        }

        String coordinates = String.format(
                Locale.ROOT,
                "XYZ %.1f / %.1f / %.1f",
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
        );
        context.fill(x(), y(), x() + hudWidth(client), y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        context.fill(x(), y(), x() + 3, y() + hudHeight(), HudTheme.ORANGE);
        context.drawString(client.font, coordinates, x() + 7, y() + 4, 0xFFFFE8C2, false);
    }

    @Override
    public int hudWidth(Minecraft client) {
        return client.font.width(currentCoordinates(client)) + 14;
    }

    @Override
    public int hudHeight() {
        return 18;
    }

    private static String currentCoordinates(Minecraft client) {
        if (client.player == null) {
            return "XYZ -- / -- / --";
        }
        return String.format(
                Locale.ROOT,
                "XYZ %.1f / %.1f / %.1f",
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
        );
    }
}
