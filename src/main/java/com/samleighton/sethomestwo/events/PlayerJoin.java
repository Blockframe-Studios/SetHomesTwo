package com.samleighton.sethomestwo.events;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.gui.HomesGui;
import com.samleighton.sethomestwo.updates.UpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {
    private final SetHomesTwo plugin;
    private final UpdateChecker updateChecker;

    public PlayerJoin(SetHomesTwo plugin, UpdateChecker updateChecker){
        this.plugin = plugin;
        this.updateChecker = updateChecker;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        // Get the player from the event
        Player player = event.getPlayer();
        plugin.getGuiSessionMap().put(player.getUniqueId(), new GuiSession(new HomesGui(player)));

        new HomesDao().refreshPlayerName(player.getUniqueId(), player.getName());

        updateChecker.notifyIfUpdateAvailable(player);
    }
}
