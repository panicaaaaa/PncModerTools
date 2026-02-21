package ua.panica.main.pncmodertools.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.model.GroupedReport;
import ua.panica.main.pncmodertools.model.Report;
import ua.panica.main.pncmodertools.model.ReportStatus;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ReportManager {

    private final PncModerTools plugin;
    private final Map<UUID, Long> cooldowns;

    public ReportManager(PncModerTools plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Integer> createReport(Player reporter, Player target, String reason) {
        return plugin.getDatabaseManager().createReportAsync(
                reporter.getUniqueId(),
                reporter.getName(),
                target.getUniqueId(),
                target.getName(),
                reason
        );
    }

    public CompletableFuture<List<GroupedReport>> getGroupedReports() {
        return plugin.getDatabaseManager().getActiveReportsAsync().thenApply(reports -> {
            Map<UUID, GroupedReport> grouped = new LinkedHashMap<>();

            for (Report report : reports) {
                UUID targetId = report.getTargetId();

                if (!grouped.containsKey(targetId)) {
                    grouped.put(targetId, new GroupedReport(targetId, report.getTargetName()));
                }

                grouped.get(targetId).addReport(report);
            }

            return new ArrayList<>(grouped.values());
        });
    }

    public CompletableFuture<Report> getReportById(int id) {
        return plugin.getDatabaseManager().getReportByIdAsync(id);
    }

    public void acceptReport(int reportId, Player handler) {
        plugin.getDatabaseManager().updateReportStatusAsync(
                reportId,
                ReportStatus.IN_PROGRESS,
                handler.getUniqueId(),
                handler.getName()
        );
    }

    public void acceptAllReports(UUID targetId, Player handler) {
        plugin.getDatabaseManager().getActiveReportsAsync().thenAccept(reports -> {
            for (Report report : reports) {
                if (report.getTargetId().equals(targetId) && report.getStatus() == ReportStatus.PENDING) {
                    acceptReport(report.getId(), handler);
                }
            }
        });
    }

    public void closeReport(int reportId, Player handler) {
        plugin.getDatabaseManager().updateReportStatusAsync(
                reportId,
                ReportStatus.RESOLVED,
                handler.getUniqueId(),
                handler.getName()
        ).thenRun(() -> {
            plugin.getDatabaseManager().deleteReportAsync(reportId);
        });
    }

    public void closeAllReports(UUID targetId, Player handler) {
        plugin.getDatabaseManager().getActiveReportsAsync().thenAccept(reports -> {
            for (Report report : reports) {
                if (report.getTargetId().equals(targetId)) {
                    closeReport(report.getId(), handler);
                }
            }
        });
    }

    public boolean isOnCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) {
            return false;
        }

        long cooldownEnd = cooldowns.get(playerId);
        long now = System.currentTimeMillis();

        if (now >= cooldownEnd) {
            cooldowns.remove(playerId);
            return false;
        }

        return true;
    }

    public long getCooldownRemaining(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) {
            return 0;
        }

        long cooldownEnd = cooldowns.get(playerId);
        long now = System.currentTimeMillis();

        return Math.max(0, (cooldownEnd - now) / 1000);
    }

    public void setCooldown(UUID playerId) {
        int cooldownSeconds = plugin.getConfigManager().getReportCooldown();
        long cooldownEnd = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.put(playerId, cooldownEnd);
    }

    public void notifyStaff(Report report) {
        if (!plugin.getConfigManager().shouldNotifyStaff()) {
            return;
        }

        String permission = plugin.getConfigManager().getReportNotifyPermission();
        Component notification = plugin.getConfigManager().getMessage(
                "report.staff-notification",
                "{id}", String.valueOf(report.getId()),
                "{reporter}", report.getReporterName(),
                "{target}", report.getTargetName(),
                "{reason}", report.getReason()
        );

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission(permission)) {
                    plugin.adventure().player(online).sendMessage(notification);
                }
            }
        });
    }
}