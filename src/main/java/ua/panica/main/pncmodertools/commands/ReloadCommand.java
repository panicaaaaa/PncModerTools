package ua.panica.main.pncmodertools.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.command.CommandSender;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.telegram.TelegramBotHandler;

@Command(name = "pncmoder", aliases = "pnm")
public class ReloadCommand {

    private final PncModerTools plugin;

    public ReloadCommand(PncModerTools plugin) {
        this.plugin = plugin;
    }

    @Execute(name = "reload")
    @Permission("PncModerTools.reload")
    public void reload(@Context CommandSender sender) {

        long start = System.currentTimeMillis();

        plugin.getConfigManager().reload();

        try {
            if (plugin.getDataSource() != null && !plugin.getDataSource().isClosed()) {
                plugin.getDataSource().close();
            }
            plugin.initializeDatabase();
            plugin.getDatabaseManager().createTables();
        } catch (Exception e) {
            return;
        }

        if (plugin.getConfigManager().isTelegramEnabled()) {
            if (plugin.telegramBot != null) {
                plugin.telegramBot.stop();
            }
            plugin.telegramBot = new TelegramBotHandler(plugin);
        } else {
            if (plugin.telegramBot != null) {
                plugin.telegramBot.stop();
                plugin.telegramBot = null;
            }
        }

        if (plugin.liteCommands != null) {
            plugin.liteCommands.unregister();
        }
        plugin.registerCommands();

        long time = System.currentTimeMillis() - start;
    }
}