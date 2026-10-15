package dev.gumiho.client.module.utility;
import dev.gumiho.client.module.SnapshotHudModule;
import dev.gumiho.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
public final class EffectsHudModule extends SnapshotHudModule {
    public EffectsHudModule() {
        super("effects_hud","Effect Timers","효과 타이머",
                "Three soonest-expiring effects, permanent effect indicator, and additional-effect count",
                "곧 끝나는 효과 3개, 영구 효과와 추가 효과 수 표시",
                ModuleCategory.UTILITY,true,178,140,"Effects | 효과",4);
    }
    @Override protected List<Row> readRows(Minecraft client) {
        if(client.player==null) return List.of();
        var effects=client.player.getActiveEffects().stream()
                .sorted(Comparator.comparingInt((MobEffectInstance e) -> e.isInfiniteDuration() ? Integer.MAX_VALUE : e.getDuration())
                        .thenComparing(e -> e.getEffect().value().getDisplayName().getString()))
                .toList();
        var rows=new ArrayList<Row>();
        for(var effect:effects.stream().limit(3).toList()) {
            String suffix=" "+(effect.getAmplifier()+1)+"  "+HudValues.duration(effect.getDuration());
            String name=effect.getEffect().value().getDisplayName().getString();
            // Reserve the timer first; trim names by rendered width, never UTF-16 character count.
            name=fit(client,name,MAX_WIDTH-14-client.font.width(suffix));
            rows.add(new Row(name+suffix,!effect.isInfiniteDuration()&&effect.getDuration()<=200 ? 0xFFFFA45B : 0xFFD7BDA8));
        }
        if(effects.size()>3) rows.add(new Row("+"+(effects.size()-3)+" more | 추가 효과",0xFF9E9187));
        return rows;
    }
}
