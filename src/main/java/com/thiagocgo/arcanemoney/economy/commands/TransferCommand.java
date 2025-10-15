package com.thiagocgo.arcanemoney.economy.commands;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.economy.utils.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TransferCommand implements CommandExecutor, TabCompleter {

    private final ArcaneMoney plugin;

    public TransferCommand(ArcaneMoney plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager configManager = plugin.getConfigManager();
        EconomyManager economy = plugin.getEconomyManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(configManager.getMessage("invalid-arguments").replace("%usage%", "/transferir <jogador> <quantidade>"));
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(configManager.getMessage("invalid-player"));
            return true;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(configManager.getMessage("same-player"));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage(configManager.getMessage("negative-amount"));
                return true;
            }
            if (economy.getBalance(player) < amount) {
                player.sendMessage(configManager.getMessage("insufficient-balance"));
                return true;
            }
            economy.removeBalance(player, amount);
            economy.addBalance(target, amount);
            player.sendMessage(configManager.getMessage("transfer-sent")
                    .replace("%amount%", String.valueOf((long) amount))
                    .replace("%player%", target.getName())
                    .replace("%currency%", economy.getCurrencyName()));
            target.sendMessage(configManager.getMessage("transfer-received")
                    .replace("%amount%", String.valueOf((long) amount))
                    .replace("%player%", player.getName())
                    .replace("%currency%", economy.getCurrencyName()));
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getMessage("invalid-amount"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}