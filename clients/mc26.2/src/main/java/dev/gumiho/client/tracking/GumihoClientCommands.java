package dev.gumiho.client.tracking;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.gumiho.client.GumihoClient;
import dev.gumiho.client.profile.CombatProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Comparator;
import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class GumihoClientCommands {
    private GumihoClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(literal("gumiho")
                .then(literal("team")
                        .then(literal("add")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(context -> set(context, PlayerGroup.TEAMMATE)))))
                .then(literal("enemy")
                        .then(literal("add")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(context -> set(context, PlayerGroup.ENEMY)))))
                .then(literal("neutral")
                        .then(argument("name", StringArgumentType.word())
                                .executes(context -> set(context, PlayerGroup.NEUTRAL))))
                .then(literal("remove")
                        .then(argument("name", StringArgumentType.word())
                                .executes(GumihoClientCommands::remove)))
                .then(literal("list").executes(GumihoClientCommands::list))
                .then(literal("profile")
                        .then(literal("assist").executes(context -> profile(context, CombatProfile.ASSIST)))
                        .then(literal("enhanced").executes(context -> profile(context, CombatProfile.ENHANCED)))
                        .then(literal("aggressive").executes(context -> profile(context, CombatProfile.AGGRESSIVE)))
                        .then(literal("war").executes(context -> profile(context, CombatProfile.WAR)))
                        .then(literal("custom").executes(context -> profile(context, CombatProfile.CUSTOM)))));
    }

    private static int set(CommandContext<CommandSourceStack> context, PlayerGroup group) {
        String name = StringArgumentType.getString(context, "name");
        GumihoClient.PLAYER_GROUPS.setGroup(name, group);
        context.getSource().sendSuccess(
                () -> Component.literal("GUMIHO: " + name + " → " + group.name().toLowerCase()), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        boolean removed = GumihoClient.PLAYER_GROUPS.remove(name);
        context.getSource().sendSuccess(
                () -> Component.literal("GUMIHO: " + (removed ? "removed " : "no saved entry for ") + name), false);
        return removed ? 1 : 0;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        Map<String, String> entries = GumihoClient.PLAYER_GROUPS.entries();
        if (entries.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("GUMIHO: player lists are empty"), false);
            return 1;
        }
        entries.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> context.getSource().sendSuccess(
                        () -> Component.literal("GUMIHO: " + entry.getKey() + " → " + entry.getValue().toLowerCase()),
                        false));
        return entries.size();
    }

    private static int profile(CommandContext<CommandSourceStack> context, CombatProfile profile) {
        GumihoClient.PROFILES.apply(profile, Minecraft.getInstance());
        context.getSource().sendSuccess(
                () -> Component.literal("GUMIHO profile: " + profile.label()), false);
        return 1;
    }
}
