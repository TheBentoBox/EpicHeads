package com.songoda.epicheads.utils;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
        
        // Get the textures property from the profile
        return profile.properties().stream()
                .filter(prop -> "textures".equals(prop.getName()))
                .map(prop -> prop.getValue())
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Compare two player heads to see if they have the same texture.
     * 
     * @param item1 First ItemStack
     * @param item2 Second ItemStack
     * @return true if both are player heads with the same texture
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
     * 
     * @param encodedTexture Base64 encoded texture JSON
     * @return The texture URL hash (without the full URL prefix), or null if parsing fails
     */
    public static String getDecodedTexture(String encodedTexture) {
        if (encodedTexture == null || encodedTexture.isEmpty()) {
            return null;
        }
        
        try {
            // Decode the base64 string
            String decoded = new String(Base64.getDecoder().decode(encodedTexture), StandardCharsets.UTF_8);
            
            // Parse the JSON
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(decoded);
            JSONObject textures = (JSONObject) json.get("textures");
            if (textures == null) {
                return null;
            }
            
            JSONObject skin = (JSONObject) textures.get("SKIN");
            if (skin == null) {
                return null;
            }
            
            String url = (String) skin.get("url");
            if (url == null) {
                return null;
            }
            
            // Extract just the hash part from the URL
            // Expected format: http://textures.minecraft.net/texture/<hash>
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                return url.substring(lastSlash + 1);
            }
            
            return url;
        } catch (IllegalArgumentException | ParseException e) {
            return null;
        }
    }
}

