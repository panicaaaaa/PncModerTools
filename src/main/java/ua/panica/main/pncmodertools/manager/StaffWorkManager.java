package ua.panica.main.pncmodertools.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.model.StaffWorkSession;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StaffWorkManager {

    private final PncModerTools plugin;
    private final Map<UUID, StaffWorkSession> activeSessions;
    private final SimpleDateFormat dateFormat;

    public StaffWorkManager(PncModerTools plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+3"));
    }

    public void toggleWork(Player player, java.util.function.Consumer<Boolean> callback) {
        UUID playerId = player.getUniqueId();

        plugin.getDatabaseManager().getWorkSessionAsync(playerId).thenAccept(session -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean enabled;

                if (session == null) {
                    StaffWorkSession newSession = new StaffWorkSession(
                            playerId,
                            player.getName(),
                            System.currentTimeMillis(),
                            0,
                            true
                    );
                    activeSessions.put(playerId, newSession);
                    plugin.getDatabaseManager().saveWorkSessionAsync(newSession);

                    if (plugin.getGroupManager().isAvailable()) {
                        plugin.getGroupManager().switchToStaffGroup(player);
                    }

                    enabled = true;

                } else if (session.isActive() || activeSessions.containsKey(playerId)) {
                    StaffWorkSession activeSession = activeSessions.getOrDefault(playerId, session);
                    long sessionTime = activeSession.getCurrentSessionTime();
                    activeSession.addTime(sessionTime);
                    activeSession.setActive(false);

                    String today = getTodayDate();
                    plugin.getDatabaseManager().addWorkTimeAsync(
                            playerId,
                            player.getName(),
                            today,
                            sessionTime
                    );

                    plugin.getDatabaseManager().deleteWorkSessionAsync(playerId);
                    activeSessions.remove(playerId);

                    if (plugin.getGroupManager().isAvailable()) {
                        plugin.getGroupManager().switchToNonStaffGroup(player);
                    }

                    enabled = false;

                } else {
                    session.setStartTime(System.currentTimeMillis());
                    session.setActive(true);
                    activeSessions.put(playerId, session);
                    plugin.getDatabaseManager().saveWorkSessionAsync(session);

                    if (plugin.getGroupManager().isAvailable()) {
                        plugin.getGroupManager().switchToStaffGroup(player);
                    }

                    enabled = true;
                }

                callback.accept(enabled);
            });
        });
    }

    public boolean isWorking(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public void getSession(UUID playerId, java.util.function.Consumer<StaffWorkSession> callback) {
        StaffWorkSession session = activeSessions.get(playerId);
        if (session != null) {
            callback.accept(session);
        } else {
            plugin.getDatabaseManager().getWorkSessionAsync(playerId).thenAccept(dbSession -> {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(dbSession));
            });
        }
    }

    public void saveAllSessions() {
        String today = getTodayDate();

        for (StaffWorkSession session : activeSessions.values()) {
            if (session.isActive()) {
                long sessionTime = session.getCurrentSessionTime();
                session.addTime(sessionTime);
                session.setActive(false);

                plugin.getDatabaseManager().addWorkTimeAsync(
                        session.getPlayerId(),
                        session.getPlayerName(),
                        today,
                        sessionTime
                );
            }

            plugin.getDatabaseManager().saveWorkSessionAsync(session);
        }

        activeSessions.clear();
    }

    public void onPlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        StaffWorkSession session = activeSessions.get(playerId);

        if (session != null && session.isActive()) {
            long sessionTime = session.getCurrentSessionTime();
            session.addTime(sessionTime);
            session.setActive(false);

            String today = getTodayDate();
            plugin.getDatabaseManager().addWorkTimeAsync(
                    playerId,
                    player.getName(),
                    today,
                    sessionTime
            );

            plugin.getDatabaseManager().deleteWorkSessionAsync(playerId);
            activeSessions.remove(playerId);

            if (plugin.getGroupManager().isAvailable()) {
                plugin.getGroupManager().switchToNonStaffGroup(player);
            }
        }
    }

    public String formatTime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public String getTodayDate() {
        return dateFormat.format(new Date());
    }
}