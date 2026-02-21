package ua.panica.main.pncmodertools.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.model.CheckSession;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CheckManager {

    private final PncModerTools plugin;
    private final MiniMessage miniMessage;
    private final Map<UUID, CheckSession> bySuspect;
    private final Map<UUID, CheckSession> byModerator;

    public CheckManager(PncModerTools plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.bySuspect = new ConcurrentHashMap<>();
        this.byModerator = new ConcurrentHashMap<>();
    }

    public Collection<CheckSession> getSessions() {
        return Collections.unmodifiableCollection(bySuspect.values());
    }

    public boolean startCheck(Player moderator, Player suspect) {
        if (bySuspect.containsKey(suspect.getUniqueId())) {
            moderator.sendMessage("§eЭтот игрок уже на проверке.");
            return false;
        }

        if (byModerator.containsKey(moderator.getUniqueId())) {
            moderator.sendMessage("§eУ вас уже есть активная проверка.");
            return false;
        }

        Location checkpoint = plugin.getReviceConfig().getCheckpoint();
        if (checkpoint == null) {
            moderator.sendMessage("§cТочка проверки не установлена! Используйте /revice setposs");
            return false;
        }

        long duration = plugin.getReviceConfig().getDefaultSeconds();
        CheckSession session = new CheckSession(moderator, suspect, suspect.getLocation().clone(), duration);

        bySuspect.put(suspect.getUniqueId(), session);
        byModerator.put(moderator.getUniqueId(), session);

        suspect.teleport(checkpoint);
        session.frozen = true;

        List<String> startMessages = plugin.getReviceConfig().getStartMessages();
        for (String msg : startMessages) {
            plugin.adventure().player(suspect).sendMessage(miniMessage.deserialize(msg));
        }

        sendTitleAndSound(suspect, "start");

        moderator.sendMessage("§aПроверка начата: §f" + suspect.getName());
        return true;
    }

    public void stopCheck(Player moderator) {
        CheckSession session = byModerator.get(moderator.getUniqueId());
        if (session == null) {
            moderator.sendMessage("§cНет активной проверки.");
            return;
        }

        finishCheck(session, true);
        moderator.sendMessage("§aПроверка завершена.");
    }

    public void banSuspect(Player moderator) {
        CheckSession session = byModerator.get(moderator.getUniqueId());
        if (session == null) {
            moderator.sendMessage("§cНет активной проверки.");
            return;
        }

        String command = plugin.getReviceConfig().getManualBanCommand()
                .replace("%player%", session.getSuspect().getName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(moderator, command);
        });

        finishCheck(session, false);
        moderator.sendMessage("§cИгрок забанен и проверка завершена.");
    }

    public void addTime(Player moderator, int seconds) {
        CheckSession session = byModerator.get(moderator.getUniqueId());
        if (session == null) {
            moderator.sendMessage("§cНет активной проверки.");
            return;
        }

        session.endsAtMillis += seconds * 1000L;
        moderator.sendMessage("§aДобавлено времени: §f" + seconds + "§a сек.");
    }

    public void pauseTimer(Player moderator) {
        CheckSession session = byModerator.get(moderator.getUniqueId());
        if (session == null) {
            moderator.sendMessage("§cНет активной проверки.");
            return;
        }

        session.timerPaused = !session.timerPaused;
        moderator.sendMessage(session.timerPaused ? "§eТаймер поставлен на паузу." : "§aТаймер возобновлён.");

        if (session.timerPaused && session.getSuspect().isOnline()) {
            String pausedMsg = plugin.getReviceConfig().getPausedMessage();
            plugin.adventure().player(session.getSuspect()).sendActionBar(miniMessage.deserialize(pausedMsg));
        }
    }

    public void teleportAgain(Player moderator) {
        CheckSession session = byModerator.get(moderator.getUniqueId());
        if (session == null) {
            moderator.sendMessage("§cНет активной проверки.");
            return;
        }

        Location checkpoint = plugin.getReviceConfig().getCheckpoint();
        if (checkpoint != null) {
            session.getSuspect().teleport(checkpoint);
            moderator.sendMessage("§aПодозреваемый перемещён на точку проверки.");
        }
    }

    public boolean isFrozen(Player player) {
        CheckSession session = bySuspect.get(player.getUniqueId());
        return session != null && session.frozen;
    }

    public boolean hasSession(Player player) {
        return bySuspect.containsKey(player.getUniqueId()) || byModerator.containsKey(player.getUniqueId());
    }

    public Optional<CheckSession> getSession(Player player) {
        CheckSession session = bySuspect.get(player.getUniqueId());
        if (session != null) return Optional.of(session);

        session = byModerator.get(player.getUniqueId());
        return Optional.ofNullable(session);
    }

    public void markQuit(Player suspect) {
        CheckSession session = bySuspect.get(suspect.getUniqueId());
        if (session == null) return;

        if (!plugin.getReviceConfig().isPunishOnQuitEnabled()) return;

        int graceSec = plugin.getReviceConfig().getPunishGraceSeconds();
        session.graceUntil = System.currentTimeMillis() + (graceSec * 1000L);
    }

    public void checkPunishTimeouts() {
        long now = System.currentTimeMillis();
        List<CheckSession> toPunish = new ArrayList<>();

        for (CheckSession session : bySuspect.values()) {
            if (session.graceUntil > 0 && now > session.graceUntil) {
                toPunish.add(session);
            }
        }

        for (CheckSession session : toPunish) {
            String command = plugin.getReviceConfig().getPunishCommand()
                    .replace("%player%", session.getSuspect().getName());

            Player moderator = session.getModerator();

            if (moderator != null && moderator.isOnline()) {
                Bukkit.dispatchCommand(moderator, command);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }

            finishCheck(session, false);
        }
    }

    public void onJoin(Player suspect) {
        CheckSession session = bySuspect.get(suspect.getUniqueId());
        if (session == null) return;

        session.graceUntil = 0;

        Location checkpoint = plugin.getReviceConfig().getCheckpoint();
        if (checkpoint != null) {
            suspect.teleport(checkpoint);
        }

        suspect.sendMessage("§eВы вернулись в режим проверки.");
    }

    public void stopAllChecks() {
        for (CheckSession session : new ArrayList<>(bySuspect.values())) {
            finishCheck(session, false);
        }
    }

    private void finishCheck(CheckSession session, boolean sendMessages) {
        session.frozen = false;
        session.timerPaused = true;

        Player suspect = session.getSuspect();
        if (suspect.isOnline()) {
            Location spawn = plugin.getReviceConfig().getSpawn();
            if (spawn != null) {
                suspect.teleport(spawn);
            } else {
                suspect.teleport(session.getOriginalLocation());
            }

            if (sendMessages) {
                List<String> stopMessages = plugin.getReviceConfig().getStopMessages();
                for (String msg : stopMessages) {
                    plugin.adventure().player(suspect).sendMessage(miniMessage.deserialize(msg));
                }

                sendTitleAndSound(suspect, "stop");
            }
        }

        bySuspect.remove(session.getSuspectId());
        byModerator.remove(session.getModeratorId());
    }

    private void sendTitleAndSound(Player player, String path) {
        String titleText = plugin.getReviceConfig().getTitleText(path);
        String subtitleText = plugin.getReviceConfig().getSubtitleText(path);

        if (titleText != null && !titleText.isEmpty()) {
            Component title = miniMessage.deserialize(titleText);
            Component subtitle = subtitleText != null ? miniMessage.deserialize(subtitleText) : Component.empty();

            int fadeIn = plugin.getReviceConfig().getTitleFadeIn(path);
            int stay = plugin.getReviceConfig().getTitleStay(path);
            int fadeOut = plugin.getReviceConfig().getTitleFadeOut(path);

            Title.Times times = Title.Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L)
            );

            plugin.adventure().player(player).showTitle(Title.title(title, subtitle, times));
        }

        String soundData = plugin.getReviceConfig().getSound(path);
        if (soundData != null && !soundData.isEmpty()) {
            String[] parts = soundData.split(",");
            try {
                Sound sound = Sound.valueOf(parts[0].trim());
                float volume = parts.length > 1 ? Float.parseFloat(parts[1].trim()) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2].trim()) : 1.0f;
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception ignored) {}
        }
    }

   private void sendPlayerTransfer(Player player, String path) {
        
   }
}