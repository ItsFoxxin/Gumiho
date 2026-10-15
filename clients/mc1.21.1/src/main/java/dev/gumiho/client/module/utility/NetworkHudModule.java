package dev.gumiho.client.module.utility;

import dev.gumiho.client.gui.HudTheme;

import dev.gumiho.client.module.HudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class NetworkHudModule extends HudModule {
    private String latency = "Offline | 연결 없음";
    private String performance = "FPS --";
    private int textWidth = 126;
    private boolean highLatency;
    private Object session;
    private int nextSample;
    public NetworkHudModule() {
        super("network_hud", "Network HUD", "네트워크 HUD",
                "Show server-reported ping and FPS; Local in single-player. Does not change the network",
                "서버 핑과 FPS 표시. 싱글플레이는 로컬로 표시하며 네트워크를 변경하지 않습니다",
                ModuleCategory.UTILITY, false, 178, 317);
    }
    private void sample(Minecraft client) {
        boolean connected = client.player != null && client.getConnection() != null;
        var info = connected ? client.getConnection().getPlayerInfo(client.player.getUUID()) : null;
        Integer ping = info == null ? null : info.getLatency();
        latency = NetworkStatus.latency(connected, client.hasSingleplayerServer(), ping);
        highLatency = NetworkStatus.highLatency(connected, client.hasSingleplayerServer(), ping);
        performance = "FPS " + Math.max(0, client.getFps());
        textWidth = Math.max(126, Math.max(client.font.width(latency), client.font.width(performance)) + 14);
    }
    @Override public void tick(Minecraft client) {
        Object current = client.getConnection();
        int tick = client.player == null ? 0 : client.player.tickCount;
        if (session != current || tick >= nextSample || tick < nextSample - 10) {
            sample(client);
            session = current;
            nextSample = tick + 10;
        }
    }
    @Override protected void onEnable(Minecraft client) { sample(client); nextSample = 0; }
    @Override protected void onDisable(Minecraft client) { session = null; nextSample = 0; }
    @Override public void renderHud(GuiGraphics graphics, Minecraft client) {
        // Disabled modules still have useful editor previews, without a per-frame player-list lookup.
        if (!enabled()) { latency = "Ping -- | 알 수 없음"; performance = "FPS " + client.getFps(); highLatency = false; }
        graphics.fill(x(), y(), x() + hudWidth(client), y() + hudHeight(), HudTheme.PANEL_BACKGROUND);
        graphics.fill(x(), y(), x() + 3, y() + hudHeight(), 0xFFFF7A18);
        graphics.drawString(client.font, latency, x() + 7, y() + 4, highLatency ? 0xFFFF7664 : 0xFFFFE8C2, false);
        graphics.drawString(client.font, performance, x() + 7, y() + 15, 0xFFFFA45B, false);
    }
    @Override public int hudWidth(Minecraft client) { return textWidth; }
    @Override public int hudHeight() { return 28; }
}
