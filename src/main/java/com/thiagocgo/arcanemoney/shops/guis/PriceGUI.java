package com.thiagocgo.arcanemoney.shops.guis;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.shops.utils.Shop;
import com.thiagocgo.arcanemoney.shops.utils.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PriceGUI {
    private static final Map<String, Map<String, Object>> guiStates = new HashMap<>();
    private static ArcaneMoney plugin;

    public static void setPlugin(ArcaneMoney pluginInstance) {
        plugin = pluginInstance;
    }

    public static void open(Player player, Shop shop, ItemStack item, int quantity, ArcaneMoney plugin) {
        if (PriceGUI.plugin == null) {
            PriceGUI.plugin = plugin;
        }

        // Clear any existing state for the player
        clearState(player);

        String title = ChatColor.LIGHT_PURPLE + "Loja ArcaneMC - Preco [" + shop.getId() + "]";
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        // Centralize buttons in columns 4, 5, 6 (slots 12, 13, 14 for increment, 21, 22, 23 for decrement)
        int[] slots = {12, 13, 14}; // Centered in 9-column grid
        String[] labels = {"Unidade", "Dezena", "Centena"};
        int[] increments = {1, 10, 100};

        // Fill non-interactive slots with gray stained glass panes
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(ChatColor.RESET + "");
        border.setItemMeta(borderMeta);
        for (int i = 0; i < 27; i++) {
            if (i != 4 && i != 16 && i != 18 && i != 26 && !contains(slots, i) && !contains(new int[]{21, 22, 23}, i)) {
                inventory.setItem(i, border);
            }
        }

        for (int i = 0; i < slots.length; i++) {
            ItemStack button = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "+" + labels[i]);
            button.setItemMeta(meta);
            inventory.setItem(slots[i], button);

            button = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "-" + labels[i]);
            button.setItemMeta(meta);
            inventory.setItem(slots[i] + 9, button); // Decrement buttons below (slots 21, 22, 23)
        }

        ItemStack confirm = new ItemStack(Material.EMERALD);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirmar");
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(16, confirm);

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Voltar");
        backButton.setItemMeta(backMeta);
        inventory.setItem(18, backButton);

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Fechar");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(26, closeButton);

        ItemStack itemDisplay = new ItemStack(item.getType(), quantity);
        ItemMeta displayMeta = itemDisplay.getItemMeta();
        displayMeta.setDisplayName(ChatColor.YELLOW + item.getType().name().toLowerCase());
        displayMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Quantidade por compra: " + quantity,
                ChatColor.GRAY + "Preco por " + quantity + ": 0"
        ));
        itemDisplay.setItemMeta(displayMeta);
        inventory.setItem(4, itemDisplay);

        String playerUUID = player.getUniqueId().toString();
        Map<String, Object> state = new HashMap<>();
        state.put("item", item.clone());
        state.put("quantity", quantity);
        state.put("buyPrice", 0.0);
        state.put("shopId", shop.getId());
        guiStates.put(playerUUID, state);

        player.openInventory(inventory);
        plugin.getLogger().info("Opened PriceGUI for " + player.getName() + " for item " + item.getType().name() + " with quantity " + quantity + ", title: " + title);
    }

    public static void handleClick(Player player, Shop shop, Inventory inventory, ItemStack clickedItem, int slot, ArcaneMoney plugin) {
        if (PriceGUI.plugin == null) {
            PriceGUI.plugin = plugin;
        }

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            plugin.getLogger().warning("Clicked item is null or air in PriceGUI for " + player.getName() + ", slot: " + slot);
            return;
        }

        String playerUUID = player.getUniqueId().toString();
        Map<String, Object> state = guiStates.get(playerUUID);
        if (state == null) {
            plugin.getLogger().warning("No GUI state found for player " + player.getName() + " in PriceGUI");
            player.sendMessage(plugin.getConfigManager().getMessage("shop-error"));
            player.closeInventory();
            return;
        }

        String shopId = (String) state.get("shopId");
        if (!shop.getId().equals(shopId)) {
            plugin.getLogger().warning("Shop ID mismatch in PriceGUI for player " + player.getName() + ": expected " + shop.getId() + ", found " + shopId);
            player.sendMessage(plugin.getConfigManager().getMessage("shop-error"));
            player.closeInventory();
            guiStates.remove(playerUUID);
            return;
        }

        ItemStack item = (ItemStack) state.get("item");
        int quantity = (int) state.get("quantity");
        double buyPrice = (double) state.getOrDefault("buyPrice", 0.0);

        plugin.getLogger().info("Processing click in PriceGUI: Player=" + player.getName() + ", Slot=" + slot + ", Item=" + clickedItem.getType().name() + ", BuyPrice=" + buyPrice);

        int[] slots = {12, 13, 14};
        int[] increments = {1, 10, 100};
        boolean priceChanged = false;

        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i] && clickedItem.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                buyPrice += increments[i];
                priceChanged = true;
                plugin.getLogger().info("Incremented price by " + increments[i] + " to " + buyPrice + " for " + player.getName());
                break;
            } else if (slot == slots[i] + 9 && clickedItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                buyPrice = Math.max(0, buyPrice - increments[i]);
                priceChanged = true;
                plugin.getLogger().info("Decremented price by " + increments[i] + " to " + buyPrice + " for " + player.getName());
                break;
            }
        }

        if (slot == 16 && clickedItem.getType() == Material.EMERALD) {
            if (buyPrice <= 0) {
                player.sendMessage(plugin.getConfigManager().getMessage("shop-no-price"));
                plugin.getLogger().warning("Attempted to confirm with buyPrice <= 0 for " + player.getName());
                return;
            }
            if (shop.isAdminShop()) {
                ShopManager shopManager = plugin.getShopManager();
                boolean success = shopManager.addItemToShop(shop, player, item, quantity, buyPrice);
                if (success) {
                    guiStates.remove(playerUUID);
                    SelectItemGUI.open(player, shop, plugin);
                    plugin.getLogger().info("Successfully added item to admin shop " + shop.getId() + " for " + player.getName());
                } else {
                    plugin.getLogger().warning("Failed to add item " + item.getType().name() + " to admin shop " + shop.getId() + " for " + player.getName());
                    player.sendMessage(plugin.getConfigManager().getMessage("shop-error"));
                }
            } else {
                StockGUI.open(player, shop, item, quantity, buyPrice, plugin);
                guiStates.remove(playerUUID);
                plugin.getLogger().info("Opening StockGUI for VIP shop " + shop.getId() + " for " + player.getName());
            }
            return;
        } else if (slot == 18 && clickedItem.getType() == Material.ARROW) {
            QuantityGUI.open(player, shop, item, plugin, "Gerenciar");
            guiStates.remove(playerUUID);
            plugin.getLogger().info("Player " + player.getName() + " returned to QuantityGUI");
            return;
        } else if (slot == 26 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            guiStates.remove(playerUUID);
            plugin.getLogger().info("Player " + player.getName() + " closed PriceGUI");
            return;
        }

        if (priceChanged) {
            ItemStack itemDisplay = new ItemStack(item.getType(), quantity);
            ItemMeta displayMeta = itemDisplay.getItemMeta();
            displayMeta.setDisplayName(ChatColor.YELLOW + item.getType().name().toLowerCase());
            displayMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Quantidade por compra: " + quantity,
                    ChatColor.GRAY + "Preco por " + quantity + ": " + (long) buyPrice
            ));
            itemDisplay.setItemMeta(displayMeta);
            inventory.setItem(4, itemDisplay);

            state.put("buyPrice", buyPrice);
            guiStates.put(playerUUID, state);
            plugin.getLogger().info("Price updated to " + buyPrice + " for item " + item.getType().name() + " by " + player.getName());
        } else {
            plugin.getLogger().info("No action taken for click in PriceGUI by " + player.getName() + " at slot " + slot);
        }
    }

    public static void clearState(Player player) {
        String playerUUID = player.getUniqueId().toString();
        guiStates.remove(playerUUID);
        if (plugin != null) {
            plugin.getLogger().info("Cleared PriceGUI state for " + player.getName());
        } else {
            Bukkit.getLogger().info("PriceGUI: Cleared state for " + player.getName() + " (plugin not initialized)");
        }
    }

    private static boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value) return true;
        }
        return false;
    }
}