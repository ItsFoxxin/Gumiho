package dev.gumiho.client.mixin;
import dev.gumiho.client.module.visual.NoWeatherModule;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** NeoForge uses named runtime methods; no intermediary/Fabric remapping is needed. */
@Mixin(value=LevelRenderer.class,remap=false)
public abstract class WeatherRendererMixin {
    @Inject(method={"renderSnowAndRain","tickRain"},at=@At("HEAD"),cancellable=true,remap=false)
    private void gumiho$hidePrecipitation(CallbackInfo ci) {
        if(NoWeatherModule.hidden()) ci.cancel();
    }
}
