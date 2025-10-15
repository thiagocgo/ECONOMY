package com.thiagocgo.arcanemoney.economy.listeners;

import com.thiagocgo.arcanemoney.ArcaneMoney;
import com.thiagocgo.arcanemoney.economy.utils.EconomyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ArcaneMoney plugin;

    public PlayerListener(ArcaneMoney plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        EconomyManager economy = plugin.getEconomyManager();
        economy.getBalance(event.getPlayer()); // Cria entrada se não existir
        plugin.getInvestmentManager().sendYieldStatus(event.getPlayer()); // Enviar status do investimento
        plugin.getBossBarManager().createBossBar(event.getPlayer()); // Criar BossBar
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getBossBarManager().removeBossBar(event.getPlayer()); // Remover BossBar
    }
}