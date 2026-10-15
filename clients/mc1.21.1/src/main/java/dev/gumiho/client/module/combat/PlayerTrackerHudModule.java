package dev.gumiho.client.module.combat;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.gui.HudEditorScreen;
import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import dev.gumiho.client.tracking.PlayerGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PlayerTrackerHudModule extends HudModule {
    private static final int WIDTH = 210;
    private static final int MAX_ROWS = 5;
    private int visibleRows = 1;
    private Object cachedLevel,cachedPlayer;
    private int cachedTick=Integer.MIN_VALUE;
    private List<AbstractClientPlayer> cachedPlayers=List.of();

    public PlayerTrackerHudModule() {
        super(
                "player_tracker_hud",
                "Player Tracker",
                "플레이어 추적기",
                "Show loaded teammates, enemies, health, distance, and armor condition",
                "불러온 팀원과 적의 체력, 거리, 방어구 상태를 표시합니다",
                ModuleCategory.COMBAT,
                true,
                178,
                6
        );
    }

    @Override
    public void renderHud(GuiGraphics graphics, Minecraft client) {
        List<AbstractClientPlayer> players = samplePlayers(client);
        visibleRows = players.size();
        if (players.isEmpty() && GumihoClient.MODULES.isEnabled("contextual_hud")
                && !(client.screen instanceof HudEditorScreen)) return;
        graphics.fill(x(), y(), x() + WIDTH, y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        graphics.fill(x(), y(), x() + 3, y() + hudHeight(), 0xFFFF7A18);
        graphics.drawString(client.font, "Tracker | 추적기", x() + 7, y() + 4, 0xFFFFE8C2, false);

        if (players.isEmpty()) {
            graphics.drawString(client.font, "No loaded players | 플레이어 없음", x() + 7, y() + 17, 0xFF9E9187, false);
            return;
        }

        int row = 0;
        for (AbstractClientPlayer player : players) {
            if (row >= MAX_ROWS) break;
            PlayerGroup group = GumihoClient.PLAYER_GROUPS.groupOf(player.getScoreboardName());
            String state = group == PlayerGroup.ENEMY && player.getHealth() <= 8.0F ? " WEAK" : "";
            if (group == PlayerGroup.TEAMMATE && player.getHealth() <= 8.0F) state = " LOW";
            if (GumihoClient.PLAYER_GROUPS.isPinned(player.getScoreboardName())) state = " PIN";
            String line = String.format(
                    Locale.ROOT,
                    "[%s%s %d%s] %s %.0fm %.0fHP %s",
                    shortGroup(group),
                    state,
                    CombatAwareness.threatScore(client, player),
                    CombatAwareness.direction(client, player.position()),
                    trimName(player.getScoreboardName()),
                    client.player.distanceTo(player),
                    player.getHealth(),
                    CombatAwareness.armorStatus(player)
            );
            graphics.drawString(client.font, client.font.plainSubstrByWidth(line,WIDTH-14), x() + 7, y() + 17 + row * 11, color(group, state), false);
            row++;
        }
    }

    @Override public int hudWidth(Minecraft client) { visibleRows = samplePlayers(client).size(); return WIDTH; }
    @Override public int hudHeight() { return dev.gumiho.client.gui.HudSizing.compactHeight(19,11,visibleRows,MAX_ROWS); }

    private List<AbstractClientPlayer> samplePlayers(Minecraft client) {
        int tick=client.player==null?0:client.player.tickCount;
        if(cachedLevel!=client.level||cachedPlayer!=client.player||cachedTick!=tick) {
            cachedLevel=client.level;cachedPlayer=client.player;cachedTick=tick;
            cachedPlayers=trackedPlayers(client);
        }
        return cachedPlayers;
    }
    @Override protected void onDisable(Minecraft client) {
        cachedPlayers=List.of();cachedLevel=null;cachedPlayer=null;cachedTick=Integer.MIN_VALUE;visibleRows=1;
    }
    private static List<AbstractClientPlayer> trackedPlayers(Minecraft client) {
        if (client.level == null || client.player == null) return List.of();
        List<AbstractClientPlayer> result = new ArrayList<>();
        for (AbstractClientPlayer player : CombatAwareness.loadedPlayers(client)) {
            PlayerGroup group = GumihoClient.PLAYER_GROUPS.groupOf(player.getScoreboardName());
            if (group != PlayerGroup.NEUTRAL || GumihoClient.MODULES.isEnabled("show_neutral_players")) result.add(player);
        }
        result.sort(Comparator
                .comparingInt((AbstractClientPlayer player) -> priority(
                        GumihoClient.PLAYER_GROUPS.groupOf(player.getScoreboardName()), player.getHealth()))
                .thenComparingDouble(client.player::distanceTo));
        return result;
    }

    private static int priority(PlayerGroup group, float health) {
        if (group == PlayerGroup.ENEMY && health <= 8.0F) return 0;
        if (group == PlayerGroup.TEAMMATE && health <= 8.0F) return 1;
        if (group == PlayerGroup.ENEMY) return 2;
        if (group == PlayerGroup.TEAMMATE) return 3;
        return 4;
    }

    private static String shortGroup(PlayerGroup group) {
        return switch (group) {
            case TEAMMATE -> "TEAM";
            case ENEMY -> "ENEMY";
            case NEUTRAL -> "NEUTRAL";
        };
    }

    private static int color(PlayerGroup group, String state) {
        if (!state.isEmpty()) return group == PlayerGroup.ENEMY ? 0xFFFF544A : 0xFFFFC15A;
        return switch (group) {
            case TEAMMATE -> 0xFF78E69A;
            case ENEMY -> 0xFFFF8A78;
            case NEUTRAL -> 0xFFB8AEA6;
        };
    }

    private static String trimName(String name) {
        return name.length() <= 8 ? name : name.substring(0, 8);
    }
}
