package ua.panica.main.pncmodertools.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.panica.main.pncmodertools.PncModerTools;

import java.io.File;

public class BotConfigManager {

    private final PncModerTools plugin;
    private File configFile;
    private FileConfiguration config;

    public BotConfigManager(PncModerTools plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "bot.yml");

        if (!configFile.exists()) {
            plugin.saveResource("bot.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public String getToken() {
        return config.getString("token", "YOUR_BOT_TOKEN_HERE");
    }

    public boolean isMarkdownEnabled() {
        return config.getBoolean("markdown", true);
    }

    public String getStartMessage() {
        return config.getString("messages.start.text", "👋 Привет!");
    }

    public String getUsageMessage() {
        return config.getString("messages.usage.text", "Используй /stats <ник>");
    }

    public String getPlayerNotFoundMessage() {
        return config.getString("messages.player_not_found.text", "❌ Игрок не найден!");
    }

    public String getInvalidDateMessage() {
        return config.getString("messages.invalid_date.text", "❌ Неверный формат даты!");
    }

    public String getStatsHeader() {
        return config.getString("messages.stats.header", "📊 Статистика для %player% за %period%:");
    }

    public String getWorkTimeFormat() {
        return config.getString("messages.stats.work_time", "⏰ Время работы: %time%");
    }

    public String getMutesFormat() {
        return config.getString("messages.stats.mutes", "🔇 Мутов: %mutes%");
    }

    public String getBansFormat() {
        return config.getString("messages.stats.bans", "🔨 Банов: %bans%");
    }

    public String getPeriodToday() {
        return config.getString("messages.stats.periods.today", "сегодня");
    }

    public String getPeriodDate() {
        return config.getString("messages.stats.periods.date", "%date%");
    }

    public String getPeriodWeek() {
        return config.getString("messages.stats.periods.week", "неделю");
    }

    public String getPeriodAlltime() {
        return config.getString("messages.stats.periods.alltime", "всё время");
    }

    public String getEmptyTime() {
        return config.getString("format.empty_time", "00:00:00");
    }

    public String formatStatsMessage(String playerName, String period, String time, int mutes, int bans) {
        String escapedPlayer = escapeMarkdownV2(playerName);
        String escapedPeriod = escapeMarkdownV2(period);

        String header = getStatsHeader()
                .replace("%player%", escapedPlayer)
                .replace("%period%", escapedPeriod);

        String workTime = getWorkTimeFormat().replace("%time%", time);
        String mutesLine = getMutesFormat().replace("%mutes%", String.valueOf(mutes));
        String bansLine = getBansFormat().replace("%bans%", String.valueOf(bans));

        return header + "\n\n" + workTime + "\n" + mutesLine + "\n" + bansLine;
    }

    private String escapeMarkdownV2(String text) {
        if (text == null) return "";

        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}