package dev.gumiho.client.module.movement;

import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class AutoSprintModule extends GumihoModule {
    public AutoSprintModule() {
        super(
                "auto_sprint",
                "Auto Sprint",
                "자동 달리기",
                "Sprint automatically while moving forward",
                "앞으로 이동할 때 자동으로 달립니다",
                ModuleCategory.MOVEMENT,
                false,
                false
        );
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null || client.screen != null) {
            return;
        }

        if (client.options.keyUp.isDown() && !client.player.isShiftKeyDown()) {
            client.player.setSprinting(true);
        }
    }
}
