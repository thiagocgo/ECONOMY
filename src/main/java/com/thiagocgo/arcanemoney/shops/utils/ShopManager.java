package com.thiagocgo.arcanemoney.shops.utils;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.economy.utils.EconomyManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopManager {
    private final ArcaneMoney plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final Map<String, Shop> shops;

    public ShopManager(ArcaneMoney plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.economyManager = plugin.getEconomyManager();
        this.shops = new HashMap<>();
        loadShops();
        startBeaconSoundTask();
        startParticleTasks();
    }

    private void startParticleTasks() {
        // Task for portal particles (existing)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Shop shop : shops.values()) {
                    Location loc = shop.getLocation().add(0.5, 0.5, 0.5);
                    shop.getWorld().spawnParticle(Particle.PORTAL, loc, 10, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }.runTaskTimer(plugin, 0L, 200L); // Every 10 seconds (200 ticks)

        // Task for end rod particles
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Shop shop : shops.values()) {
                    Location loc = shop.getLocation().add(0.5, 0.5, 0.5);
                    shop.getWorld().spawnParticle(Particle.END_ROD, loc, 5, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 200L); // Every 10 seconds (200 ticks)
    }

    private void startBeaconSoundTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Shop shop : shops.values()) {
                    Location loc = shop.getLocation().add(0.5, 0.5, 0.5);
                    shop.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // Every 5 seconds (100 ticks)
    }

    public void createShop(Player player, Location location, ShulkerBox shulkerBox) {
        String id = getShopId(location);
        UUID owner = player.getUniqueId();
        String ownerName = player.getName();
        String type = player.hasPermission("arcanemoney.admin") ? "admin" : "player";
        Shop shop = new Shop(id, owner, ownerName, type, location);
        shops.put(id, shop);
        saveShop(shop);
        player.sendMessage(configManager.getMessage("shop-place-success")
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ())));
    }

    public void removeShop(Location location) {
        String id = getShopId(location);
        shops.remove(id);
        configManager.getShops().set("shops." + id, null);
        configManager.saveShops();
    }

    public Shop getShop(Location location) {
        String id = getShopId(location);
        Shop shop = shops.get(id);
        if (shop == null) {
            plugin.getLogger().warning("No shop found at location: " + location);
        }
        return shop;
    }

    public List<Shop> getPlayerShops(Player player) {
        List<Shop> playerShops = new ArrayList<>();
        for (Shop shop : shops.values()) {
            if (shop.getOwner().equals(player.getUniqueId()) || shop.getOwnerName().equals("ArcaneMC")) {
                playerShops.add(shop);
            }
        }
        return playerShops;
    }

    public List<Shop> getAllShops() {
        return new ArrayList<>(shops.values());
    }

    public void loadShops() {
        ConfigurationSection shopsSection = configManager.getShops().getConfigurationSection("shops");
        if (shopsSection == null) {
            plugin.getLogger().info("No shops found in shops.yml");
            return;
        }

        for (String id : shopsSection.getKeys(false)) {
            try {
                ConfigurationSection shopSection = shopsSection.getConfigurationSection(id);
                if (shopSection == null) {
                    plugin.getLogger().warning("Invalid shop section for ID: " + id);
                    continue;
                }
                UUID owner = UUID.fromString(shopSection.getString("owner"));
                String ownerName = shopSection.getString("owner_name", "Unknown");
                String type = shopSection.getString("type", "player");
                String[] parts = id.split("_");
                if (parts.length != 4) {
                    plugin.getLogger().warning("Invalid shop ID format: " + id);
                    continue;
                }
                World world = plugin.getServer().getWorld(parts[0]);
                if (world == null) {
                    plugin.getLogger().warning("World not found for shop ID: " + id);
                    continue;
                }
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                Location location = new Location(world, x, y, z);
                Block block = world.getBlockAt(x, y, z);
                if (!(block.getState() instanceof ShulkerBox)) {
                    plugin.getLogger().warning("Block at " + id + " is not a shulker box, skipping shop load");
                    continue;
                }
                Shop shop = new Shop(id, owner, ownerName, type, location);

                ConfigurationSection itemsSection = shopSection.getConfigurationSection("items");
                if (itemsSection != null) {
                    for (String itemTag : itemsSection.getKeys(false)) {
                        ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemTag);
                        if (itemSection == null) {
                            plugin.getLogger().warning("Invalid item section for itemTag: " + itemTag + " in shop: " + id);
                            continue;
                        }
                        double buyPrice = itemSection.getDouble("buy_price");
                        int stock = itemSection.getInt("stock");
                        int quantity = itemSection.getInt("quantity", 1);
                        int maxStock = itemSection.getInt("max_stock", type.equals("admin") ? -1 : configManager.getShopStockLimit());
                        shop.getItems().put(itemTag, new Shop.ShopItem(itemTag, buyPrice, stock, quantity, maxStock));
                    }
                }

                shops.put(id, shop);
                updateShopStock(shop);
                plugin.getLogger().info("Loaded shop: " + id);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load shop: " + id + " - " + e.getMessage());
            }
        }
    }

    public void saveShop(Shop shop) {
        ConfigurationSection shopSection = configManager.getShops().createSection("shops." + shop.getId());
        shopSection.set("owner", shop.getOwner().toString());
        shopSection.set("owner_name", shop.getOwnerName());
        shopSection.set("type", shop.getType());
        ConfigurationSection itemsSection = shopSection.createSection("items");
        for (Map.Entry<String, Shop.ShopItem> entry : shop.getItems().entrySet()) {
            ConfigurationSection itemSection = itemsSection.createSection(entry.getKey());
            itemSection.set("buy_price", entry.getValue().getBuyPrice());
            itemSection.set("stock", entry.getValue().getStock());
            itemSection.set("quantity", entry.getValue().getQuantity());
            itemSection.set("max_stock", entry.getValue().getMaxStock());
            plugin.getLogger().info("Saving item " + entry.getKey() + " with stock " + entry.getValue().getStock() + " for shop " + shop.getId());
        }

        configManager.saveShops();
        plugin.getLogger().info("Saved shop: " + shop.getId());
    }

    public void updateShopStock(Shop shop) {
        if (shop.isAdminShop()) {
            for (Shop.ShopItem item : shop.getItems().values()) {
                item.setStock(-1);
            }
            return;
        }
        // For VIP shops, stock is managed virtually and not tied to shulker inventory
        for (Shop.ShopItem item : shop.getItems().values()) {
            item.setStock(Math.min(item.getStock(), item.getMaxStock()));
        }
    }

    public boolean addItemToShop(Shop shop, Player player, ItemStack item, int quantity, double buyPrice, int stockToAdd) {
        String itemTag = "minecraft:" + item.getType().name().toLowerCase();
        int maxStock = shop.isAdminShop() ? -1 : plugin.getConfigManager().getShopStockLimit(); // Use config value for non-admin shops
        Shop.ShopItem shopItem = shop.getItems().get(itemTag);

        if (shopItem != null) {
            // Update existing item
            shopItem.setBuyPrice(buyPrice);
            shopItem.setQuantity(quantity);
            if (!shop.isAdminShop() && maxStock != -1) { // Only check stock limit if not admin shop and not unlimited
                int newStock = shopItem.getStock() + stockToAdd;
                if (newStock > maxStock) {
                    player.sendMessage(configManager.getMessage("shop-max-stock-exceeded")
                            .replace("%item%", itemTag)
                            .replace("%max_stock%", String.valueOf(maxStock)));
                    plugin.getLogger().warning("Failed to add " + stockToAdd + " of " + itemTag + " to shop " + shop.getId() + ": max stock exceeded (" + maxStock + ")");
                    return false;
                }
                ItemStack toRemove = new ItemStack(item.getType(), stockToAdd);
                if (!player.getInventory().containsAtLeast(toRemove, stockToAdd)) {
                    player.sendMessage(configManager.getMessage("trade-insufficient-items")
                            .replace("%qtd%", String.valueOf(stockToAdd))
                            .replace("%item%", itemTag));
                    plugin.getLogger().warning("Player " + player.getName() + " lacks " + stockToAdd + " of " + itemTag + " for shop " + shop.getId());
                    return false;
                }
                player.getInventory().removeItem(toRemove);
                shopItem.setStock(newStock);
                plugin.getLogger().info("Updated item " + itemTag + " with buyPrice " + buyPrice + ", quantity " + quantity + ", stock " + shopItem.getStock() + " in shop " + shop.getId());
            } else {
                // For admin shops or unlimited stock, just update stock without checking max
                shopItem.setStock(shopItem.getStock() + stockToAdd);
                ItemStack toRemove = new ItemStack(item.getType(), stockToAdd);
                if (!player.getInventory().containsAtLeast(toRemove, stockToAdd)) {
                    player.sendMessage(configManager.getMessage("trade-insufficient-items")
                            .replace("%qtd%", String.valueOf(stockToAdd))
                            .replace("%item%", itemTag));
                    plugin.getLogger().warning("Player " + player.getName() + " lacks " + stockToAdd + " of " + itemTag + " for shop " + shop.getId());
                    return false;
                }
                player.getInventory().removeItem(toRemove);
                plugin.getLogger().info("Updated item " + itemTag + " with buyPrice " + buyPrice + ", quantity " + quantity + ", stock " + shopItem.getStock() + " in shop " + shop.getId());
            }
        } else {
            // Create new item
            if (!shop.isAdminShop() && maxStock != -1) {
                ItemStack toRemove = new ItemStack(item.getType(), stockToAdd);
                if (!player.getInventory().containsAtLeast(toRemove, stockToAdd)) {
                    player.sendMessage(configManager.getMessage("trade-insufficient-items")
                            .replace("%qtd%", String.valueOf(stockToAdd))
                            .replace("%item%", itemTag));
                    plugin.getLogger().warning("Player " + player.getName() + " lacks " + stockToAdd + " of " + itemTag + " for shop " + shop.getId());
                    return false;
                }
                if (stockToAdd > maxStock) {
                    player.sendMessage(configManager.getMessage("shop-max-stock-exceeded")
                            .replace("%item%", itemTag)
                            .replace("%max_stock%", String.valueOf(maxStock)));
                    plugin.getLogger().warning("Failed to add " + stockToAdd + " of " + itemTag + " to shop " + shop.getId() + ": max stock exceeded (" + maxStock + ")");
                    return false;
                }
                shopItem = new Shop.ShopItem(itemTag, buyPrice, stockToAdd, quantity, maxStock);
                shop.getItems().put(itemTag, shopItem);
                player.getInventory().removeItem(toRemove);
                plugin.getLogger().info("Created new shop item " + itemTag + " with stock " + stockToAdd + " in shop " + shop.getId());
            } else {
                shopItem = new Shop.ShopItem(itemTag, buyPrice, stockToAdd, quantity, maxStock);
                shop.getItems().put(itemTag, shopItem);
                ItemStack toRemove = new ItemStack(item.getType(), stockToAdd);
                if (!player.getInventory().containsAtLeast(toRemove, stockToAdd)) {
                    player.sendMessage(configManager.getMessage("trade-insufficient-items")
                            .replace("%qtd%", String.valueOf(stockToAdd))
                            .replace("%item%", itemTag));
                    plugin.getLogger().warning("Player " + player.getName() + " lacks " + stockToAdd + " of " + itemTag + " for shop " + shop.getId());
                    return false;
                }
                player.getInventory().removeItem(toRemove);
                plugin.getLogger().info("Created new shop item " + itemTag + " with stock " + stockToAdd + " in shop " + shop.getId());
            }
        }

        saveShop(shop);
        player.sendMessage(configManager.getMessage("shop-item-added")
                .replace("%item%", itemTag)
                .replace("%quantity%", String.valueOf(quantity))
                .replace("%price%", String.valueOf((long) buyPrice)));
        return true;
    }

    public boolean addItemToShop(Shop shop, Player player, ItemStack item, int quantity, double buyPrice) {
        // For admin shops, use default behavior (no stock to add)
        if (shop.isAdminShop()) {
            return addItemToShop(shop, player, item, quantity, buyPrice, -1);
        }
        // For VIP shops, stock is handled in StockGUI
        return false; // Should not be called directly
    }

    public boolean removeItemFromShop(Shop shop, String itemTag, Player player) {
        Shop.ShopItem item = shop.getItems().get(itemTag);
        if (item == null) {
            player.sendMessage(configManager.getMessage("trade-item-not-found").replace("%item%", itemTag));
            return false;
        }

        if (!shop.isAdminShop()) {
            ItemStack toReturn = new ItemStack(Material.matchMaterial(itemTag.split(":")[1].toUpperCase()), item.getStock());
            if (!player.getInventory().addItem(toReturn).isEmpty()) {
                player.sendMessage(configManager.getMessage("insufficient-inventory-space"));
                return false;
            }
        }

        shop.getItems().remove(itemTag);
        saveShop(shop);
        player.sendMessage(configManager.getMessage("shop-item-removed").replace("%item%", itemTag));
        return true;
    }

    public boolean buyItem(Shop shop, Player player, String itemTag, int quantityMultiplier) {
        Shop.ShopItem item = shop.getItems().get(itemTag);
        if (item == null) {
            player.sendMessage(configManager.getMessage("trade-item-not-found").replace("%item%", itemTag));
            return false;
        }

        int purchaseQuantity = item.getQuantity();
        if (!shop.isAdminShop() && item.getStock() < purchaseQuantity && item.getStock() != -1) {
            player.sendMessage(configManager.getMessage("shop-no-stock"));
            return false;
        }

        double buyPrice = item.getBuyPrice();
        if (buyPrice <= 0) {
            player.sendMessage(configManager.getMessage("shop-no-price"));
            return false;
        }

        if (plugin.getInvestmentManager().isVipOrInvestor(player)) {
            buyPrice *= (1 - configManager.getConfig().getDouble("trade.vip-discount", 0.1));
        }
        double totalCost = Math.floor(buyPrice);

        if (!economyManager.processTransaction(player, -totalCost, "Purchase from shop " + shop.getId())) {
            return false;
        }

        // Play achievement sound and spawn firework particles for buyer
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);

        if (!shop.isAdminShop()) {
            Player owner = plugin.getServer().getPlayer(shop.getOwner());
            if (owner != null) {
                economyManager.processTransaction(owner, totalCost, "Sale to " + player.getName() + " in shop " + shop.getId());
                owner.sendMessage(configManager.getMessage("shop-buy-notification")
                        .replace("%player%", player.getName())
                        .replace("%item%", itemTag)
                        .replace("%stock%", String.valueOf(item.getStock() - purchaseQuantity)));
                plugin.getBossBarManager().updateBossBar(owner);
            }
            item.setStock(item.getStock() - purchaseQuantity);
            if (item.getStock() <= 0) {
                if (owner != null) {
                    owner.sendMessage(configManager.getMessage("shop-stock-depleted")
                            .replace("%item%", itemTag));
                }
            }
        }

        ItemStack toAdd = new ItemStack(Material.matchMaterial(itemTag.split(":")[1].toUpperCase()), purchaseQuantity);
        if (!player.getInventory().addItem(toAdd).isEmpty()) {
            player.sendMessage(configManager.getMessage("insufficient-inventory-space"));
            economyManager.processTransaction(player, totalCost, "Refund for failed purchase from shop " + shop.getId());
            if (!shop.isAdminShop()) {
                Player owner = plugin.getServer().getPlayer(shop.getOwner());
                if (owner != null) {
                    economyManager.processTransaction(owner, -totalCost, "Refund for failed sale to " + player.getName());
                    plugin.getBossBarManager().updateBossBar(owner);
                }
                item.setStock(item.getStock() + purchaseQuantity);
            }
            saveShop(shop);
            return false;
        }

        saveShop(shop);
        player.sendMessage(configManager.getMessage("shop-buy-success")
                .replace("%quantity%", String.valueOf(purchaseQuantity))
                .replace("%item%", itemTag)
                .replace("%price%", String.valueOf((long) totalCost))
                .replace("%currency%", economyManager.getCurrencyName()));
        plugin.getBossBarManager().updateBossBar(player);
        return true;
    }

    public boolean withdrawItems(Shop shop, Player player, String itemTag, int quantity) {
        if (!shop.getOwner().equals(player.getUniqueId()) && !player.hasPermission("arcanemoney.admin")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return false;
        }

        Shop.ShopItem item = shop.getItems().get(itemTag);
        if (item == null || (!shop.isAdminShop() && item.getStock() < quantity)) {
            player.sendMessage(configManager.getMessage("shop-no-stock"));
            return false;
        }

        ItemStack toWithdraw = new ItemStack(Material.matchMaterial(itemTag.split(":")[1].toUpperCase()), quantity);
        if (!player.getInventory().addItem(toWithdraw).isEmpty()) {
            player.sendMessage(configManager.getMessage("insufficient-inventory-space"));
            return false;
        }

        if (!shop.isAdminShop()) {
            item.setStock(item.getStock() - quantity);
        }
        saveShop(shop);
        player.sendMessage(configManager.getMessage("shop-withdraw-items")
                .replace("%quantity%", String.valueOf(quantity))
                .replace("%item%", itemTag));
        return true;
    }

    private String getShopId(Location location) {
        return location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
    }

    public Shop getShopById(String id) {
        Shop shop = shops.get(id);
        if (shop == null) {
            plugin.getLogger().warning("No shop found for ID: " + id);
        }
        return shop;
    }
}