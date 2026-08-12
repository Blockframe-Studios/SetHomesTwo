package com.samleighton.sethomestwo.events;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.gui.HomesGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {
    private final SetHomesTwo plugin;

    public PlayerJoin(SetHomesTwo plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        // Get the player from the event
        Player player = event.getPlayer();
        plugin.getGuiSessionMap().put(player.getUniqueId(), new GuiSession(new HomesGui(player)));
    }
}
