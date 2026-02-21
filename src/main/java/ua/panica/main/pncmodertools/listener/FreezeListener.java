package ua.panica.main.pncmodertools.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import ua.panica.main.pncmodertools.manager.CheckManager;

public class FreezeListener implements Listener {

    private final CheckManager checkManager;

    public FreezeListener(CheckManager checkManager) {
        this.checkManager = checkManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (checkManager.isFrozen(player)) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (checkManager.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (checkManager.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (checkManager.isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLook(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (checkManager.isFrozen(player)) {
            if (event.getFrom().getYaw() != event.getTo().getYaw() ||
                    event.getFrom().getPitch() != event.getTo().getPitch()) {
                event.setTo(event.getFrom());
            }
        }
    }
}