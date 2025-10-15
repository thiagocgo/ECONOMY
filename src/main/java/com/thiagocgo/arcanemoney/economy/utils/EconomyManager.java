package com.thiagocgo.arcanemoney.economy.utils;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EconomyManager {

    private final ConfigManager configManager;
    private final ArcaneMoney plugin;

    public EconomyManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.plugin = configManager.getPlugin();
        validateEconomyFile(); // Validar ao inicializar
    }

    private void validateEconomyFile() {
        FileConfiguration economy = configManager.getEconomy();
        List<Map<?, ?>> players = economy.getMapList("server-economy.players");
        List<Map<String, Object>> cleanedPlayers = new ArrayList<>();
        List<String> seenUuids = new ArrayList<>();

        for (Map<?, ?> p : players) {
            String uuid = (String) p.get("uuid");
            if (!seenUuids.contains(uuid)) {
                seenUuids.add(uuid);
                // Garantir que o saldo não seja negativo e seja inteiro
                double balance = Math.floor(((Number) p.get("balance")).doubleValue());
                if (balance < 0) {
                    ((Map<String, Object>) p).put("balance", 0.0);
                    configManager.getPlugin().getLogger().warning("Negative balance detected for UUID " + uuid + ". Set to 0.");
                } else {
                    ((Map<String, Object>) p).put("balance", balance);
                }
                cleanedPlayers.add((Map<String, Object>) p);
            } else {
                String nickname = (String) p.get("nickname");
                configManager.getPlugin().getLogger().warning(
                        configManager.getMessage("duplicate-uuid").replace("%player%", nickname));
            }
        }

        economy.set("server-economy.players", cleanedPlayers);
        configManager.saveEconomy();
    }

    public double getBalance(Player player) {
        FileConfiguration economy = configManager.getEconomy();
        List<Map<?, ?>> players = economy.getMapList("server-economy.players");
        String uuid = player.getUniqueId().toString();
        for (Map<?, ?> p : players) {
            if (p.get("uuid").equals(uuid)) {
                return Math.floor(((Number) p.get("balance")).doubleValue());
            }
        }
        // Novo jogador: criar entrada
        double startingBalance = Math.floor(configManager.getConfig().getDouble("starting-balance"));
        addPlayer(player, startingBalance);
        return startingBalance;
    }

    public void setBalance(Player player, double amount) {
        amount = Math.floor(amount); // Arredonda para inteiro
        if (amount < 0) {
            configManager.getPlugin().getLogger().warning("Attempt to set negative balance for " + player.getName() + ". Set to 0.");
            amount = 0;
        }
        FileConfiguration economy = configManager.getEconomy();
        List<Map<?, ?>> players = economy.getMapList("server-economy.players");
        String uuid = player.getUniqueId().toString();
        for (Map<?, ?> p : players) {
            if (p.get("uuid").equals(uuid)) {
                ((Map<String, Object>) p).put("balance", amount);
                ((Map<String, Object>) p).put("nickname", player.getName());
                economy.set("server-economy.players", players);
                configManager.saveEconomy();
                plugin.getBossBarManager().updateBossBar(player); // Atualizar BossBar
                return;
            }
        }
        // Novo jogador
        addPlayer(player, amount);
    }

    public void addBalance(Player player, double amount) {
        if (amount < 0) {
            configManager.getPlugin().getLogger().warning("Attempt to add negative amount for " + player.getName() + ". Ignored.");
            return;
        }
        setBalance(player, getBalance(player) + Math.floor(amount));
    }

    public void removeBalance(Player player, double amount) {
        if (amount < 0) {
            configManager.getPlugin().getLogger().warning("Attempt to remove negative amount for " + player.getName() + ". Ignored.");
            return;
        }
        setBalance(player, Math.max(0, getBalance(player) - Math.floor(amount)));
    }

    public boolean processTransaction(Player player, double amount, String reason) {
        amount = Math.floor(amount); // Arredonda para inteiro
        if (amount == 0) {
            return false;
        }
        double currentBalance = getBalance(player);
        double newBalance = currentBalance + amount;
        if (newBalance < 0) {
            configManager.getPlugin().getLogger().warning("Transaction failed for " + player.getName() + ": insufficient balance for " + reason);
            player.sendMessage(configManager.getMessage("insufficient-balance"));
            return false;
        }
        setBalance(player, newBalance);
        configManager.getPlugin().getLogger().info("Transaction processed for " + player.getName() + ": " + amount + " (" + reason + ")");
        return true;
    }

    private void addPlayer(Player player, double balance) {
        balance = Math.floor(balance); // Arredonda para inteiro
        if (balance < 0) {
            configManager.getPlugin().getLogger().warning("Attempt to add player " + player.getName() + " with negative balance. Set to 0.");
            balance = 0;
        }
        FileConfiguration economy = configManager.getEconomy();
        List<Map<?, ?>> players = economy.getMapList("server-economy.players");
        String uuid = player.getUniqueId().toString();

        // Verificar duplicata antes de adicionar
        for (Map<?, ?> p : players) {
            if (p.get("uuid").equals(uuid)) {
                configManager.getPlugin().getLogger().warning(
                        configManager.getMessage("duplicate-uuid").replace("%player%", player.getName()));
                return;
            }
        }

        Map<String, Object> playerData = new HashMap<>();
        playerData.put("nickname", player.getName());
        playerData.put("uuid", uuid);
        playerData.put("balance", balance);
        players.add(playerData);
        economy.set("server-economy.players", players);
        configManager.saveEconomy();
        plugin.getBossBarManager().createBossBar(player); // Criar BossBar para novo jogador
        player.sendMessage(configManager.getMessage("nickname-updated").replace("%player%", player.getName()));
    }

    public String getCurrencyName() {
        return configManager.getConfig().getString("currency.name", "Arcane Coins");
    }

    public String getCurrencySymbol() {
        return configManager.getConfig().getString("currency.symbol", "AC");
    }
}