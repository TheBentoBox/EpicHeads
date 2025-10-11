package com.songoda.epicheads.head;

import com.songoda.core.utils.TextUtils;
import com.songoda.epicheads.EpicHeads;
import com.songoda.epicheads.settings.Settings;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.DataComponentTypes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Head {
    private int id;
    private String name = null;
    private String url = null;
    private final boolean local;
    private double averageRating = 0.0;
    private int totalRatings = 0;

    private Category category;

    public Head(String name, String url, Category category, boolean local) {
        this.name = name;
        this.url = url;
        this.category = category;
        this.local = local;
        this.averageRating = 0.0;
        this.totalRatings = 0;
    }

    public Head(int id, String name, String url, Category category, boolean local) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.category = category;
        this.local = local;
        this.averageRating = 0.0;
        this.totalRatings = 0;
    }

    public Head(int id, boolean local) {
        this.id = id;
        this.local = local;
        this.averageRating = 0.0;
        this.totalRatings = 0;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @deprecated Use {@link #setUrl(String)} instead.
     */
    @Deprecated
    public void setURL(String url) {
        setUrl(url);
    }

    /**
     * @deprecated Use {@link #getUrl()} instead.
     */
    @Deprecated
    public String getURL() {
        return getUrl();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        if (this.url == null) {
            return "d23eaefbd581159384274cdbbd576ced82eb72423f2ea887124f9ed33a6872c";
        }
        return this.url;
    }

    public Category getCategory() {
        return this.category;
    }

    public boolean isLocal() {
        return this.local;
    }

    public double getAverageRating() {
        return this.averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalRatings() {
        return this.totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public String getRatingDisplay() {
        if (this.totalRatings == 0) {
            return "☆☆☆☆☆";
        }
        int fullStars = (int) Math.round(this.averageRating);
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= fullStars) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        return stars.toString();
    }

    public ItemStack asItemStack() {
        return asItemStack(false, false);
    }

    public ItemStack asItemStack(boolean favorite) {
        return asItemStack(favorite, false);
    }

    public ItemStack asItemStack(boolean favorite, boolean free) {
        ItemStack skull = createSkullFromUrl(getUrl());
        ItemMeta meta = skull.getItemMeta();
        meta.setDisplayName(getHeadItemName(favorite));
        meta.setLore(getHeadItemLore(free));

        skull.setItemMeta(meta);
        return skull;
    }

    public String getHeadItemName(boolean favorite) {
        return TextUtils.formatText((favorite ? "&6⭐ " : "") + "&9" + this.name);
    }

    public List<String> getHeadItemLore(boolean free) {
        EpicHeads plugin = EpicHeads.getInstance();
        double cost = Settings.HEAD_COST.getDouble();
        List<String> lore = new ArrayList<>();
        
        if (!free) {
            if (cost != 0) {
                String finalCost = Settings.ECONOMY_PLUGIN.getString().equalsIgnoreCase("item") ? cost + " " + Settings.ITEM_TOKEN_TYPE.getString() : String.valueOf(cost); 
                lore.add(plugin.getLocale().getMessage("general.head.cost").processPlaceholder("cost", finalCost).toText());
            } else {
                lore.add(plugin.getLocale().getMessage("general.head.cost_free").toText());
            }
        }
        return lore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Head head = (Head) o;
        return this.id == head.id && this.local == head.local;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.local);
    }

    @Override
    public String toString() {
        return "Head{" +
                "id=" + this.id +
                ", name='" + this.name + '\'' +
                ", URL='" + this.url + '\'' +
                ", local=" + this.local +
                ", category=" + this.category +
                '}';
    }

    private ItemStack createSkullFromUrl(String urlOrTextureHash) {
        String textureValue;

        // Handle direct texture values.
        if (urlOrTextureHash.matches("[A-Za-z0-9+/-]{100,}={0,3}")) {
            textureValue = urlOrTextureHash;

        // Handle URLs.
        } else {
            // If the URL doesn't start with http:// or https://, add
            // https://textures.minecraft.net/texture/ to resolve using the
            // Minecraft texture server.
            if (!urlOrTextureHash.startsWith("http://") && !urlOrTextureHash.startsWith("https://")) {
                urlOrTextureHash = "https://textures.minecraft.net/texture/" + urlOrTextureHash;
            }

            // Create a raw texture value and encode it to a base64 string.
            String rawTextureValue = "{\"textures\":{\"SKIN\":{\"url\":\"" + urlOrTextureHash + "\"}}}";
            textureValue = Base64.getEncoder().encodeToString(rawTextureValue.getBytes());
        }

        ProfileProperty property = new ProfileProperty("textures", textureValue, "");
        ResolvableProfile profile = ResolvableProfile.resolvableProfile().addProperty(property).build();

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        item.setData(DataComponentTypes.PROFILE, profile);
        return item;
    }
}
