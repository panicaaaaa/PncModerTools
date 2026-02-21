package ua.panica.main.pncmodertools.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.entity.Player;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.gui.ReviceGUI;

@Command(name = "revice")
public class ReviceCommand {

    private final PncModerTools plugin;

    public ReviceCommand(PncModerTools plugin) {
        this.plugin = plugin;
    }

    @Execute
    @Permission("hexmodertools.revice.start")
    public void start(@Context Player sender, @Arg Player target) {
        boolean success = plugin.getCheckManager().startCheck(sender, target);
        if (success) {
            new ReviceGUI(plugin, sender).open();
        }
    }

    @Execute(name = "setposs")
    @Permission("hexmodertools.revice.setposs")
    public void setCheckpoint(@Context Player sender) {
        plugin.getReviceConfig().saveCheckpoint(sender.getLocation());
        sender.sendMessage("§aТочка проверки сохранена.");
    }

    @Execute(name = "setspawn")
    @Permission("hexmodertools.revice.setspawn")
    public void setSpawn(@Context Player sender) {
        plugin.getReviceConfig().saveSpawn(sender.getLocation());
        sender.sendMessage("§aТочка спавна сохранена.");
    }

    @Execute(name = "gui")
    @Permission("hexmodertools.revice.start")
    public void openGui(@Context Player sender) {
        new ReviceGUI(plugin, sender).open();
    }

    @Execute(name = "reload")
    @Permission("hexmodertools.revice.reload")
    public void reload(@Context Player sender) {
        plugin.getReviceConfig().reload();
        sender.sendMessage("§aКонфиг revice.yml перезагружен!");
    }
}