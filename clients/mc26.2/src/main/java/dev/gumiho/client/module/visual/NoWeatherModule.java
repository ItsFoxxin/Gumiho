package dev.gumiho.client.module.visual;
import dev.gumiho.client.module.GumihoModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
/** Renderer-only suppression: never changes client or server rain/thunder state. */
public final class NoWeatherModule extends GumihoModule {
    private static volatile boolean hidden;
    public NoWeatherModule() {
        super("no_weather","Hide Precipitation","강수 숨기기",
                "Hide vanilla rain/snow, splashes and ambient rain sounds. Sky, lightning, thunder and gameplay stay unchanged",
                "비와 눈, 물방울과 빗소리만 숨깁니다. 하늘, 번개, 천둥과 게임 규칙은 유지됩니다",
                ModuleCategory.VISUAL,false,false);
    }
    public static boolean hidden() { return hidden; }
    @Override protected void onEnable(Minecraft client) { hidden=true; }
    @Override protected void onDisable(Minecraft client) { hidden=false; }
}
