package ua.panica.main.pncmodertools.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.model.CheckSession;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;

public class ReviceGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final PncModerTools plugin;
    private final Player viewer;

    public ReviceGUI(PncModerTools plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void open() {
        Optional<CheckSession> opt = plugin.getCheckManager().getSession(viewer);
        if (opt.isEmpty()) {
            viewer.sendMessage("§cНет активной проверки для GUI.");
            return;
        }

        String title = parseColors(plugin.getReviceConfig().getGuiTitle());
        String structure = plugin.getReviceConfig().getGuiStructure();

        Gui.Builder<?, ?> builder = Gui.normal();
        builder.setStructure(structure.split("\n")); // ← Добавь .split("\n")

        List<String> keys = plugin.getReviceConfig().getGuiItemKeys();
        for (String key : keys) {
            GuiItemConfig itemConfig = plugin.getReviceConfig().getGuiItem(key);
            if (itemConfig != null) {
                Item item = createGuiItem(key, itemConfig);
                builder.addIngredient(key.charAt(0), item);
            }
        }

        Gui gui = builder.build();

        Window window = Window.single()
                .setViewer(viewer)
                .setTitle(title)
                .setGui(gui)
                .build();

        window.open();
    }

    private Item createGuiItem(String key, GuiItemConfig itemConfig) {
        Material material = itemConfig.getMaterial();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = itemConfig.getName();
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(parseColors(name));
            }

            List<String> lore = itemConfig.getLore();
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(parseColors(line));
                }
                meta.setLore(coloredLore);
            }

            item.setItemMeta(meta);
        }

        return new SimpleItem(new ItemBuilder(item)) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                handleAction(key, player);
            }
        };
    }

    private void handleAction(String key, Player player) {
        switch (key.toUpperCase(Locale.ROOT)) {
            case "B":
                plugin.getCheckManager().banSuspect(player);
                player.closeInventory();
                break;

            case "A":
                int addSec = plugin.getReviceConfig().getAddTimeSeconds();
                plugin.getCheckManager().addTime(player, addSec);
                break;

            case "P":
                plugin.getCheckManager().pauseTimer(player);
                break;

            case "T":
                plugin.getCheckManager().teleportAgain(player);
                break;

            case "S":
                plugin.getCheckManager().stopCheck(player);
                player.closeInventory();
                break;

            default:
                break;
        }
    }

    private Material parseMaterial(String materialStr) {
        if (materialStr == null) return Material.GRAY_STAINED_GLASS_PANE;
        Material m = Material.matchMaterial(materialStr.toUpperCase(Locale.ROOT));
        return m != null ? m : Material.GRAY_STAINED_GLASS_PANE;
    }

    private String parseColors(String text) {
        if (text == null) return "";
        return LEGACY.serialize(MM.deserialize(text));
    }
}