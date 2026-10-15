package dev.gumiho.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class GumihoCategoryButton extends AbstractButton {
    private static final int ORANGE = 0xFFFF7A18;
    private static final int CREAM = 0xFFFFE8C2;

    private final BooleanSupplier selected;
    private final Runnable action;

    public GumihoCategoryButton(int x, int y, int width, int height, Component message, BooleanSupplier selected, Runnable action) {
        super(x, y, width, height, message);
        this.selected = selected;
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean activeCategory = selected.getAsBoolean();
        int background = activeCategory ? 0xEE4A240C : (isHoveredOrFocused() ? 0xDD302116 : 0xC91B1714);
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
        if (activeCategory) {
            graphics.fill(getX(), getY(), getX() + 4, getY() + getHeight(), ORANGE);
        }
        graphics.drawString(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + 10,
                getY() + (getHeight() - 8) / 2,
                activeCategory ? CREAM : 0xFFD7C8BA
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
