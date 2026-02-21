package ua.panica.main.pncmodertools.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.gui.GuiItemConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviceConfigManager {

    private final PncModerTools plugin;
    private File configFile;
    private FileConfiguration config;
    private final Map<String, GuiItemConfig> guiItems;

    public ReviceConfigManager(PncModerTools plugin) {
        this.plugin = plugin;
        this.guiItems = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "revice.yml");

        if (!configFile.exists()) {
            plugin.saveResource("revice.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadGuiItems();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        guiItems.clear();
        loadGuiItems();
    }

    private void loadGuiItems() {
        ConfigurationSection itemsSection = config.getConfigurationSection("gui.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    guiItems.put(key, new GuiItemConfig(itemSection));
                }
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public Location getCheckpoint() {
        ConfigurationSection sec = config.getConfigurationSection("checkpoint");
        if (sec == null) return null;

        World w = Bukkit.getWorld(sec.getString("world", "world"));
        if (w == null) return null;

        return new Location(w,
                sec.getDouble("x"),
                sec.getDouble("y"),
                sec.getDouble("z"),
                (float) sec.getDouble("yaw"),
                (float) sec.getDouble("pitch"));
    }

    public void saveCheckpoint(Location loc) {
        config.set("checkpoint.world", loc.getWorld().getName());
        config.set("checkpoint.x", loc.getX());
        config.set("checkpoint.y", loc.getY());
        config.set("checkpoint.z", loc.getZ());
        config.set("checkpoint.yaw", loc.getYaw());
        config.set("checkpoint.pitch", loc.getPitch());
        saveConfig();
    }

    public Location getSpawn() {
        ConfigurationSection sec = config.getConfigurationSection("spawn");
        if (sec == null) return null;

        World w = Bukkit.getWorld(sec.getString("world", "world"));
        if (w == null) return null;

        return new Location(w,
                sec.getDouble("x"),
                sec.getDouble("y"),
                sec.getDouble("z"),
                (float) sec.getDouble("yaw"),
                (float) sec.getDouble("pitch"));
    }

    public void saveSpawn(Location loc) {
        config.set("spawn.world", loc.getWorld().getName());
        config.set("spawn.x", loc.getX());
        config.set("spawn.y", loc.getY());
        config.set("spawn.z", loc.getZ());
        config.set("spawn.yaw", loc.getYaw());
        config.set("spawn.pitch", loc.getPitch());
        saveConfig();
    }

    public long getDefaultSeconds() {
        return config.getLong("timer.default-seconds", 600);
    }

    public int getAddTimeSeconds() {
        return config.getInt("timer.add-time", 300);
    }

    public String getActionBarFormat() {
        return config.getString("timer.actionbar-format", "<gray>Проверка: <#FFD166>%mm:ss%</#FFD166></gray>");
    }

    public String getPausedMessage() {
        return config.getString("timer.paused-message", "<yellow>Проверка на паузе.</yellow>");
    }

    public String getChatPrefix() {
        return config.getString("chat.prefix", "<gray>[<light_purple>REVICE</light_purple>]</gray>");
    }

    public String getSuspectRole() {
        return config.getString("chat.role.suspect", "<red>ПОДОЗРЕВАЕМЫЙ</red>");
    }

    public String getModerRole() {
        return config.getString("chat.role.moder", "<green>МОДЕР</green>");
    }

    public String getChatFormat() {
        return config.getString("chat.format", "%prefix% %role% <white>%name%</white>: <gray>%message%</gray>");
    }

    public boolean isPunishOnQuitEnabled() {
        return config.getBoolean("punish.on-quit.enabled", true);
    }

    public int getPunishGraceSeconds() {
        return config.getInt("punish.on-quit.grace-seconds", 60);
    }

    public String getPunishCommand() {
        return config.getString("punish.on-quit.command", "ban %player% 7d Покинул проверку");
    }

    public String getManualBanCommand() {
        return config.getString("punish.manual-ban.command", "ban %player% 7d Читы (revice)");
    }

    public List<String> getStartMessages() {
        return config.getStringList("messages.start.chat");
    }

    public List<String> getStopMessages() {
        return config.getStringList("messages.stop.chat");
    }

    public String getTitleText(String path) {
        return config.getString("messages." + path + ".title.title", "");
    }

    public String getSubtitleText(String path) {
        return config.getString("messages." + path + ".title.subtitle", "");
    }

    public int getTitleFadeIn(String path) {
        return config.getInt("messages." + path + ".title.fadein", 10);
    }

    public int getTitleStay(String path) {
        return config.getInt("messages." + path + ".title.stay", 40);
    }

    public int getTitleFadeOut(String path) {
        return config.getInt("messages." + path + ".title.fadeout", 10);
    }

    public String getSound(String path) {
        return config.getString("messages." + path + ".sound", "");
    }

    public String getFrozenMessage() {
        return config.getString("messages.frozen.chat", "<gray>Вы заморожены.</gray>");
    }

    public String getGuiTitle() {
        return config.getString("gui.title", "<gradient:#5B247A:#8E44AD>Проверка • Панель</gradient>");
    }

    public int getGuiSize() {
        return config.getInt("gui.size", 27);
    }

    public String getGuiStructure() {
        List<String> structure = config.getStringList("gui.structure");
        return String.join("\n", structure);
    }

    public GuiItemConfig getGuiItem(String key) {
        return guiItems.get(key);
    }

    public List<String> getGuiItemKeys() {
        ConfigurationSection sec = config.getConfigurationSection("gui.items");
        return sec != null ? new ArrayList<>(sec.getKeys(false)) : new ArrayList<>();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save revice.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
}