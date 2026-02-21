package ua.panica.main.pncmodertools.manager;

import litebans.api.Database;
import org.bukkit.Bukkit;
import ua.panica.main.pncmodertools.PncModerTools;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StatisticsManager {

    private final PncModerTools plugin;
    private final boolean liteBansEnabled;

    public StatisticsManager(PncModerTools plugin) {
        this.plugin = plugin;
        this.liteBansEnabled = Bukkit.getPluginManager().getPlugin("LiteBans") != null;
    }

    public void updateDailyStats(UUID playerId, String playerName, String date) {
        if (!liteBansEnabled) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                int mutes = getMutesCount(playerId, date);
                int bans = getBansCount(playerId, date);

                plugin.getDatabaseManager().getDailyStatsAsync(playerId, date).thenAccept(stats -> {
                    long workTime = stats != null ? stats.getWorkTime() : 0;

                    plugin.getDatabaseManager().updateDailyStatsAsync(
                            playerId,
                            playerName,
                            date,
                            workTime,
                            mutes,
                            bans
                    );
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private int getMutesCount(UUID playerId, String date) {
        if (!liteBansEnabled) {
            return 0;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            long dayStart = getDateStartMillis(date);
            long dayEnd = dayStart + (24 * 60 * 60 * 1000);

            String sql = "SELECT COUNT(*) FROM {mutes} WHERE banned_by_uuid = ? AND time >= ? AND time < ?";

            stmt = Database.get().prepareStatement(sql);
            stmt.setString(1, playerId.toString());
            stmt.setLong(2, dayStart);
            stmt.setLong(3, dayEnd);

            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception ignored) {}
            try {
                if (stmt != null) stmt.close();
            } catch (Exception ignored) {}
        }

        return 0;
    }

    private int getBansCount(UUID playerId, String date) {
        if (!liteBansEnabled) {
            return 0;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            long dayStart = getDateStartMillis(date);
            long dayEnd = dayStart + (24 * 60 * 60 * 1000);

            String sql = "SELECT COUNT(*) FROM {bans} WHERE banned_by_uuid = ? AND time >= ? AND time < ?";

            stmt = Database.get().prepareStatement(sql);
            stmt.setString(1, playerId.toString());
            stmt.setLong(2, dayStart);
            stmt.setLong(3, dayEnd);

            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception ignored) {}
            try {
                if (stmt != null) stmt.close();
            } catch (Exception ignored) {}
        }

        return 0;
    }

    private long getDateStartMillis(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}