package ua.panica.main.pncmodertools.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ua.panica.main.pncmodertools.manager.CheckManager;


public class ReviceJoinListener implements Listener {

    private final CheckManager checkManager;

    public ReviceJoinListener(CheckManager checkManager) {
        this.checkManager = checkManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        checkManager.onJoin(player);
    }
}