package ua.panica.main.pncmodertools.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.entity.Player;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.gui.ReportsGUI;

@Command(name = "reports")
@Permission("hexmodertools.reports.view")
public class ReportsCommand {

    private final PncModerTools plugin;

    public ReportsCommand(PncModerTools plugin) {
        this.plugin = plugin;
    }

    @Execute
    public void execute(@Context Player sender) {
        new ReportsGUI(plugin, sender).open();
    }
}
