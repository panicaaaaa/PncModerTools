package ua.panica.main.pncmodertools.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.manager.CheckManager;
import ua.panica.main.pncmodertools.model.CheckSession;

import java.util.Optional;

public class ReviceChatListener implements Listener {

    private final PncModerTools plugin;
    private final CheckManager checkManager;
    private final MiniMessage miniMessage;

    public ReviceChatListener(PncModerTools plugin, CheckManager checkManager) {
        this.plugin = plugin;
        this.checkManager = checkManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        Optional<CheckSession> opt = checkManager.getSession(sender);

        if (opt.isEmpty()) return;

        event.setCancelled(true);

        CheckSession session = opt.get();
        String prefix = plugin.getReviceConfig().getChatPrefix();
        String roleFormat;

        if (sender.getUniqueId().equals(session.getSuspectId())) {
            roleFormat = plugin.getReviceConfig().getSuspectRole();
        } else {
            roleFormat = plugin.getReviceConfig().getModerRole();
        }

        String format = plugin.getReviceConfig().getChatFormat()
                .replace("%prefix%", prefix)
                .replace("%role%", roleFormat)
                .replace("%name%", sender.getName())
                .replace("%message%", event.getMessage());

        Component message = miniMessage.deserialize(format);

        plugin.adventure().player(session.getModerator()).sendMessage(message);
        plugin.adventure().player(session.getSuspect()).sendMessage(message);
    }
}