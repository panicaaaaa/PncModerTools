package ua.panica.main.pncmodertools.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ua.panica.main.pncmodertools.manager.CheckManager;

public class ReviceCommandBlockListener implements Listener {

    private final CheckManager checkManager;

    public ReviceCommandBlockListener(CheckManager checkManager) {
        this.checkManager = checkManager;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (checkManager.isFrozen(player)) {
            String command = event.getMessage().toLowerCase();

            if (!command.startsWith("/revice")) {
                event.setCancelled(true);
                player.sendMessage("§cВы не можете использовать команды во время проверки.");
            }
        }
    }
}