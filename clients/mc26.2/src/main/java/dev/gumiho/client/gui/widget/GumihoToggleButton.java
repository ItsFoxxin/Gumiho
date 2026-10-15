package dev.gumiho.client.gui.widget;

import dev.gumiho.client.module.GumihoModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class GumihoToggleButton extends AbstractButton {
    private final GumihoModule module;
    private final Runnable action;

    public GumihoToggleButton(int x, int y, int width, int height, GumihoModule module, Runnable action) {
        super(x, y, width, height, Component.empty());
        this.module = module;
        this.action = action;
    }

    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean enabled = module.enabled();
        int background = enabled ? 0xFFFF7A18 : 0xFF342C27;
        if (isHoveredOrFocused()) {
            background = enabled ? 0xFFFF9342 : 0xFF4A3B32;
        }
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
        String state = enabled ? "ON | 켜짐" : "OFF | 꺼짐";
        graphics.centeredText(
                Minecraft.getInstance().font,
                state,
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                enabled ? 0xFF1A100A : 0xFFFFE8C2
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
