package dev.gumiho.client.mixin;
import dev.gumiho.client.module.visual.NoWeatherModule;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=WeatherEffectRenderer.class, remap=false)
public abstract class WeatherRendererMixin {
    @Inject(method="extractRenderState", at=@At("HEAD"), cancellable=true, remap=false)
    private void gumiho$hidePrecipitation(ClientLevel level,float partial,Vec3 camera,WeatherRenderState state,CallbackInfo ci) {
        if(NoWeatherModule.hidden()) { state.reset(); ci.cancel(); }
    }
}
