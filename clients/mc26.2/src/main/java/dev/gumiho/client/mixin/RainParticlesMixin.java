package dev.gumiho.client.mixin;
import dev.gumiho.client.module.visual.NoWeatherModule;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ClientLevel.class,remap=false)
public abstract class RainParticlesMixin {
    @Inject(method="tickWeatherEffects",at=@At("HEAD"),cancellable=true,remap=false)
    private void gumiho$hideSplashes(CallbackInfo ci) {
        if(NoWeatherModule.hidden()) ci.cancel();
    }
}
