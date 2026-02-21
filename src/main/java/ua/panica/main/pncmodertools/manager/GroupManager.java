package ua.panica.main.pncmodertools.manager;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ua.panica.main.pncmodertools.PncModerTools;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GroupManager {

    private final PncModerTools plugin;
    private final LuckPerms luckPerms;
    private final Map<UUID, String> cachedStaffGroups;

    public GroupManager(PncModerTools plugin) {
        this.plugin = plugin;
        this.cachedStaffGroups = new ConcurrentHashMap<>();

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
        } else {
            this.luckPerms = null;
        }
    }

    public boolean isAvailable() {
        return luckPerms != null && plugin.getConfigManager().isStaffGroupsEnabled();
    }

    public void switchToStaffGroup(Player player) {
        if (!isAvailable()) return;

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        UUID playerId = player.getUniqueId();
        List<String> staffGroups = plugin.getConfigManager().getStaffGroups();
        List<String> currentGroups = getCurrentGroups(user);

        String cachedGroup = cachedStaffGroups.get(playerId);
        if (cachedGroup != null && staffGroups.contains(cachedGroup)) {
            addGroup(user, cachedGroup);
            return;
        }

        plugin.getDatabaseManager().getStaffGroupAsync(playerId).thenAccept(savedGroup -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (savedGroup != null && staffGroups.contains(savedGroup)) {
                    cachedStaffGroups.put(playerId, savedGroup);
                    addGroup(user, savedGroup);
                } else {
                    String currentStaffGroup = null;
                    for (String group : currentGroups) {
                        if (staffGroups.contains(group)) {
                            currentStaffGroup = group;
                            break;
                        }
                    }

                    if (currentStaffGroup == null) {
                        String firstStaffGroup = staffGroups.isEmpty() ? null : staffGroups.get(0);
                        if (firstStaffGroup != null) {
                            addGroup(user, firstStaffGroup);
                        }
                    }
                }
            });
        });
    }

    public void switchToNonStaffGroup(Player player) {
        if (!isAvailable()) return;

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        UUID playerId = player.getUniqueId();
        List<String> staffGroups = plugin.getConfigManager().getStaffGroups();
        List<String> currentGroups = getCurrentGroups(user);

        String currentStaffGroup = null;
        for (String group : currentGroups) {
            if (staffGroups.contains(group)) {
                currentStaffGroup = group;
                break;
            }
        }

        if (currentStaffGroup != null) {
            cachedStaffGroups.put(playerId, currentStaffGroup);
            plugin.getDatabaseManager().saveStaffGroupAsync(playerId, currentStaffGroup);

            removeGroup(user, currentStaffGroup);

            String nonStaffGroup = getNextLowerGroup(currentGroups, staffGroups);

            if (nonStaffGroup != null && !currentGroups.contains(nonStaffGroup)) {
                addGroup(user, nonStaffGroup);
            }
        }
    }

    private List<String> getCurrentGroups(User user) {
        return user.getNodes().stream()
                .filter(node -> node.getKey().startsWith("group."))
                .map(node -> node.getKey().substring(6))
                .collect(Collectors.toList());
    }

    private String getNextLowerGroup(List<String> currentGroups, List<String> staffGroups) {
        List<String> nonStaffGroups = currentGroups.stream()
                .filter(group -> !staffGroups.contains(group))
                .collect(Collectors.toList());

        if (nonStaffGroups.isEmpty()) {
            return "default";
        }

        return nonStaffGroups.get(0);
    }

    private void addGroup(User user, String group) {
        user.data().add(Node.builder("group." + group).build());
        luckPerms.getUserManager().saveUser(user);
    }

    private void removeGroup(User user, String group) {
        user.data().remove(Node.builder("group." + group).build());
        luckPerms.getUserManager().saveUser(user);
    }
}