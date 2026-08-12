package com.samleighton.sethomestwo.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Per-player GUI state. Owns the player's home list screen and whichever screen
 * is currently open, and routes inventory events to the active screen.
 */
public class GuiSession {

    private final HomesGui homesGui;
    private GuiScreen activeScreen;

    public GuiSession(HomesGui homesGui) {
        this.homesGui = homesGui;
    }

    public HomesGui getHomesGui() {
        return homesGui;
    }

    public GuiScreen getActiveScreen() {
        return activeScreen;
    }

    public void setActiveScreen(GuiScreen activeScreen) {
        this.activeScreen = activeScreen;
    }

    /**
     * Re-display the home list and make it the active screen.
     *
     * @param player The viewing player
     */
    public void openHomeList(Player player) {
        setActiveScreen(homesGui);
        homesGui.displayInventory(player);
    }

    /**
     * Route a click to the active screen, if the click belongs to it.
     *
     * @param event The click
     */
    public void handleClick(InventoryClickEvent event) {
        if (activeScreen == null) return;
        if (!event.getInventory().equals(activeScreen.getInventory())) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;

        activeScreen.onClick(event, this);
    }

    /**
     * Cancel drags inside the active screen.
     *
     * @param event The drag
     */
    public void handleDrag(InventoryDragEvent event) {
        if (activeScreen == null) return;
        if (!event.getInventory().equals(activeScreen.getInventory())) return;

        event.setCancelled(true);
    }
}
