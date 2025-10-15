package com.thiagocgo.arcanemoney.shops.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Shop {
    private final String id; // world_x_y_z
    private final UUID owner;
    private final String ownerName;
    private final String type; // "player" or "admin"
    private final Location location;
    private final Map<String, ShopItem> items; // itemTag -> ShopItem

    public Shop(String id, UUID owner, String ownerName, String type, Location location) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.type = type;
        this.location = location;
        this.items = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getType() {
        return type;
    }

    public World getWorld() {
        return location.getWorld();
    }

    public int getX() {
        return location.getBlockX();
    }

    public int getY() {
        return location.getBlockY();
    }

    public int getZ() {
        return location.getBlockZ();
    }

    public Map<String, ShopItem> getItems() {
        return items;
    }

    public ShulkerBox getShulkerBox() {
        Block block = location.getBlock();
        if (!(block.getState() instanceof ShulkerBox)) {
            return null;
        }
        return (ShulkerBox) block.getState();
    }

    public Inventory getInventory() {
        ShulkerBox shulkerBox = getShulkerBox();
        if (shulkerBox == null) {
            return null;
        }
        return shulkerBox.getInventory();
    }

    public boolean isAdminShop() {
        return type.equals("admin");
    }

    public Location getLocation() {
        return location;
    }

    public static class ShopItem {
        private final String itemTag;
        private double buyPrice; // Price for the entire quantity (not per unit)
        private int stock;
        private int quantity; // Quantity per purchase
        private final int maxStock;

        public ShopItem(String itemTag, double buyPrice, int stock, int quantity, int maxStock) {
            this.itemTag = itemTag;
            this.buyPrice = buyPrice;
            this.stock = stock;
            this.quantity = Math.max(1, quantity);
            this.maxStock = maxStock;
        }

        public String getItemTag() {
            return itemTag;
        }

        public double getBuyPrice() {
            return buyPrice;
        }

        public void setBuyPrice(double buyPrice) {
            this.buyPrice = buyPrice;
        }

        public int getStock() {
            return stock;
        }

        public void setStock(int stock) {
            if (maxStock == -1) {
                this.stock = Math.max(0, stock); // Allow unlimited stock, but prevent negative
            } else {
                this.stock = Math.max(0, Math.min(stock, maxStock)); // Cap at maxStock
            }
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = Math.max(1, quantity);
        }

        public int getMaxStock() {
            return maxStock;
        }
    }
}