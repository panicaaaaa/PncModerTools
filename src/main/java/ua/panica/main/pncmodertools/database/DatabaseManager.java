package ua.panica.main.pncmodertools.database;

import litebans.api.Database;
import org.bukkit.Bukkit;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.model.Report;
import ua.panica.main.pncmodertools.model.ReportStatus;
import ua.panica.main.pncmodertools.model.StaffWorkSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final PncModerTools plugin;

    public DatabaseManager(PncModerTools plugin) {
        this.plugin = plugin;
    }

    public Connection getConnection() throws SQLException {
        try {
            return Database.get().prepareStatement("SELECT 1").getConnection();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get LiteBans connection, using H2 instead");
            return plugin.getDataSource().getConnection();
        }
    }

    public void createTables() {
        runAsync(() -> {
            try (Connection conn = plugin.getDataSource().getConnection()) {
                String reportsTable = "CREATE TABLE IF NOT EXISTS reports (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "reporter_uuid VARCHAR(36) NOT NULL," +
                        "reporter_name VARCHAR(16) NOT NULL," +
                        "target_uuid VARCHAR(36) NOT NULL," +
                        "target_name VARCHAR(16) NOT NULL," +
                        "reason TEXT NOT NULL," +
                        "status VARCHAR(20) NOT NULL," +
                        "handler_uuid VARCHAR(36)," +
                        "handler_name VARCHAR(16)," +
                        "created_at BIGINT NOT NULL," +
                        "handled_at BIGINT DEFAULT 0" +
                        ")";

                String workSessionsTable = "CREATE TABLE IF NOT EXISTS work_sessions (" +
                        "player_uuid VARCHAR(36) PRIMARY KEY," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "start_time BIGINT NOT NULL," +
                        "total_time BIGINT DEFAULT 0," +
                        "active BOOLEAN DEFAULT FALSE" +
                        ")";

                String dailyStatsTable = "CREATE TABLE IF NOT EXISTS daily_stats (" +
                        "player_uuid VARCHAR(36)," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "date VARCHAR(10) NOT NULL," +
                        "work_time BIGINT DEFAULT 0," +
                        "mutes INT DEFAULT 0," +
                        "bans INT DEFAULT 0," +
                        "PRIMARY KEY (player_uuid, date)" +
                        ")";

                String staffGroupsTable = "CREATE TABLE IF NOT EXISTS staff_groups (" +
                        "player_uuid VARCHAR(36) PRIMARY KEY," +
                        "staff_group VARCHAR(50) NOT NULL" +
                        ")";

                Statement stmt = conn.createStatement();
                stmt.execute(reportsTable);
                stmt.execute(workSessionsTable);
                stmt.execute(dailyStatsTable);
                stmt.execute(staffGroupsTable);
                stmt.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Integer> createReportAsync(UUID reporterId, String reporterName, UUID targetId, String targetName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO reports (reporter_uuid, reporter_name, target_uuid, target_name, reason, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, reporterId.toString());
                stmt.setString(2, reporterName);
                stmt.setString(3, targetId.toString());
                stmt.setString(4, targetName);
                stmt.setString(5, reason);
                stmt.setString(6, ReportStatus.PENDING.name());
                stmt.setLong(7, System.currentTimeMillis());

                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return -1;
        });
    }

    public CompletableFuture<List<Report>> getActiveReportsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Report> reports = new ArrayList<>();
            String sql = "SELECT * FROM reports WHERE status != ? ORDER BY created_at DESC";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, ReportStatus.RESOLVED.name());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    reports.add(mapReport(rs));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return reports;
        });
    }

    public CompletableFuture<Report> getReportByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM reports WHERE id = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return mapReport(rs);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    public CompletableFuture<Void> updateReportStatusAsync(int id, ReportStatus status, UUID handlerId, String handlerName) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE reports SET status = ?, handler_uuid = ?, handler_name = ?, handled_at = ? WHERE id = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, status.name());
                stmt.setString(2, handlerId != null ? handlerId.toString() : null);
                stmt.setString(3, handlerName);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.setInt(5, id);

                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> deleteReportAsync(int id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM reports WHERE id = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Integer> deleteExpiredReportsAsync(long currentTime) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM reports WHERE created_at < ?";
            long expiryTime = currentTime - (24 * 60 * 60 * 1000);

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, expiryTime);

                int deleted = stmt.executeUpdate();
                return deleted;

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return 0;
        });
    }

    private Report mapReport(ResultSet rs) throws SQLException {
        UUID handlerId = rs.getString("handler_uuid") != null ?
                UUID.fromString(rs.getString("handler_uuid")) : null;

        return new Report(
                rs.getInt("id"),
                UUID.fromString(rs.getString("reporter_uuid")),
                rs.getString("reporter_name"),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                rs.getString("reason"),
                ReportStatus.fromString(rs.getString("status")),
                handlerId,
                rs.getString("handler_name"),
                rs.getLong("created_at"),
                rs.getLong("handled_at")
        );
    }

    public CompletableFuture<StaffWorkSession> getWorkSessionAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM work_sessions WHERE player_uuid = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return new StaffWorkSession(
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getLong("start_time"),
                            rs.getLong("total_time"),
                            rs.getBoolean("active")
                    );
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    public CompletableFuture<Void> saveWorkSessionAsync(StaffWorkSession session) {
        return CompletableFuture.runAsync(() -> {
            String sql = "MERGE INTO work_sessions (player_uuid, player_name, start_time, total_time, active) " +
                    "KEY (player_uuid) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, session.getPlayerId().toString());
                stmt.setString(2, session.getPlayerName());
                stmt.setLong(3, session.getStartTime());
                stmt.setLong(4, session.getTotalTime());
                stmt.setBoolean(5, session.isActive());

                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> deleteWorkSessionAsync(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM work_sessions WHERE player_uuid = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> updateDailyStatsAsync(UUID playerId, String playerName, String date, long workTime, int mutes, int bans) {
        return CompletableFuture.runAsync(() -> {
            String sql = "MERGE INTO daily_stats (player_uuid, player_name, date, work_time, mutes, bans) " +
                    "KEY (player_uuid, date) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                stmt.setString(2, playerName);
                stmt.setString(3, date);
                stmt.setLong(4, workTime);
                stmt.setInt(5, mutes);
                stmt.setInt(6, bans);

                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> addWorkTimeAsync(UUID playerId, String playerName, String date, long workTime) {
        return CompletableFuture.runAsync(() -> {
            String selectSql = "SELECT work_time FROM daily_stats WHERE player_uuid = ? AND date = ?";
            String insertSql = "MERGE INTO daily_stats (player_uuid, player_name, date, work_time, mutes, bans) " +
                    "KEY (player_uuid, date) VALUES (?, ?, ?, ?, 0, 0)";

            try (Connection conn = plugin.getDataSource().getConnection()) {
                long currentTime = 0;

                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, playerId.toString());
                    selectStmt.setString(2, date);
                    ResultSet rs = selectStmt.executeQuery();
                    if (rs.next()) {
                        currentTime = rs.getLong("work_time");
                    }
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, playerId.toString());
                    insertStmt.setString(2, playerName);
                    insertStmt.setString(3, date);
                    insertStmt.setLong(4, currentTime + workTime);
                    insertStmt.executeUpdate();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<DailyStats> getDailyStatsAsync(UUID playerId, String date) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM daily_stats WHERE player_uuid = ? AND date = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                stmt.setString(2, date);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return new DailyStats(
                            rs.getString("player_name"),
                            rs.getLong("work_time"),
                            rs.getInt("mutes"),
                            rs.getInt("bans")
                    );
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    public CompletableFuture<long[]> getWeekStatsAsync(UUID playerId, List<String> dates) {
        return CompletableFuture.supplyAsync(() -> {
            long totalWorkTime = 0;
            int totalMutes = 0;
            int totalBans = 0;

            String sql = "SELECT work_time, mutes, bans FROM daily_stats WHERE player_uuid = ? AND date = ?";

            try (Connection conn = plugin.getDataSource().getConnection()) {
                for (String date : dates) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, playerId.toString());
                        stmt.setString(2, date);

                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            totalWorkTime += rs.getLong("work_time");
                            totalMutes += rs.getInt("mutes");
                            totalBans += rs.getInt("bans");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return new long[]{totalWorkTime, totalMutes, totalBans};
        });
    }

    public CompletableFuture<long[]> getAllTimeStatsAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT SUM(work_time) as total_time, SUM(mutes) as total_mutes, SUM(bans) as total_bans " +
                    "FROM daily_stats WHERE player_uuid = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    long workTime = rs.getLong("total_time");
                    int mutes = rs.getInt("total_mutes");
                    int bans = rs.getInt("total_bans");
                    return new long[]{workTime, mutes, bans};
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return new long[]{0, 0, 0};
        });
    }

    public CompletableFuture<List<StaffWorkSession>> getActiveWorkSessionsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<StaffWorkSession> sessions = new ArrayList<>();
            String sql = "SELECT * FROM work_sessions WHERE active = TRUE";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    sessions.add(new StaffWorkSession(
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getLong("start_time"),
                            rs.getLong("total_time"),
                            rs.getBoolean("active")
                    ));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return sessions;
        });
    }

    public CompletableFuture<Void> saveStaffGroupAsync(UUID playerId, String group) {
        return CompletableFuture.runAsync(() -> {
            String sql = "MERGE INTO staff_groups (player_uuid, staff_group) KEY (player_uuid) VALUES (?, ?)";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                stmt.setString(2, group);
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<String> getStaffGroupAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT staff_group FROM staff_groups WHERE player_uuid = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getString("staff_group");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    public CompletableFuture<Void> deleteStaffGroupAsync(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM staff_groups WHERE player_uuid = ?";

            try (Connection conn = plugin.getDataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static class DailyStats {
        private final String playerName;
        private final long workTime;
        private final int mutes;
        private final int bans;

        public DailyStats(String playerName, long workTime, int mutes, int bans) {
            this.playerName = playerName;
            this.workTime = workTime;
            this.mutes = mutes;
            this.bans = bans;
        }

        public String getPlayerName() {
            return playerName;
        }

        public long getWorkTime() {
            return workTime;
        }

        public int getMutes() {
            return mutes;
        }

        public int getBans() {
            return bans;
        }
    }
}