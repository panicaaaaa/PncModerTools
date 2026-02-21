package ua.panica.main.pncmodertools.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.panica.main.pncmodertools.manager.CheckManager;

public class ReviceQuitListener implements Listener {

    private final CheckManager checkManager;

    public ReviceQuitListener(CheckManager checkManager) {
        this.checkManager = checkManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        checkManager.markQuit(player);
    }
}