package com.thiagocgo.arcanemoney.shops.guis;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.shops.utils.Shop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class QuantityGUI {
    private static final int[] QUANTITIES = {1, 2, 4, 8, 16, 32, 64};
    private static final Map<String, Map<String, Object>> guiStates = new HashMap<>();
    private static ArcaneMoney plugin; // Add static plugin field

    public static void setPlugin(ArcaneMoney pluginInstance) {
        plugin = pluginInstance;
    }

    public static void open(Player player, Shop shop, ItemStack item, ArcaneMoney plugin, String context) {
        if (QuantityGUI.plugin == null) {
            QuantityGUI.plugin = plugin; // Initialize plugin if not set
        }
        clearState(player); // Clear any existing state
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.LIGHT_PURPLE + "Loja ArcaneMC - Quantidade [" + shop.getId() + "]");

        for (int i = 0; i < QUANTITIES.length; i++) {
            ItemStack qtyItem = new ItemStack(Material.PAPER);
            ItemMeta meta = qtyItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Quantidade: " + QUANTITIES[i]);
            qtyItem.setItemMeta(meta);
            qtyItem.setAmount(QUANTITIES[i]);
            inventory.setItem(i + 10, qtyItem);
        }

        ItemStack itemDisplay = new ItemStack(item.getType());
        ItemMeta displayMeta = itemDisplay.getItemMeta();
        displayMeta.setDisplayName(ChatColor.YELLOW + item.getType().name().toLowerCase());
        itemDisplay.setItemMeta(displayMeta);
        inventory.setItem(4, itemDisplay);

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

        String playerUUID = player.getUniqueId().toString();
        Map<String, Object> state = new HashMap<>();
        state.put("item", item);
        state.put("shopId", shop.getId());
        state.put("context", context);
        guiStates.put(playerUUID, state);

        player.openInventory(inventory);
        plugin.getLogger().info("Opened QuantityGUI for " + player.getName() + " with context " + context);
    }

    public static void handleClick(Player player, Shop shop, Inventory inventory, ItemStack clickedItem, int slot, ArcaneMoney plugin) {
        if (QuantityGUI.plugin == null) {
            QuantityGUI.plugin = plugin; // Initialize plugin if not set
        }
        String playerUUID = player.getUniqueId().toString();
        Map<String, Object> state = guiStates.get(playerUUID);
        if (state == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("shop-error"));
            player.closeInventory();
            return;
        }

        String shopId = (String) state.get("shopId");
        if (!shop.getId().equals(shopId)) {
            plugin.getLogger().warning("Shop ID mismatch in QuantityGUI for player " + player.getName() + ": expected " + shop.getId() + ", found " + shopId);
            player.sendMessage(plugin.getConfigManager().getMessage("shop-error"));
            player.closeInventory();
            guiStates.remove(playerUUID);
            return;
        }

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (slot == 18 && clickedItem.getType() == Material.ARROW) {
            if ("Gerenciar".equals(state.get("context"))) {
                SelectItemGUI.open(player, shop, plugin);
            } else {
                ShopManageGUI.open(player, shop, plugin);
            }
            guiStates.remove(playerUUID);
            plugin.getLogger().info("Player " + player.getName() + " returned from QuantityGUI to previous menu");
            return;
        } else if (slot == 26 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            guiStates.remove(playerUUID);
            plugin.getLogger().info("Player " + player.getName() + " closed QuantityGUI");
            return;
        }

        if (slot < 10 || slot >= 10 + QUANTITIES.length) return;

        int quantity = QUANTITIES[slot - 10];
        ItemStack item = (ItemStack) state.get("item");
        String context = (String) state.get("context");

        if ("Gerenciar".equals(context) || "Adicionar".equals(context)) {
            PriceGUI.open(player, shop, item, quantity, plugin);
            guiStates.remove(playerUUID);
            plugin.getLogger().info("Player " + player.getName() + " selected quantity " + quantity + " and moved to PriceGUI");
        }
    }

    public static void clearState(Player player) {
        String playerUUID = player.getUniqueId().toString();
        guiStates.remove(playerUUID);
        if (plugin != null) {
            plugin.getLogger().info("Cleared QuantityGUI state for " + player.getName());
        } else {
            Bukkit.getLogger().info("QuantityGUI: Cleared state for " + player.getName() + " (plugin not initialized)");
        }
    }
}