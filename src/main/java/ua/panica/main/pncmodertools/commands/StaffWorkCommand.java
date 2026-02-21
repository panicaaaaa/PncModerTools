package ua.panica.main.pncmodertools.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import ua.panica.main.pncmodertools.PncModerTools;

@Command(name = "staffwork", aliases = {"sw", "work"})
@Permission("hexmodertools.staffwork")
public class StaffWorkCommand {

    private final PncModerTools plugin;

    public StaffWorkCommand(PncModerTools plugin) {
        this.plugin = plugin;
    }

    @Execute
    public void execute(@Context Player sender) {
        plugin.getStaffWorkManager().toggleWork(sender, enabled -> {
            if (enabled) {
                Component message = plugin.getConfigManager().getMessage("staffwork.enabled");
                plugin.adventure().player(sender).sendMessage(message);
            } else {
                plugin.getStaffWorkManager().getSession(sender.getUniqueId(), session -> {
                    if (session != null && session.getTotalTime() > 0) {
                        String formattedTime = plugin.getStaffWorkManager().formatTime(session.getTotalTime());
                        Component message = plugin.getConfigManager().getMessage("staffwork.disabled",
                                "{time}", formattedTime);
                        plugin.adventure().player(sender).sendMessage(message);
                    } else {
                        Component message = plugin.getConfigManager().getMessage("staffwork.disabled-simple");
                        plugin.adventure().player(sender).sendMessage(message);
                    }
                });
            }
        });
    }
}