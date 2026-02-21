package ua.panica.main.pncmodertools.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class GuiItemConfig {

    private final Material material;
    private final int customModelData;
    private final String name;
    private final List<String> lore;
    private final String headValue;
    private final String action;
    private final String command;

    public GuiItemConfig(ConfigurationSection section) {
        this.material = Material.valueOf(section.getString("material", "PAPER"));
        this.customModelData = section.getInt("model", 0);
        this.name = section.getString("name", "");
        this.lore = section.getStringList("lore");
        this.headValue = section.getString("head-value", null);
        this.action = section.getString("action", "NONE");
        this.command = section.getString("command", "");
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    public String getHeadValue() {
        return headValue;
    }

    public String getAction() {
        return action;
    }

    public String getCommand() {
        return command;
    }

    public boolean hasCustomModelData() {
        return customModelData > 0;
    }

    public boolean isPlayerHead() {
        return headValue != null && !headValue.isEmpty();
    }
}
