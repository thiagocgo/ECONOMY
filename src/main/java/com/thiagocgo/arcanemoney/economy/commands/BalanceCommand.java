package com.thiagocgo.arcanemoney.economy.commands;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.ConfigManager;
import com.thiagocgo.arcanemoney.economy.utils.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final ArcaneMoney plugin;

    public BalanceCommand(ArcaneMoney plugin) {
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

        Player player = (Player) sender;
        double balance = economy.getBalance(player);
        player.sendMessage(configManager.getMessage("balance")
                .replace("%amount%", String.valueOf((long) balance))
                .replace("%currency%", economy.getCurrencyName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}