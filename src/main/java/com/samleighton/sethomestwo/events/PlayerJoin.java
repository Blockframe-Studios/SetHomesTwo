package com.samleighton.sethomestwo.events;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.gui.HomesGui;
import com.samleighton.sethomestwo.importers.PendingV1Import;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
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
        notifyPendingV1Import(player);
    }

    /**
     * Tells an admin that v1 homes are waiting. The permission is checked first
     * so an ordinary join never queries the database or reads v1's file.
     */
    private void notifyPendingV1Import(Player player) {
        if (!player.hasPermission("sh2.import-homes")) return;

        int waiting = PendingV1Import.waitingToBeImported();
        if (waiting == 0) return;

        ChatUtils.sendInfo(player, String.format(ConfigUtil.getConfig().getString(
                "v1ImportPending", UserInfo.V1_IMPORT_PENDING.getValue()), waiting));
    }
}
