package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.module.movement.BridgeRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BridgeSettingsScreen extends Screen {
    private final Screen parent;
    public BridgeSettingsScreen(Screen parent) {
        super(Component.literal("Building | 건축")); this.parent = parent;
    }
    @Override protected void init() {
        int x = left() + 12, y = top() + 40, w = panelWidth() - 24;
        for (BridgeRules.Mode mode : BridgeRules.Mode.values()) {
            String selected = BridgeRules.mode(GumihoClient.CONFIG.config().bridgeMode) == mode ? "> " : "";
            String help = switch (mode) {
                case BRIDGE -> "Hold blocks and walk toward an edge. Crouches and fills beneath you. Manual Use takes priority. | 블록을 들고 가장자리로 이동하세요.";
                case TOWER -> "Hold blocks and Jump to build beneath your feet during a normal jump. No flying or automatic jumping. | 블록을 들고 점프 키를 누르세요.";
                case WALL -> "Hold blocks + Use and aim at a block face to build up or sideways. Release Use to stop. No automatic cages. | 사용 키를 누르고 놓을 면을 조준하세요.";
            };
            addRenderableWidget(Button.builder(Component.literal(selected + mode.label), b -> {
                GumihoClient.BRIDGE_ASSIST.suspend(minecraft);
                GumihoClient.CONFIG.config().bridgeMode = mode.name();
                GumihoClient.CONFIG.save();
                rebuildWidgets();
            }).bounds(x, y, w, 22).tooltip(Tooltip.create(Component.literal(help))).build());
            y += 26;
        }
        addRenderableWidget(Button.builder(delayLabel(), b -> {
            int delay = BridgeRules.delay(GumihoClient.CONFIG.config().bridgePlaceDelay);
            GumihoClient.CONFIG.config().bridgePlaceDelay = delay < 6 ? 6 : delay < 8 ? 8 : delay < 12 ? 12 : 4;
            GumihoClient.CONFIG.save();
            b.setMessage(delayLabel());
        }).bounds(x, y, w, 22).tooltip(Tooltip.create(Component.literal(
                "Minimum ticks between attempts, including rejected placements. 20 ticks = 1 second. | 설치 시도 간 최소 간격입니다."))).build());
        addRenderableWidget(Button.builder(Component.literal("Done | 완료"), b -> onClose())
                .bounds(width / 2 - 50, top() + panelHeight() - 30, 100, 22).build());
    }
    private Component delayLabel() {
        return Component.literal("Place Delay | 설치 간격: " + BridgeRules.delay(GumihoClient.CONFIG.config().bridgePlaceDelay) + " ticks");
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float tick) {
        super.extractBackground(g, mx, my, tick);
        g.fill(left(), top(), left() + panelWidth(), top() + panelHeight(), 0xF3120E0B);
        g.fill(left(), top(), left() + panelWidth(), top() + 3, 0xFFFF7A18);
        g.centeredText(font, title, width / 2, top() + 10, 0xFFFFE8C2);
        g.centeredText(font, "Hover for help | 마우스를 올려 설명 보기", width / 2, top() + 24, 0xFFFFA45B);
        super.extractRenderState(g, mx, my, tick);
    }
    private int panelWidth() { return Math.min(360, width - 24); }
    private int panelHeight() { return Math.min(194, height - 16); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    @Override public void onClose() { GumihoClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float tick) { }
}
