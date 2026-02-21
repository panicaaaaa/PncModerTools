package ua.panica.main.pncmodertools.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.panica.main.pncmodertools.PncModerTools;

public class PlayerListener implements Listener {

    private final PncModerTools plugin;

    public PlayerListener(PncModerTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getStaffWorkManager().onPlayerQuit(player);

        if (player.hasPermission("hexmodertools.staffwork")) {
            String today = plugin.getStaffWorkManager().getTodayDate();
            plugin.getStatisticsManager().updateDailyStats(
                    player.getUniqueId(),
                    player.getName(),
                    today
            );
        }
    }
}