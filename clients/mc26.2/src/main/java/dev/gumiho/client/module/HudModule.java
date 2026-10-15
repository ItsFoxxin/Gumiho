package dev.gumiho.client.module;

import net.minecraft.client.Minecraft;

public abstract class HudModule extends GumihoModule {
    private int x;
    private int y;
    private final int defaultX;
    private final int defaultY;
    private int displayX, displayY;
    private boolean layoutPrepared;
    private double scale = 1;

    protected HudModule(
            String id,
            String englishName,
            String koreanName,
            String englishDescription,
            String koreanDescription,
            boolean enabledByDefault,
            int defaultX,
            int defaultY
    ) {
        this(
                id,
                englishName,
                koreanName,
                englishDescription,
                koreanDescription,
                ModuleCategory.VISUAL,
                enabledByDefault,
                defaultX,
                defaultY
        );
    }

    protected HudModule(
            String id,
            String englishName,
            String koreanName,
            String englishDescription,
            String koreanDescription,
            ModuleCategory category,
            boolean enabledByDefault,
            int defaultX,
            int defaultY
    ) {
        super(
                id,
                englishName,
                koreanName,
                englishDescription,
                koreanDescription,
                category,
                enabledByDefault,
                false
        );
        this.x = defaultX;
        this.y = defaultY;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
    }

    public abstract int hudWidth(Minecraft client);

    public abstract int hudHeight();

    public final int x() {
        return layoutPrepared ? displayX : x;
    }

    public final int y() {
        return layoutPrepared ? displayY : y;
    }

    public final void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        layoutPrepared = false;
    }

    public final void resetPosition() {
        this.x = defaultX;
        this.y = defaultY;
        layoutPrepared = false;
    }

    public final double scale() { return scale; }
    public final void setScale(double value) {
        scale = dev.gumiho.client.gui.HudSizing.scale(value);
        layoutPrepared = false;
    }
    public final int scaledWidth(Minecraft client) {
        return dev.gumiho.client.gui.HudSizing.extent(hudWidth(client),scale);
    }
    public final int scaledHeight() {
        return dev.gumiho.client.gui.HudSizing.extent(hudHeight(),scale);
    }
    public final void renderScaled(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft client) {
        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(x(),y());
            pose.scale((float)scale,(float)scale);
            pose.translate(-x(),-y());
            renderHud(graphics,client);
        } finally { pose.popMatrix(); }
    }

    public final int preferredX() { return x; }
    public final int preferredY() { return y; }

    public final void prepareLayout(Minecraft client, int width, int height) {
        displayX = dev.gumiho.client.gui.HudLayout.visibleOrigin(x, scaledWidth(client), width);
        displayY = dev.gumiho.client.gui.HudLayout.visibleOrigin(y, scaledHeight(), height);
        layoutPrepared = true;
    }
}
