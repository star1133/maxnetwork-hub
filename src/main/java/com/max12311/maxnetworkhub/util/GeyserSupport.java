package com.max12311.maxnetworkhub.util;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * GeyserMC / Floodgate detection helpers.
 * Floodgate Bedrock players have UUID MSB == 0 (first two UUID fields are all zeroes).
 */
public final class GeyserSupport {

    private GeyserSupport() {}

    /** Returns true if this UUID belongs to a Bedrock player via Floodgate. */
    public static boolean isBedrockPlayer(UUID uuid) {
        return uuid.getMostSignificantBits() == 0L;
    }

    public static boolean isBedrockPlayer(Player player) {
        return isBedrockPlayer(player.getUniqueId());
    }

    /**
     * Strip the Geyser name prefix (e.g. ".") from a player name.
     * Returns the raw Java name unchanged if no prefix found.
     */
    public static String stripPrefix(String name, String prefix) {
        if (prefix == null || prefix.isEmpty()) return name;
        if (name.startsWith(prefix)) return name.substring(prefix.length());
        return name;
    }
}
