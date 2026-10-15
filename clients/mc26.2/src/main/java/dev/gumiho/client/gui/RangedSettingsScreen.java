package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public final class RangedSettingsScreen extends Screen {
    private final Screen parent;
    private int page;
    public RangedSettingsScreen(Screen parent) { super(Component.literal("Ranged Settings | 원거리 설정")); this.parent = parent; }
    @Override protected void init() {
        var c = GumihoClient.CONFIG.config();
        int x = left() + 14, y = top() + 48, w = panelWidth() - 28;
        if (page == 0) {
            toggle(x, y, w, "Bows | 활", () -> c.rangedBows, v -> c.rangedBows = v);
            toggle(x, y + 28, w, "Crossbows | 쇠뇌", () -> c.rangedCrossbows, v -> c.rangedCrossbows = v);
            toggle(x, y + 56, w, "Thrown Tridents | 삼지창 투척", () -> c.rangedTridents, v -> c.rangedTridents = v);
            toggle(x, y + 84, w, "Target Lead | 이동 예측", () -> c.rangedLead, v -> c.rangedLead = v);
            toggle(x, y + 112, w, "Check Flight Path | 비행 경로 확인", () -> c.rangedCheckPath, v -> c.rangedCheckPath = v);
        } else {
            slider(x, y, w, "Range | 거리", 16, 96, c.bowAimRange, "m", v -> c.bowAimRange = v);
            slider(x, y + 28, w, "Full FOV | 전체 시야각", 10, 180, c.rangedFov, "°", v -> c.rangedFov = v);
            slider(x, y + 56, w, "Aim Speed | 조준 속도", 0.5, 10, c.bowAimSmoothness, "°/tick", v -> c.bowAimSmoothness = v);
            slider(x, y + 84, w, "Aim Ease | 조준 완화", 50, 400, c.rangedAimResponse * 1000, "ms", v -> c.rangedAimResponse = v / 1000);
        }
        int footer = top() + panelHeight() - 30;
        addRenderableWidget(Button.builder(Component.literal(page == 0 ? "Limits >" : "< Weapons"), b -> { page = 1 - page; rebuildWidgets(); })
                .bounds(x, footer, 100, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done | 완료"), b -> onClose()).bounds(x + w - 100, footer, 100, 22).build());
    }
    private void toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")), b -> {
            set.accept(!get.getAsBoolean()); b.setMessage(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")));
        }).bounds(x, y, w, 24).build());
    }
    private void slider(int x, int y, int w, String name, double min, double max, double value, String suffix, DoubleConsumer set) {
        addRenderableWidget(new GumihoSettingsScreen.NumericSlider(x, y, w, 24, min, max, value,
                v -> Component.literal(name + ": " + String.format(Locale.ROOT, "%.1f", v) + suffix), set));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        g.fill(left(), top(), left() + panelWidth(), top() + panelHeight(), 0xF3120E0B);
        g.fill(left(), top(), left() + panelWidth(), top() + 3, 0xFFFF7A18);
        g.centeredText(font, title.getString() + "  " + (page + 1) + "/2", width / 2, top() + 10, 0xFFFFE8C2);
        String hint = page == 0 ? "Manual fire. Riptide/fireworks excluded." : "Global target range/FOV and friend rules still apply.";
        g.centeredText(font, font.plainSubstrByWidth(hint, panelWidth() - 24), width / 2, top() + 26, 0xFFFFA45B);
        super.extractRenderState(g, mx, my, partial);
    }
    private int panelWidth() { return Math.min(420, width - 24); }
    private int panelHeight() { return Math.min(236, height - 24); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    @Override public void onClose() { GumihoClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) { }
}
