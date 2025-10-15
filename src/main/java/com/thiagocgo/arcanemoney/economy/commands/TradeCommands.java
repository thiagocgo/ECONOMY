package com.thiagocgo.arcanemoney.economy.commands;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.economy.utils.EconomyManager;
import com.thiagocgo.arcanemoney.economy.utils.InvestmentManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TradeCommands implements CommandExecutor, TabCompleter {

    private final ArcaneMoney plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final InvestmentManager investmentManager;

    public TradeCommands(ArcaneMoney plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.economyManager = plugin.getEconomyManager();
        this.investmentManager = plugin.getInvestmentManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration trades = configManager.getTrades();

        if (command.getName().equalsIgnoreCase("sell")) {
            return handleSellCommand(player, args);
        } else if (command.getName().equalsIgnoreCase("trade")) {
            return handleTradeCommand(player, args);
        }

        return false;
    }

    private boolean handleSellCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/sell <modid> <item> <qtd>"));
            return true;
        }

        String modid = args[0].toLowerCase();
        String item = args[1].toLowerCase();
        String itemTag = modid + ":" + item;
        int quantity;

        try {
            quantity = Integer.parseInt(args[2]);
            if (quantity <= 0) {
                player.sendMessage(configManager.getMessage("negative-amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getMessage("invalid-amount"));
            return true;
        }

        String tradeConfig = configManager.getTrades().getString("trades." + itemTag);
        if (tradeConfig == null) {
            player.sendMessage(configManager.getMessage("trade-item-not-found").replace("%item%", itemTag));
            return true;
        }

        double sellPrice = parsePrice(tradeConfig, "sell");
        if (sellPrice < 0) {
            player.sendMessage(configManager.getMessage("trade-item-not-found").replace("%item%", itemTag));
            return true;
        }

        double vipBonus = configManager.getConfig().getDouble("trade.vip-bonus", 0.1);
        if (investmentManager.isVipOrInvestor(player)) {
            sellPrice *= (1 + vipBonus);
        }

        Material material = Material.getMaterial(item.toUpperCase());
        if (material == null) {
            player.sendMessage(configManager.getMessage("trade-item-not-found").replace("%item%", itemTag));
            return true;
        }

        ItemStack itemStack = new ItemStack(material, quantity);
        PlayerInventory inventory = player.getInventory();
        if (!inventory.containsAtLeast(itemStack, quantity)) {
            player.sendMessage(configManager.getMessage("trade-insufficient-items")
                    .replace("%qtd%", String.valueOf(quantity))
                    .replace("%item%", itemTag));
            return true;
        }

        inventory.removeItem(itemStack);
        double totalEarned = Math.floor(sellPrice * quantity);
        economyManager.processTransaction(player, totalEarned, "Sale of " + quantity + " " + itemTag);

        player.sendMessage(configManager.getMessage("trade-sell-success")
                .replace("%qtd%", String.valueOf(quantity))
                .replace("%item%", itemTag)
                .replace("%amount%", String.valueOf((long) totalEarned))
                .replace("%currency%", economyManager.getCurrencyName()));
        plugin.getBossBarManager().updateBossBar(player);
        return true;
    }

    private boolean handleTradeCommand(Player player, String[] args) {
        if (!player.hasPermission("arcanemoney.admin")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            configManager.getMessageList("admin-help").forEach(player::sendMessage);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length != 5) {
                    player.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/trade add <modid> <item> <preço_compra> <preço_venda>"));
                    return true;
                }

                String modid = args[1].toLowerCase();
                String item = args[2].toLowerCase();
                String itemTag = modid + ":" + item;
                double buyPrice;
                double sellPrice;

                try {
                    buyPrice = Double.parseDouble(args[3]);
                    sellPrice = Double.parseDouble(args[4]);
                    if (buyPrice < 0 || sellPrice < 0) {
                        player.sendMessage(configManager.getMessage("negative-amount"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(configManager.getMessage("invalid-amount"));
                    return true;
                }

                Material material = Material.getMaterial(item.toUpperCase());
                if (material == null) {
                    player.sendMessage(configManager.getMessage("trade-item-not-found").replace("%item%", itemTag));
                    return true;
                }

                FileConfiguration trades = configManager.getTrades();
                trades.set("trades." + itemTag, "buy=" + buyPrice + ",sell=" + sellPrice);
                configManager.saveTrades();

                player.sendMessage(configManager.getMessage("trade-add-success")
                        .replace("%item%", itemTag)
                        .replace("%buy_price%", String.valueOf((long) buyPrice))
                        .replace("%sell_price%", String.valueOf((long) sellPrice)));
                return true;

            case "remove":
                if (args.length != 3) {
                    player.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/trade remove <modid> <item>"));
                    return true;
                }

                modid = args[1].toLowerCase();
                item = args[2].toLowerCase();
                itemTag = modid + ":" + item;

                if (!configManager.getTrades().contains("trades." + itemTag)) {
                    player.sendMessage(configManager.getMessage("trade-item-not-found").replace("%item%", itemTag));
                    return true;
                }

                configManager.getTrades().set("trades." + itemTag, null);
                configManager.saveTrades();

                player.sendMessage(configManager.getMessage("trade-remove-success").replace("%item%", itemTag));
                return true;

            case "list":
                FileConfiguration tradesList = configManager.getTrades();
                player.sendMessage(configManager.getMessage("trade-list-header"));
                for (String tradeItem : tradesList.getConfigurationSection("trades").getKeys(false)) {
                    String tradeConfig = tradesList.getString("trades." + tradeItem);
                    double buyPriceList = parsePrice(tradeConfig, "buy");
                    double sellPriceList = parsePrice(tradeConfig, "sell");
                    player.sendMessage(configManager.getMessage("trade-list-entry")
                            .replace("%item%", tradeItem)
                            .replace("%buy_price%", String.valueOf((long) buyPriceList))
                            .replace("%sell_price%", String.valueOf((long) sellPriceList)));
                }
                return true;

            default:
                configManager.getMessageList("admin-help").forEach(player::sendMessage);
                return true;
        }
    }

    private double parsePrice(String tradeConfig, String type) {
        if (tradeConfig == null) return -1;
        String[] parts = tradeConfig.split(",");
        for (String part : parts) {
            if (part.startsWith(type + "=")) {
                try {
                    return Double.parseDouble(part.split("=")[1]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        FileConfiguration trades = configManager.getTrades();

        if (command.getName().equalsIgnoreCase("sell")) {
            if (args.length == 1) {
                completions.add("minecraft");
            } else if (args.length == 2) {
                completions.addAll(trades.getConfigurationSection("trades").getKeys(false).stream()
                        .filter(key -> key.startsWith(args[0].toLowerCase() + ":"))
                        .map(key -> key.split(":")[1])
                        .collect(Collectors.toList()));
            }
        } else if (command.getName().equalsIgnoreCase("trade")) {
            if (args.length == 1) {
                if (sender.hasPermission("arcanemoney.admin")) {
                    completions.addAll(Arrays.asList("add", "remove", "list"));
                } else {
                    completions.add("list");
                }
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
                completions.add("minecraft");
            } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
                completions.addAll(Arrays.stream(Material.values())
                        .map(material -> material.name().toLowerCase())
                        .filter(name -> name.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList()));
            } else if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
                completions.addAll(trades.getConfigurationSection("trades").getKeys(false).stream()
                        .filter(key -> key.startsWith(args[1].toLowerCase() + ":"))
                        .map(key -> key.split(":")[1])
                        .collect(Collectors.toList()));
            }
        }

        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}