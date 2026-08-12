package com.samleighton.sethomestwo.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * One screen a player can have open. Screens do not register themselves as
 * listeners; the shared listener routes clicks to the session, and the session
 * routes them to whichever screen is active.
 */
public interface GuiScreen {

    /**
     * @return The inventory backing this screen, used to confirm a click
     * belongs to it.
     */
    Inventory getInventory();

    /**
     * Handle a click already confirmed to belong to this screen. The event has
     * already been cancelled by the session.
     *
     * @param event   The click
     * @param session The owning session, for navigating to another screen
     */
    void onClick(InventoryClickEvent event, GuiSession session);
}
