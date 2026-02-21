package ua.panica.main.pncmodertools.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.model.CheckSession;

public class ReviceActionBarTask extends BukkitRunnable {

    private final PncModerTools plugin;
    private final MiniMessage miniMessage;

    public ReviceActionBarTask(PncModerTools plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void run() {
        plugin.getCheckManager().checkPunishTimeouts();

        for (CheckSession session : plugin.getCheckManager().getSessions()) {
            updateActionBar(session);
        }
    }

    private void updateActionBar(CheckSession session) {
        Player suspect = session.getSuspect();
        Player moderator = session.getModerator();

        if (!suspect.isOnline() || !moderator.isOnline()) {
            return;
        }

        if (session.timerPaused) {
            return;
        }

        long remainingMs = session.getRemainingMillis();
        String timeFormatted = formatTime(remainingMs);

        String format = plugin.getReviceConfig().getActionBarFormat()
                .replace("%mm:ss%", timeFormatted);

        Component component = miniMessage.deserialize(format);

        plugin.adventure().player(suspect).sendActionBar(component);
        plugin.adventure().player(moderator).sendActionBar(component);
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}