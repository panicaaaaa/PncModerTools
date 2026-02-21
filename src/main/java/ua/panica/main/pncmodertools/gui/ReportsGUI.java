package ua.panica.main.pncmodertools.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import ua.panica.main.pncmodertools.PncModerTools;
import ua.panica.main.pncmodertools.model.GroupedReport;
import ua.panica.main.pncmodertools.model.ReportStatus;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportsGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final PncModerTools plugin;
    private final Player viewer;
    private final SimpleDateFormat dateFormat;
    private final boolean hasPlaceholderAPI;

    public ReportsGUI(PncModerTools plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        this.hasPlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public void open() {
        plugin.getReportManager().getGroupedReports().thenAccept(groupedReports -> {
            Bukkit.getScheduler().runTask(plugin, () -> openMenu(groupedReports));
        });
    }

    private void openMenu(List<GroupedReport> groupedReports) {
        List<Item> items = new ArrayList<>();

        for (GroupedReport grouped : groupedReports) {
            items.add(new GroupedReportItem(grouped));
        }

        String[] structure = plugin.getConfigManager().getMenuStructure().split("\n");

        Gui.Builder<?, ?> builder = PagedGui.items()
                .setStructure(structure)
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(items);

        for (char key : getCustomItemKeys()) {
            GuiItemConfig itemConfig = plugin.getConfigManager().getGuiItem(String.valueOf(key));
            if (itemConfig != null) {
                builder.addIngredient(key, createCustomItem(itemConfig));
            }
        }

        Gui gui = builder.build();

        String titleText = parsePlaceholders(plugin.getConfigManager().getMenuTitle(), viewer);
        String titleParsed = LEGACY.serialize(MM.deserialize(titleText));

        Window window = Window.single()
                .setViewer(viewer)
                .setTitle(titleParsed)
                .setGui(gui)
                .build();

        window.open();
    }

    private Set<Character> getCustomItemKeys() {
        Set<Character> keys = new HashSet<>();
        String structure = plugin.getConfigManager().getMenuStructure();
        for (char c : structure.toCharArray()) {
            if (c != 'x' && c != '\n' && c != ' ') {
                keys.add(c);
            }
        }
        return keys;
    }

    private Item createCustomItem(GuiItemConfig config) {
        ItemStack item;

        if (config.isPlayerHead()) {
            item = createPlayerHead(config.getHeadValue());
        } else {
            item = new ItemStack(config.getMaterial());
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (config.hasCustomModelData()) {
                meta.setCustomModelData(config.getCustomModelData());
            }

            String nameText = parsePlaceholders(config.getName(), viewer);
            String nameParsed = LEGACY.serialize(MM.deserialize(nameText));
            meta.setDisplayName(nameParsed);

            List<String> lore = new ArrayList<>();
            for (String line : config.getLore()) {
                String parsed = parsePlaceholders(line, viewer);
                String loreLine = LEGACY.serialize(MM.deserialize(parsed));
                lore.add(loreLine);
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return new SimpleItem(new ItemBuilder(item)) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                if ("COMMAND".equalsIgnoreCase(config.getAction())) {
                    player.performCommand(config.getCommand());
                } else if ("CLOSE".equalsIgnoreCase(config.getAction())) {
                    player.closeInventory();
                }
            }
        };
    }

    private ItemStack createPlayerHead(String base64Value) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            try {
                GameProfile profile = new GameProfile(UUID.randomUUID(), "hex_report");
                profile.getProperties().put("textures", new Property("textures", base64Value));

                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);

                head.setItemMeta(meta);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create player head: " + e.getMessage());
            }
        }

        return head;
    }

    private class GroupedReportItem extends AbstractItem {

        private final GroupedReport grouped;

        public GroupedReportItem(GroupedReport grouped) {
            this.grouped = grouped;
        }

        @Override
        public ItemProvider getItemProvider() {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            Player target = Bukkit.getPlayer(grouped.getTargetId());
            if (target != null && meta != null) {
                meta.setOwningPlayer(target);
            }

            ReportStatus status = grouped.getHighestPriorityStatus();
            String statusColor = getStatusColor(status);
            String statusName = getStatusName(status);

            String nameTemplate = plugin.getConfigManager().getReportItemName();
            String nameText = nameTemplate
                    .replace("{target}", grouped.getTargetName())
                    .replace("{count}", String.valueOf(grouped.getReportCount()))
                    .replace("{status}", statusName);

            nameText = parsePlaceholders(nameText, viewer);
            String nameParsed = LEGACY.serialize(MM.deserialize(nameText));
            if (meta != null) {
                meta.setDisplayName(nameParsed);
            }

            List<String> lore = new ArrayList<>();
            List<String> loreTemplate = plugin.getConfigManager().getReportItemLore();

            for (String line : loreTemplate) {
                if (line.contains("{reporters}")) {
                    for (String reporter : grouped.getReporters()) {
                        String reporterLine = plugin.getConfigManager().getReporterFormat().replace("{reporter}", reporter);
                        reporterLine = parsePlaceholders(reporterLine, viewer);
                        lore.add(LEGACY.serialize(MM.deserialize(reporterLine)));
                    }
                } else if (line.contains("{reasons}")) {
                    int index = 1;
                    for (String reason : grouped.getReasons()) {
                        String reasonLine = plugin.getConfigManager().getReasonFormat()
                                .replace("{index}", String.valueOf(index++))
                                .replace("{reason}", reason);
                        reasonLine = parsePlaceholders(reasonLine, viewer);
                        lore.add(LEGACY.serialize(MM.deserialize(reasonLine)));
                    }
                } else {
                    String parsed = line
                            .replace("{target}", grouped.getTargetName())
                            .replace("{count}", String.valueOf(grouped.getReportCount()))
                            .replace("{status_color}", statusColor)
                            .replace("{status}", statusName);

                    parsed = parsePlaceholders(parsed, viewer);
                    lore.add(LEGACY.serialize(MM.deserialize(parsed)));
                }
            }

            if (meta != null) {
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }

            skull.setAmount(Math.min(64, grouped.getReportCount()));

            return new ItemBuilder(skull);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType == ClickType.SHIFT_LEFT) {
                Player target = Bukkit.getPlayer(grouped.getTargetId());
                if (target != null && target.isOnline()) {
                    player.teleport(target.getLocation());
                    sendMessage(player, "<green>Вы телепортировались к игроку " + target.getName());
                } else {
                    sendMessage(player, "<red>Игрок не в сети!");
                }
                player.closeInventory();

            } else if (clickType == ClickType.LEFT) {
                plugin.getReportManager().acceptAllReports(grouped.getTargetId(), player);
                sendMessage(player, "<green>Вы приняли все репорты на " + grouped.getTargetName());
                player.closeInventory();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new ReportsGUI(plugin, player).open();
                }, 1L);

            } else if (clickType == ClickType.RIGHT) {
                plugin.getReportManager().closeAllReports(grouped.getTargetId(), player);
                sendMessage(player, "<green>Вы закрыли все репорты на " + grouped.getTargetName());
                player.closeInventory();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new ReportsGUI(plugin, player).open();
                }, 1L);
            }
        }
    }

    private String getStatusColor(ReportStatus status) {
        switch (status) {
            case PENDING:
                return "<red>";
            case IN_PROGRESS:
                return "<yellow>";
            case RESOLVED:
                return "<green>";
            default:
                return "<gray>";
        }
    }

    private String getStatusName(ReportStatus status) {
        switch (status) {
            case PENDING:
                return "Нерассмотренный";
            case IN_PROGRESS:
                return "Рассматривается";
            case RESOLVED:
                return "Просмотрено";
            default:
                return "Неизвестно";
        }
    }

    private String parsePlaceholders(String text, Player player) {
        if (hasPlaceholderAPI && player != null) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    private void sendMessage(Player player, String text) {
        plugin.adventure().player(player).sendMessage(MM.deserialize(text));
    }
}