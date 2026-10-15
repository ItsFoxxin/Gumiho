package dev.gumiho.client.module.combat;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import dev.gumiho.client.tracking.PlayerGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DirectionalIndicatorsModule extends GumihoModule {
    public DirectionalIndicatorsModule() {
        super("directional_indicators", "Enemy Indicators", "적 방향 표시",
                "Show screen-edge direction markers for loaded marked enemies",
                "불러온 지정 적의 화면 가장자리 방향 표시를 보여줍니다",
                ModuleCategory.COMBAT, true, false);
    }

    @Override
    public void renderHud(GuiGraphics graphics, Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) return;
        List<AbstractClientPlayer> enemies = CombatAwareness.loadedPlayers(client).stream()
                .filter(player -> GumihoClient.PLAYER_GROUPS.groupOf(player.getScoreboardName()) == PlayerGroup.ENEMY)
                .sorted(Comparator.comparingDouble(client.player::distanceTo))
                .limit(3)
                .toList();
        for (int index = 0; index < enemies.size(); index++) {
            AbstractClientPlayer enemy = enemies.get(index);
            float yaw = targetYaw(client, enemy);
            float relative = Mth.wrapDegrees(yaw - client.player.getYRot());
            boolean behind = Math.abs(relative) > 90.0F;
            float horizontal = Mth.clamp(relative / 90.0F, -1.0F, 1.0F);
            int screenWidth = graphics.guiWidth();
            int x = Math.round(screenWidth / 2.0F + horizontal * (screenWidth / 2.0F - 55.0F));
            int y = behind ? graphics.guiHeight() - 52 - index * 12 : 34 + index * 12;
            String label = String.format(Locale.ROOT, "%s %s %.0fm T%d",
                    CombatAwareness.direction(client, enemy.position()), trim(enemy.getScoreboardName()),
                    client.player.distanceTo(enemy), CombatAwareness.threatScore(client, enemy));
            int width = client.font.width(label) + 8;
            graphics.fill(x - width / 2, y - 2, x + width / 2, y + 10, 0xD0180D0B);
            graphics.drawCenteredString(client.font, label, x, y, enemy.getHealth() <= 8.0F ? 0xFFFF544A : 0xFFFFA45B);
        }
    }

    private static float targetYaw(Minecraft client, AbstractClientPlayer enemy) {
        double dx = enemy.getX() - client.player.getX();
        double dz = enemy.getZ() - client.player.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
    }

    private static String trim(String name) {
        return name.length() <= 10 ? name : name.substring(0, 10);
    }
}
