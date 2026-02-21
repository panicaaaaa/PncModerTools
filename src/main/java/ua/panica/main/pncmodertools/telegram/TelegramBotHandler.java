package ua.panica.main.pncmodertools.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import ua.panica.main.pncmodertools.PncModerTools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TelegramBotHandler {

    private final PncModerTools plugin;
    private TelegramBot bot;
    private volatile boolean running = false;

    public TelegramBotHandler(PncModerTools plugin) {
        this.plugin = plugin;
        String token = plugin.getBotConfig().getToken();

        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("Telegram bot token is not configured!");
            this.bot = null;
            return;
        }

        try {
            this.bot = new TelegramBot(token);
            setupListener();
            this.running = true;
            plugin.getLogger().info("Telegram bot initialized successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Telegram bot: " + e.getMessage());
            e.printStackTrace();
            this.bot = null;
        }
    }

    private void setupListener() {
        if (bot == null) return;

        bot.setUpdatesListener(updates -> {
            if (!running) {
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }

            for (Update update : updates) {
                try {
                    if (update.message() != null && update.message().text() != null) {
                        handleCommand(update);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error handling telegram update: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                if (e.response().errorCode() == 409) {
                    plugin.getLogger().warning("Another bot instance is already running! Stopping this one...");
                    stop();
                    return;
                }
            }
            plugin.getLogger().warning("Telegram bot error: " + e.getMessage());
        });
    }

    private void handleCommand(Update update) {
        if (!running) return;

        String text = update.message().text();
        long chatId = update.message().chat().id();

        if (!text.startsWith("/")) {
            return;
        }

        String[] parts = text.split(" ");
        String command = parts[0].toLowerCase();

        if (command.equals("/stats") || command.equals("/stats@" + bot.getToken().split(":")[0])) {
            if (parts.length < 2) {
                sendMessage(chatId, plugin.getBotConfig().getUsageMessage());
                return;
            }

            String playerName = parts[1];
            String period = parts.length >= 3 ? parts[2].toLowerCase() : "today";

            handleStatsCommand(chatId, playerName, period);
        } else if (command.equals("/start")) {
            sendMessage(chatId, plugin.getBotConfig().getStartMessage());
        } else {
            sendMessage(chatId, plugin.getBotConfig().getStartMessage());
        }
    }

    private void handleStatsCommand(long chatId, String playerName, String period) {
        if (!running) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);

            if (!player.hasPlayedBefore() && !player.isOnline()) {
                sendMessage(chatId, plugin.getBotConfig().getPlayerNotFoundMessage());
                return;
            }

            UUID playerId = player.getUniqueId();

            if (period.equals("today")) {
                handleTodayStats(chatId, playerId, playerName);
            } else if (period.equals("week")) {
                handleWeekStats(chatId, playerId, playerName);
            } else if (period.equals("alltime")) {
                handleAllTimeStats(chatId, playerId, playerName);
            } else {
                handleDateStats(chatId, playerId, playerName, period);
            }
        });
    }

    private void handleTodayStats(long chatId, UUID playerId, String playerName) {
        String today = plugin.getStaffWorkManager().getTodayDate();
        plugin.getStatisticsManager().updateDailyStats(playerId, playerName, today);

        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }

        plugin.getDatabaseManager().getDailyStatsAsync(playerId, today).thenAccept(stats -> {
            String periodText = plugin.getBotConfig().getPeriodToday();

            if (stats == null) {
                String message = plugin.getBotConfig().formatStatsMessage(
                        playerName,
                        periodText,
                        plugin.getBotConfig().getEmptyTime(),
                        0,
                        0
                );
                sendMessage(chatId, message);
                return;
            }

            String timeFormatted = plugin.getStaffWorkManager().formatTime(stats.getWorkTime());
            String message = plugin.getBotConfig().formatStatsMessage(
                    playerName,
                    periodText,
                    timeFormatted,
                    stats.getMutes(),
                    stats.getBans()
            );

            sendMessage(chatId, message);
        });
    }

    private void handleDateStats(long chatId, UUID playerId, String playerName, String dateStr) {
        try {
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd.MM.yy");
            LocalDate date = LocalDate.parse(dateStr, inputFormat);
            String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            plugin.getStatisticsManager().updateDailyStats(playerId, playerName, formattedDate);

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            plugin.getDatabaseManager().getDailyStatsAsync(playerId, formattedDate).thenAccept(stats -> {
                String periodText = plugin.getBotConfig().getPeriodDate().replace("%date%", dateStr);

                if (stats == null) {
                    String message = plugin.getBotConfig().formatStatsMessage(
                            playerName,
                            periodText,
                            plugin.getBotConfig().getEmptyTime(),
                            0,
                            0
                    );
                    sendMessage(chatId, message);
                    return;
                }

                String timeFormatted = plugin.getStaffWorkManager().formatTime(stats.getWorkTime());
                String message = plugin.getBotConfig().formatStatsMessage(
                        playerName,
                        periodText,
                        timeFormatted,
                        stats.getMutes(),
                        stats.getBans()
                );

                sendMessage(chatId, message);
            });

        } catch (Exception e) {
            sendMessage(chatId, plugin.getBotConfig().getInvalidDateMessage());
        }
    }

    private void handleWeekStats(long chatId, UUID playerId, String playerName) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        List<String> dates = new ArrayList<>();
        for (LocalDate date = weekAgo; !date.isAfter(today); date = date.plusDays(1)) {
            dates.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        for (String date : dates) {
            plugin.getStatisticsManager().updateDailyStats(playerId, playerName, date);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }

        plugin.getDatabaseManager().getWeekStatsAsync(playerId, dates).thenAccept(totalStats -> {
            String periodText = plugin.getBotConfig().getPeriodWeek();
            String timeFormatted = plugin.getStaffWorkManager().formatTime(totalStats[0]);

            String message = plugin.getBotConfig().formatStatsMessage(
                    playerName,
                    periodText,
                    timeFormatted,
                    (int) totalStats[1],
                    (int) totalStats[2]
            );

            sendMessage(chatId, message);
        });
    }

    private void handleAllTimeStats(long chatId, UUID playerId, String playerName) {
        plugin.getDatabaseManager().getAllTimeStatsAsync(playerId).thenAccept(totalStats -> {
            String periodText = plugin.getBotConfig().getPeriodAlltime();
            String timeFormatted = plugin.getStaffWorkManager().formatTime(totalStats[0]);

            String message = plugin.getBotConfig().formatStatsMessage(
                    playerName,
                    periodText,
                    timeFormatted,
                    (int) totalStats[1],
                    (int) totalStats[2]
            );

            sendMessage(chatId, message);
        });
    }

    private void sendMessage(long chatId, String text) {
        if (bot == null || !running) return;

        try {
            SendMessage request = new SendMessage(chatId, text);

            if (plugin.getBotConfig().isMarkdownEnabled()) {
                request.parseMode(ParseMode.MarkdownV2);
            }

            bot.execute(request);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send Telegram message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (!running) return;

        running = false;

        if (bot != null) {
            try {
                bot.removeGetUpdatesListener();
                plugin.getLogger().info("Telegram bot stopped successfully!");
            } catch (Exception e) {
                plugin.getLogger().warning("Error stopping Telegram bot: " + e.getMessage());
            }
        }
    }
}