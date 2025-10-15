package com.thiagocgo.arcanemoney.economy.commands;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.economy.utils.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArcaneMoneyCommand implements CommandExecutor, TabCompleter {

    private final ArcaneMoney plugin;

    public ArcaneMoneyCommand(ArcaneMoney plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager configManager = plugin.getConfigManager();
        EconomyManager economy = plugin.getEconomyManager();

        // Check for arcanemoney.admin permission or OP status
        if (!sender.hasPermission("arcanemoney.admin") && !(sender instanceof Player && ((Player) sender).isOp())) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        // Show help if no arguments are provided
        if (args.length == 0) {
            configManager.getMessageList("admin-help").forEach(sender::sendMessage);
            return true;
        }

        // Process subcommands
        switch (args[0].toLowerCase()) {
            case "vip":
                if (args.length != 2) {
                    sender.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/money vip <player>"));
                    return true;
                }
                OfflinePlayer targetVip = Bukkit.getOfflinePlayer(args[1]);
                if (!targetVip.hasPlayedBefore() && !targetVip.isOnline()) {
                    sender.sendMessage(configManager.getMessage("invalid-player"));
                    return true;
                }
                // Adiciona permissão via LuckPerms
                String uuid = targetVip.getUniqueId().toString();
                if (targetVip.isOnline() && targetVip.getPlayer().hasPermission("arcanemoney.vip")) {
                    sender.sendMessage(configManager.getMessage("player-already-vip").replace("%player%", targetVip.getName()));
                    return true;
                }
                // Executa comando do LuckPerms para adicionar permissão
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + targetVip.getName() + " permission set arcanemoney.vip true");
                sender.sendMessage(configManager.getMessage("vip-added").replace("%player%", targetVip.getName()));
                if (targetVip.isOnline()) {
                    targetVip.getPlayer().sendMessage(configManager.getMessage("vip-granted"));
                }
                break;

            case "adicionar":
                if (args.length != 3) {
                    sender.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/money adicionar <player> <amount>"));
                    return true;
                }
                OfflinePlayer targetAdd = Bukkit.getOfflinePlayer(args[1]);
                if (!targetAdd.hasPlayedBefore() && !targetAdd.isOnline()) {
                    sender.sendMessage(configManager.getMessage("invalid-player"));
                    return true;
                }
                try {
                    double amountAdd = Double.parseDouble(args[2]);
                    if (amountAdd <= 0) {
                        sender.sendMessage(configManager.getMessage("negative-amount"));
                        return true;
                    }
                    economy.addBalance(targetAdd.isOnline() ? targetAdd.getPlayer() : null, amountAdd);
                    sender.sendMessage(configManager.getMessage("admin-add")
                            .replace("%amount%", String.valueOf((long) amountAdd))
                            .replace("%player%", targetAdd.getName())
                            .replace("%currency%", economy.getCurrencyName()));
                    if (targetAdd.isOnline()) {
                        targetAdd.getPlayer().sendMessage(configManager.getMessage("transfer-received")
                                .replace("%amount%", String.valueOf((long) amountAdd))
                                .replace("%player%", sender.getName())
                                .replace("%currency%", economy.getCurrencyName()));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(configManager.getMessage("invalid-amount"));
                }
                break;

            case "remover":
                if (args.length != 3) {
                    sender.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/money remover <player> <amount>"));
                    return true;
                }
                OfflinePlayer targetRemove = Bukkit.getOfflinePlayer(args[1]);
                if (!targetRemove.hasPlayedBefore() && !targetRemove.isOnline()) {
                    sender.sendMessage(configManager.getMessage("invalid-player"));
                    return true;
                }
                try {
                    double amountRemove = Double.parseDouble(args[2]);
                    if (amountRemove <= 0) {
                        sender.sendMessage(configManager.getMessage("negative-amount"));
                        return true;
                    }
                    if (targetRemove.isOnline() && economy.getBalance(targetRemove.getPlayer()) < amountRemove) {
                        sender.sendMessage(configManager.getMessage("insufficient-balance"));
                        return true;
                    }
                    economy.removeBalance(targetRemove.isOnline() ? targetRemove.getPlayer() : null, amountRemove);
                    sender.sendMessage(configManager.getMessage("admin-remove")
                            .replace("%amount%", String.valueOf((long) amountRemove))
                            .replace("%player%", targetRemove.getName())
                            .replace("%currency%", economy.getCurrencyName()));
                    if (targetRemove.isOnline()) {
                        targetRemove.getPlayer().sendMessage(configManager.getMessage("admin-remove")
                                .replace("%amount%", String.valueOf((long) amountRemove))
                                .replace("%player%", sender.getName())
                                .replace("%currency%", economy.getCurrencyName()));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(configManager.getMessage("invalid-amount"));
                }
                break;

            case "saldo":
                if (args.length != 2) {
                    sender.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/money saldo <player>"));
                    return true;
                }
                OfflinePlayer targetBalance = Bukkit.getOfflinePlayer(args[1]);
                if (!targetBalance.hasPlayedBefore() && !targetBalance.isOnline()) {
                    sender.sendMessage(configManager.getMessage("invalid-player"));
                    return true;
                }
                double balance = targetBalance.isOnline() ? economy.getBalance(targetBalance.getPlayer()) : getOfflineBalance(targetBalance);
                sender.sendMessage(configManager.getMessage("admin-balance")
                        .replace("%player%", targetBalance.getName())
                        .replace("%amount%", String.valueOf((long) balance))
                        .replace("%currency%", economy.getCurrencyName()));
                break;

            case "transferir":
                if (args.length != 4) {
                    sender.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/money transferir <from> <to> <amount>"));
                    return true;
                }
                OfflinePlayer from = Bukkit.getOfflinePlayer(args[1]);
                OfflinePlayer to = Bukkit.getOfflinePlayer(args[2]);
                if (!from.hasPlayedBefore() || !to.hasPlayedBefore()) {
                    sender.sendMessage(configManager.getMessage("invalid-player"));
                    return true;
                }
                try {
                    double amountTransfer = Double.parseDouble(args[3]);
                    if (amountTransfer <= 0) {
                        sender.sendMessage(configManager.getMessage("negative-amount"));
                        return true;
                    }
                    if (from.isOnline() && economy.getBalance(from.getPlayer()) < amountTransfer) {
                        sender.sendMessage(configManager.getMessage("insufficient-balance"));
                        return true;
                    }
                    economy.removeBalance(from.isOnline() ? from.getPlayer() : null, amountTransfer);
                    economy.addBalance(to.isOnline() ? to.getPlayer() : null, amountTransfer);
                    sender.sendMessage(configManager.getMessage("admin-transfer")
                            .replace("%amount%", String.valueOf((long) amountTransfer))
                            .replace("%player1%", from.getName())
                            .replace("%player2%", to.getName())
                            .replace("%currency%", economy.getCurrencyName()));
                    if (from.isOnline()) {
                        from.getPlayer().sendMessage(configManager.getMessage("transfer-sent")
                                .replace("%amount%", String.valueOf((long) amountTransfer))
                                .replace("%player%", to.getName())
                                .replace("%currency%", economy.getCurrencyName()));
                    }
                    if (to.isOnline()) {
                        to.getPlayer().sendMessage(configManager.getMessage("transfer-received")
                                .replace("%amount%", String.valueOf((long) amountTransfer))
                                .replace("%player%", from.getName())
                                .replace("%currency%", economy.getCurrencyName()));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(configManager.getMessage("invalid-amount"));
                }
                break;

            case "reload":
                configManager.reloadConfig();
                plugin.getBossBarManager().initializeBossBars();
                sender.sendMessage(configManager.getMessage("reload"));
                break;

            default:
                configManager.getMessageList("admin-help").forEach(sender::sendMessage);
                break;
        }
        return true;
    }

    private double getOfflineBalance(OfflinePlayer player) {
        FileConfiguration economy = plugin.getConfigManager().getEconomy();
        List<Map<?, ?>> players = economy.getMapList("server-economy.players");
        String uuid = player.getUniqueId().toString();
        for (Map<?, ?> p : players) {
            if (p.get("uuid").equals(uuid)) {
                return Math.floor(((Number) p.get("balance")).doubleValue());
            }
        }
        return Math.floor(plugin.getConfigManager().getConfig().getDouble("starting-balance"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("arcanemoney.admin") && !(sender instanceof Player && ((Player) sender).isOp())) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("adicionar", "remover", "saldo", "transferir", "reload", "vip"));
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            return getPlayerNames(args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("transferir")) {
            return getPlayerNames(args[2]);
        }

        return completions;
    }

    private List<String> getPlayerNames(String arg) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> filterCompletions(List<String> completions, String arg) {
        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }
}