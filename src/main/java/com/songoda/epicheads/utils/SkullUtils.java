package com.songoda.epicheads.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for working with player heads using modern Paper APIs.
 */
public class SkullUtils {

    /**
     * Get the texture value (base64 encoded) from a player head ItemStack.
     *
     * @param item The ItemStack to get the profile value from
     * @return The base64 encoded texture value, or null if not available
     */
    public static String getProfileValue(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return null;
        }

        ResolvableProfile profile = item.getData(DataComponentTypes.PROFILE);
        if (profile == null) {
            return null;
        }

        return profile.properties().stream()
                .filter(prop -> "textures".equals(prop.getName()))
                .map(prop -> prop.getValue())
                .findFirst()
                .orElse(null);
    }

    /**
     * Compare two player heads to see if they have the same texture.
     */
    public static boolean haveSameProfile(ItemStack item1, ItemStack item2) {
        String profile1 = getProfileValue(item1);
        String profile2 = getProfileValue(item2);

        if (profile1 == null || profile2 == null) {
            return false;
        }

        return profile1.equals(profile2);
    }

    /**
     * Decode a base64 encoded texture value and extract the texture URL/hash.
     */
    public static String getDecodedTexture(String encodedTexture) {
        if (encodedTexture == null || encodedTexture.isEmpty()) {
            return null;
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(encodedTexture), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            JsonObject textures = json.getAsJsonObject("textures");
            if (textures == null) {
                return null;
            }

            JsonObject skin = textures.getAsJsonObject("SKIN");
            if (skin == null) {
                return null;
            }

            String url = skin.get("url").getAsString();
            if (url == null) {
                return null;
            }

            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                return url.substring(lastSlash + 1);
            }

            return url;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
