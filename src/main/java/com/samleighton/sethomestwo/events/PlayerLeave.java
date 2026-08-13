package com.samleighton.sethomestwo.events;

import com.samleighton.sethomestwo.SetHomesTwo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeave implements Listener {

    private final SetHomesTwo plugin;

    public PlayerLeave(SetHomesTwo plugin){
        this.plugin = plugin;
    }

    private void handlePlayerDisconnect(Player player){
        plugin.getGuiSessionMap().remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        this.handlePlayerDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event){
        this.handlePlayerDisconnect(event.getPlayer());
    }
}
