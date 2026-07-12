package com.artillexstudios.axgraves.hooks;

import com.github.drakescraft_labs.slimefun4.utils.SlimefunUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SlimefunHook {

    private static boolean enabled;
    private static final AtomicBoolean detectionFailureLogged = new AtomicBoolean();

    private SlimefunHook() {
    }

    public static void init() {
        enabled = Bukkit.getPluginManager().getPlugin("Slimefun") != null;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isSoulbound(ItemStack item) {
        if (!enabled || item == null || item.getType().isAir()) {
            return false;
        }
        try {
            return SlimefunUtils.isSoulbound(item);
        } catch (LinkageError | RuntimeException ex) {
            if (detectionFailureLogged.compareAndSet(false, true)) {
                Bukkit.getLogger().log(
                        java.util.logging.Level.SEVERE,
                        "[AxGraves] Slimefun Soulbound detection failed. Items will be treated as Soulbound to prevent duplication.",
                        ex
                );
            }
            return true;
        }
    }
}
