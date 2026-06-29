package com.roguelike.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

}
