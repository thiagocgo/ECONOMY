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

public class CustomerGUI {
    public static void open(Player player, Shop shop, ArcaneMoney plugin) {
        plugin.getLogger().info("Opening CostumerGUI for player " + player.getName() + " at shop " + shop.getId());
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.LIGHT_PURPLE + "Loja ArcaneMC [" + shop.getId() + "]");

        int slot = 0;
        for (Shop.ShopItem item : shop.getItems().values()) {
            if (slot >= 45) break;
            ItemStack itemStack = new ItemStack(Material.matchMaterial(item.getItemTag().split(":")[1].toUpperCase()), item.getQuantity());
            ItemMeta meta = itemStack.getItemMeta();
            double buyPrice = item.getBuyPrice();
            if (plugin.getInvestmentManager().isVipOrInvestor(player)) {
                buyPrice *= (1 - plugin.getConfigManager().getConfig().getDouble("trade.vip-discount", 0.1));
            }
            meta.setDisplayName(ChatColor.YELLOW + item.getItemTag().split(":")[1].toUpperCase());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Preço por " + item.getQuantity() + ": " + (buyPrice > 0 ? (long) buyPrice : "Não disponível para compra"),
                    ChatColor.GRAY + "Estoque: " + (shop.isAdminShop() ? "Ilimitado" : item.getStock()),
                    buyPrice > 0 && (shop.isAdminShop() || item.getStock() >= item.getQuantity() || item.getStock() == -1) ? ChatColor.GREEN + "Clique para comprar" : ChatColor.RED + "Não disponível"
            ));
            itemStack.setItemMeta(meta);
            inventory.setItem(slot++, itemStack);
        }

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Fechar");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(53, closeButton);

        player.openInventory(inventory);
    }

    public static void handleClick(Player player, Shop shop, Inventory inventory, ItemStack clickedItem, int slot, ArcaneMoney plugin) {
        if (slot >= 45 || clickedItem == null || clickedItem.getType() == Material.AIR) {
            if (slot == 53 && clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                player.closeInventory();
            }
            return;
        }

        String itemTag = "minecraft:" + clickedItem.getType().name().toLowerCase();
        Shop.ShopItem item = shop.getItems().get(itemTag);
        if (item == null || (!shop.isAdminShop() && item.getStock() <= 0) || item.getBuyPrice() <= 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("shop-no-stock"));
            return;
        }

        ShopManager shopManager = plugin.getShopManager();
        shopManager.buyItem(shop, player, itemTag, 1);
    }
}