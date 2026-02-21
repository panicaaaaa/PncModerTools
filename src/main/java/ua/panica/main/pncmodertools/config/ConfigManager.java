package ua.panica.main.pncmodertools.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.gui.GuiItemConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final PncModerTools plugin;
    private final MiniMessage miniMessage;
    private final Map<String, GuiItemConfig> guiItems;

    public ConfigManager(PncModerTools plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.guiItems = new HashMap<>();
        loadGuiItems();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    private void loadGuiItems() {
        guiItems.clear();
        ConfigurationSection itemsSection = getConfig().getConfigurationSection("menu.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    guiItems.put(key, new GuiItemConfig(itemSection));
                }
            }
        }
    }

    public GuiItemConfig getGuiItem(String key) {
        return guiItems.get(key);
    }

    public boolean isTelegramEnabled() {
        return getConfig().getBoolean("telegram.enabled", false);
    }


    public String getMessageString(String path) {
        return getConfig().getString("messages." + path, "");
    }

    public Component getMessage(String path) {
        String message = getMessageString(path);
        return miniMessage.deserialize(message);
    }

    public Component getMessage(String path, String... replacements) {
        String message = getMessageString(path);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        return miniMessage.deserialize(message);
    }

    public int getReportCooldown() {
        return getConfig().getInt("reports.cooldown", 60);
    }

    public boolean shouldNotifyStaff() {
        return getConfig().getBoolean("reports.notify-staff", true);
    }

    public String getReportNotifyPermission() {
        return getConfig().getString("reports.notify-permission", "hexmodertools.reports.notify");
    }

    public String getMenuTitle() {
        return getConfig().getString("menu.title", "Reports");
    }


    public String getMenuStructure() {
        List<String> structure = getConfig().getStringList("menu.structure");
        return String.join("\n", structure);
    }

    public String getReportItemName() {
        return getConfig().getString("menu.report-item.name", "<gradient:#ff0000:#ff7700>Репорт на {target}</gradient>");
    }

    public List<String> getReportItemLore() {
        return getConfig().getStringList("menu.report-item.lore");
    }

    public String getReporterFormat() {
        return getConfig().getString("menu.report-item.reporter-format", "<gray>- <white>{reporter}");
    }

    public String getReasonFormat() {
        return getConfig().getString("menu.report-item.reason-format", "<gray>{index}. <white>{reason}");
    }

    public boolean isStaffGroupsEnabled() {
        return getConfig().getBoolean("staff-work.groups.enabled", false);
    }

    public List<String> getStaffGroups() {
        return getConfig().getStringList("staff-work.groups.staff-groups");
    }

    public void reload() {
        plugin.reloadConfig();
        loadGuiItems();
    }
}