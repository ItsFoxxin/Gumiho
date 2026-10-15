package dev.gumiho.client.module.settings;

import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;

public final class BooleanSettingModule extends GumihoModule {
    public BooleanSettingModule(
            String id,
            String englishName,
            String koreanName,
            String englishDescription,
            String koreanDescription,
            boolean enabledByDefault
    ) {
        super(id, englishName, koreanName, englishDescription, koreanDescription,
                ModuleCategory.SETTINGS, enabledByDefault, false);
    }
}
