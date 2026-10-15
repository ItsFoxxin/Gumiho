package dev.gumiho.client.module.visual;

import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class FullbrightModule extends GumihoModule {
    private Double previousGamma;

    public FullbrightModule() {
        super(
                "fullbright",
                "Max Brightness",
                "최대 밝기",
                "Use maximum vanilla brightness, not night vision. Formerly Fullbright; restores your previous setting",
                "기본 밝기를 최대로 유지하며 해제하면 이전 설정을 복원합니다",
                ModuleCategory.VISUAL,
                false,
                false
        );
    }

    @Override
    protected void onEnable(Minecraft client) {
        previousGamma = client.options.gamma().get();
        client.options.gamma().set(1.0D);
    }

    @Override
    public void tick(Minecraft client) {
        if (previousGamma == null) {
            previousGamma = client.options.gamma().get();
        }
        if (client.options.gamma().get() < 1.0D) {
            previousGamma = client.options.gamma().get();
            client.options.gamma().set(1.0D);
        }
    }

    @Override
    protected void onDisable(Minecraft client) {
        if (previousGamma != null) {
            if (client.options.gamma().get() == 1.0D) client.options.gamma().set(previousGamma);
            previousGamma = null;
        }
    }
}
