package ua.panica.main.pncmodertools.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.panica.main.pncmodertools.PncModerTools;

@Command(name = "report")
public class ReportCommand {

    private final PncModerTools plugin;

    public ReportCommand(PncModerTools plugin) {
        this.plugin = plugin;
    }

    @Execute
    public void executeNoArgs(@Context Player sender) {
        Component message = plugin.getConfigManager().getMessage("report.usage");
        plugin.adventure().player(sender).sendMessage(message);
    }

    @Execute
    @Permission("PncModerTools.report")
    public void execute(@Context Player sender, @Arg Player target, @Arg String reason) {
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            Component message = plugin.getConfigManager().getMessage("report.cannot-report-self");
            plugin.adventure().player(sender).sendMessage(message);
            return;
        }

        if (!target.isOnline()) {
            Component message = plugin.getConfigManager().getMessage("report.player-offline",
                    "{target}", target.getName());
            plugin.adventure().player(sender).sendMessage(message);
            return;
        }

        if (plugin.getReportManager().isOnCooldown(sender.getUniqueId())) {
            long remaining = plugin.getReportManager().getCooldownRemaining(sender.getUniqueId());
            Component message = plugin.getConfigManager().getMessage("report.cooldown",
                    "{time}", String.valueOf(remaining));
            plugin.adventure().player(sender).sendMessage(message);
            return;
        }

        plugin.getReportManager().createReport(sender, target, reason).thenAccept(reportId -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (reportId > 0) {
                    Component successMessage = plugin.getConfigManager().getMessage("report.success",
                            "{id}", String.valueOf(reportId),
                            "{target}", target.getName());
                    plugin.adventure().player(sender).sendMessage(successMessage);

                    plugin.getReportManager().setCooldown(sender.getUniqueId());

                    plugin.getReportManager().getReportById(reportId).thenAccept(report -> {
                        if (report != null) {
                            plugin.getReportManager().notifyStaff(report);
                        }
                    });
                } else {
                    Component errorMessage = plugin.getConfigManager().getMessage("report.error");
                    plugin.adventure().player(sender).sendMessage(errorMessage);
                }
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                throwable.printStackTrace();
                Component errorMessage = plugin.getConfigManager().getMessage("report.error");
                plugin.adventure().player(sender).sendMessage(errorMessage);
            });
            return null;
        });
    }
}