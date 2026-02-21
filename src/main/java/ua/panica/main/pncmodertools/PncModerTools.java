package ua.panica.main.pncmodertools;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ua.panica.main.pncmodertools.commands.*;
import ua.panica.main.pncmodertools.config.BotConfigManager;
import ua.panica.main.pncmodertools.config.ConfigManager;
import ua.panica.main.pncmodertools.config.ReviceConfigManager;
import ua.panica.main.pncmodertools.database.DatabaseManager;
import ua.panica.main.pncmodertools.listener.*;
import ua.panica.main.pncmodertools.manager.*;
import ua.panica.main.pncmodertools.telegram.TelegramBotHandler;

public final class PncModerTools extends JavaPlugin {

    private static PncModerTools instance;
    private BukkitAudiences adventure;
    private HikariDataSource dataSource;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private ReportManager reportManager;
    private StaffWorkManager staffWorkManager;
    private StatisticsManager statisticsManager;
    private GroupManager groupManager;
    private ReviceConfigManager reviceConfig;
    private CheckManager checkManager;
    public TelegramBotHandler telegramBot;
    private BotConfigManager botConfig;
    public LiteCommands<CommandSender> liteCommands;

    @Override
    public void onEnable() {
        instance = this;
        this.adventure = BukkitAudiences.create(this);
        saveDefaultConfig();
        this.reviceConfig = new ReviceConfigManager(this);
        this.configManager = new ConfigManager(this);
        initializeDatabase();
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.createTables();
        this.reportManager = new ReportManager(this);
        this.staffWorkManager = new StaffWorkManager(this);
        this.statisticsManager = new StatisticsManager(this);
        this.groupManager = new GroupManager(this);
        this.checkManager = new CheckManager(this);
        this.botConfig = new BotConfigManager(this);
        if (configManager.isTelegramEnabled()) {
            this.telegramBot = new TelegramBotHandler(this);
        }
        registerCommands();
        registerListeners();

        new ua.panica.main.pncmodertools.task.ReviceActionBarTask(this).runTaskTimer(this, 20L, 20L);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        hello();

    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FreezeListener(checkManager), this);
        Bukkit.getPluginManager().registerEvents(new ReviceQuitListener(checkManager), this);
        Bukkit.getPluginManager().registerEvents(new ReviceJoinListener(checkManager), this);
        Bukkit.getPluginManager().registerEvents(new ReviceCommandBlockListener(checkManager), this);
        Bukkit.getPluginManager().registerEvents(new ReviceChatListener(this, checkManager), this);
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    public void onDisable() {
        if (staffWorkManager != null) {
            staffWorkManager.saveAllSessions();
        }
        if (liteCommands != null) {
            liteCommands.unregister();
        }
        if (adventure != null) {
            adventure.close();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        if (telegramBot != null) {
            telegramBot.stop();
        }
        cmdclr("§4✖️§r§f Плагин §c§lPncModerTools§r §fбыл выключен!");
    }

    public void initializeDatabase() {
        HikariConfig config = new HikariConfig();
        String dbPath = getDataFolder().getAbsolutePath() + "/database";
        config.setJdbcUrl("jdbc:h2:" + dbPath);
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
    }

    public void registerCommands() {
        this.liteCommands = LiteBukkitFactory.builder()
                .commands(
                        new ReportCommand(this),
                        new ReportsCommand(this),
                        new StaffWorkCommand(this),
                        new ReloadCommand(this),
                        new ReviceCommand(this)
                )
                .invalidUsage((invocation, result, chain) -> {
                    CommandSender sender = invocation.sender();
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        adventure().player(player).sendMessage(configManager.getMessage("error_syntax"));
                    }
                })
                .build();
    }

    public ReviceConfigManager getReviceConfig() {
        return reviceConfig;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public BotConfigManager getBotConfig() {
        return botConfig;
    }

    public static PncModerTools getInstance() {
        return instance;
    }

    public BukkitAudiences adventure() {
        return adventure;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public StaffWorkManager getStaffWorkManager() {
        return staffWorkManager;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }


    private void cmdclr(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('§', message));
    }

    private void hello() {
        cmdclr("");
        cmdclr("");
        cmdclr("§3______           _     ______           " );
        cmdclr("§3| ___ \\         (_)    |  _  \\          ");
        cmdclr("§3| |_/ /_ _ _ __  _  ___| | | |_____   __");
        cmdclr("§3|  __/ _` | '_ \\| |/ __| | | / _ \\ \\ / /");
        cmdclr("§3| | | (_| | | | | | (__| |/ /  __/\\ V / ");
        cmdclr("§3\\_|  \\__,_|_| |_|_|\\___|___/ \\___| \\_/  " );
        cmdclr("");
        cmdclr("§3⭐ §fПлагин был создан кодером §3Паника §7tg: @pncatk");
        cmdclr("");
        cmdclr("§a✔️§r§f Плагин §3§lPncModerTools§r §fуспешно запущен!");
        cmdclr("");
    }
}