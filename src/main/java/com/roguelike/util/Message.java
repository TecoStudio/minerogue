package com.roguelike.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Message {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    public static Component toComponent(String text) {
        if (text == null) return Component.empty();
        if (text.indexOf('§') >= 0) {
            return SECTION.deserialize(text);
        }
        return LEGACY.deserialize(text);
    }

    public static void send(CommandSender sender, String text) {
        sender.sendMessage(toComponent(text));
    }

    public static void send(Player player, String text) {
        player.sendMessage(toComponent(text));
    }

    public static void action(Player player, String text) {
        player.sendActionBar(toComponent(text));
    }

    public static void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(toComponent(title), toComponent(subtitle),
                Title.Times.times(java.time.Duration.ofMillis(fadeIn * 50L),
                        java.time.Duration.ofMillis(stay * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L))));
    }
}
