package com.roguelike.mob.internal;

import java.util.Locale;

final class MobCombatScript {
    private MobCombatScript() {
    }

    static boolean actionEnabled(String script, String action, boolean fallbackWhenBlank) {
        if (script == null || script.isBlank()) return fallbackWhenBlank;
        String normalizedAction = normalize(action);
        boolean foundEnable = false;
        for (String rawLine : script.split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) continue;
            String normalizedLine = normalize(line);
            if (normalizedLine.equals("disable " + normalizedAction) || normalizedLine.equals("without " + normalizedAction)) {
                return false;
            }
            if (normalizedLine.equals(normalizedAction)
                    || normalizedLine.equals("action " + normalizedAction)
                    || normalizedLine.equals("use " + normalizedAction)) {
                foundEnable = true;
            }
        }
        return foundEnable;
    }

    private static String stripComment(String line) {
        int index = line.indexOf('#');
        return index < 0 ? line : line.substring(0, index);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
