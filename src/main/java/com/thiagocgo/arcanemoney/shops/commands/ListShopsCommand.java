package com.thiagocgo.arcanemoney.shops.commands;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.shops.utils.Shop;
import com.thiagocgo.arcanemoney.shops.utils.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ListShopsCommand implements CommandExecutor {

    private final ArcaneMoney plugin;

    public ListShopsCommand(ArcaneMoney plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager configManager = plugin.getConfigManager();
        ShopManager shopManager = plugin.getShopManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        List<Shop> playerShops = shopManager.getPlayerShops(player);

        if (playerShops.isEmpty()) {
            player.sendMessage(configManager.getMessage("shop-list-header"));
            player.sendMessage(ChatColor.RED + "Você não possui lojas.");
            return true;
        }

        player.sendMessage(configManager.getMessage("shop-list-header"));
        for (Shop shop : playerShops) {
            String items = shop.getItems().keySet().stream()
                    .map(item -> item.split(":")[1])
                    .collect(Collectors.joining(", "));
            String location = shop.getWorld() + " " + shop.getX() + "," + shop.getY() + "," + shop.getZ();
            player.sendMessage(configManager.getMessage("shop-list-entry")
                    .replace("%location%", location)
                    .replace("%type%", shop.isAdminShop() ? "Admin" : "Jogador")
                    .replace("%items%", items.isEmpty() ? "Nenhum" : items));
        }
        return true;
    }
}