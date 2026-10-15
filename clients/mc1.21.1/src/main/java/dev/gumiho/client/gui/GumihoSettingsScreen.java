package dev.gumiho.client.gui;

import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.config.GumihoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public final class GumihoSettingsScreen extends Screen {
    private static final int ORANGE = 0xFFFF7A18;
    private static final int CREAM = 0xFFFFE8C2;
    private final Screen parent;

    public GumihoSettingsScreen(Screen parent) {
        super(Component.literal("GUMIHO Settings | 설정"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        GumihoConfig config = GumihoClient.CONFIG.config();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int sliderLeft = left + 14;
        int sliderWidth = panelWidth - 28;
        int sliderTop = top + 48;
        addRenderableWidget(new NumericSlider(sliderLeft, sliderTop, sliderWidth, 24, 16, 96, config.bowAimRange,
                value -> label("Ranged Range | 원거리 거리", value, "m"), value -> config.bowAimRange = value));
        addRenderableWidget(new NumericSlider(sliderLeft, sliderTop + 28, sliderWidth, 24, 0.5, 10, config.bowAimSmoothness,
                value -> label("Ranged Speed | 원거리 속도", value, "°/tick"), value -> config.bowAimSmoothness = value));
        addRenderableWidget(new NumericSlider(sliderLeft, sliderTop + 56, sliderWidth, 24, 2, 20, config.autoTotemHealth,
                value -> label("Auto Totem | 자동 토템", value / 2.0D, " hearts"), value -> config.autoTotemHealth = value));
        addRenderableWidget(new NumericSlider(sliderLeft, sliderTop + 84, sliderWidth, 24, 1, 19, config.autoEatHunger,
                value -> label("Auto Eat Hunger | 자동 먹기", Math.round(value), "/20"), value -> config.autoEatHunger = (int) Math.round(value)));
        addRenderableWidget(new NumericSlider(sliderLeft, sliderTop + 112, sliderWidth, 24, 2, 20, config.lowHealthWarning,
                value -> label("Low Health Warning | 체력 경고", value / 2.0D, " hearts"), value -> config.lowHealthWarning = value));
        addRenderableWidget(Button.builder(Component.literal("Done | 완료"), clicked -> onClose())
                .bounds(width / 2 - 50, top + panelHeight - 30, 100, 22).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF3120E0B);
        graphics.fill(left, top, left + panelWidth, top + 3, ORANGE);
        graphics.drawCenteredString(font, "GUMIHO Settings | 구미호 설정", width / 2, top + 10, CREAM);
        graphics.drawCenteredString(font, "Changes apply immediately | 변경 사항은 즉시 적용됩니다", width / 2, top + 22, 0xFFFFA45B);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        GumihoClient.CONFIG.save();
        minecraft.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    private int panelWidth() { return Math.min(380, width - 24); }
    private int panelHeight() { return Math.min(236, height - 24); }

    private static Component label(String name, double value, String suffix) {
        return Component.literal(name + ": " + String.format(Locale.ROOT, "%.1f", value) + suffix);
    }

    private static Component label(String name, long value, String suffix) {
        return Component.literal(name + ": " + value + suffix);
    }

    static final class NumericSlider extends AbstractSliderButton {
        private final double minimum;
        private final double maximum;
        private final DoubleFunction<Component> label;
        private final DoubleConsumer setter;

        NumericSlider(int x, int y, int width, int height, double minimum, double maximum, double initial,
                              DoubleFunction<Component> label, DoubleConsumer setter) {
            super(x, y, width, height, Component.empty(), (initial - minimum) / (maximum - minimum));
            this.minimum = minimum;
            this.maximum = maximum;
            this.label = label;
            this.setter = setter;
            updateMessage();
        }

        private double actualValue() {
            return minimum + value * (maximum - minimum);
        }

        @Override protected void updateMessage() { setMessage(label.apply(actualValue())); }
        @Override protected void applyValue() { setter.accept(actualValue()); }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int background = isHoveredOrFocused() ? 0xEF3A2C24 : 0xE92B211B;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
            graphics.fill(getX(), getY(), getX() + 2, getY() + height, isHoveredOrFocused() ? ORANGE : 0xFF674A38);

            int trackLeft = getX() + 8;
            int trackRight = getX() + width - 8;
            int trackY = getY() + height - 5;
            int knobX = trackLeft + (int) Math.round(value * (trackRight - trackLeft));
            graphics.fill(trackLeft, trackY, trackRight, trackY + 2, 0xFF5A4639);
            graphics.fill(trackLeft, trackY, knobX, trackY + 2, ORANGE);
            graphics.fill(knobX - 2, trackY - 2, knobX + 2, trackY + 4, 0xFFFFA45B);

            Minecraft client = Minecraft.getInstance();
            graphics.drawCenteredString(client.font, getMessage(), getX() + width / 2, getY() + 5, CREAM);
        }
    }
}
