package com.artillexstudios.axgraves.migration;

import com.artillexstudios.axgraves.AxGraves;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class MoreGravesImporter {
    private static final String MARKER_NAME = "moregraves-imported.marker";

    private MoreGravesImporter() {
    }

    /** Imports legacy graves once, preserving the source file as rollback evidence. */
    public static void importIfNeeded() {
        File dataFolder = AxGraves.getInstance().getDataFolder();
        File marker = new File(dataFolder, MARKER_NAME);
        File axData = new File(dataFolder, "data.json");
        File legacyData = new File(dataFolder.getParentFile(), "MoreGraves/graves.yml");
        if (marker.exists() || axData.exists() || !legacyData.isFile()) return;

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(legacyData);
            ConfigurationSection graves = yaml.getConfigurationSection("graves");
            if (graves == null) return;

            int imported = 0;
            for (String id : graves.getKeys(false)) {
                ConfigurationSection section = graves.getConfigurationSection(id);
                if (section == null) continue;

                World world = Bukkit.getWorld(section.getString("world", ""));
                String ownerId = section.getString("owner-id", "");
                if (world == null || ownerId.isBlank()) continue;

                Location location = new Location(
                        world,
                        section.getDouble("base.x"),
                        section.getDouble("base.y"),
                        section.getDouble("base.z")
                );
                OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerId));
                List<ItemStack> items = new ArrayList<>();
                addItems(items, section.getList("storage"));
                addItems(items, section.getList("armor"));
                ItemStack offhand = section.getItemStack("offhand");
                if (offhand != null && !offhand.getType().isAir()) items.add(offhand);

                long created = section.getLong("created-at", System.currentTimeMillis() / 1000L) * 1000L;
                SpawnedGraves.addGrave(new Grave(location, owner, items, 0, created));
                imported++;
            }

            if (!SpawnedGraves.saveToFile()) {
                throw new IllegalStateException("Could not persist imported graves");
            }
            Files.writeString(marker.toPath(), "Imported " + imported + " MoreGraves graves. Source retained at " + legacyData + System.lineSeparator());
            AxGraves.getInstance().getLogger().info("Imported " + imported + " legacy MoreGraves graves.");
        } catch (Exception ex) {
            AxGraves.getInstance().getLogger().log(Level.SEVERE, "Legacy MoreGraves import failed; source data was not modified.", ex);
        }
    }

    private static void addItems(List<ItemStack> destination, List<?> source) {
        if (source == null) return;
        for (Object value : source) {
            if (value instanceof ItemStack item && !item.getType().isAir()) {
                destination.add(item);
            }
        }
    }
}
