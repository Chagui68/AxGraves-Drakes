package com.artillexstudios.axgraves.utils;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axgraves.AxGraves;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;

import static java.time.temporal.ChronoUnit.SECONDS;

public class UpdateNotifier implements Listener {
    private static Config config;
    private static Config lang;
    private static boolean onJoin;
    private static String prefix;
    private static String updateNotifier;

    private final String current;
    private String latest = null;
    private boolean newest = true;

    public static void init(Config config, Config lang) {
        UpdateNotifier.config = config;
        UpdateNotifier.lang = lang;
        reload();
    }

    public static void reload() {
        onJoin = config.getBoolean("update-notifier.on-join", true);
        prefix = config.getString("prefix");
        updateNotifier = lang.getString("update-notifier");
    }

    public UpdateNotifier() {
        this.current = AxGraves.getInstance().getDescription().getVersion();

        AxGraves.getInstance().getServer().getPluginManager().registerEvents(this, AxGraves.getInstance());

        long time = 30L * 60L * 20L;
        Scheduler.get().runAsyncTimer(t -> {
            this.latest = readVersion();
            this.newest = !isOutdated(current);

            if (latest == null || newest) return;
            Scheduler.get().runLaterAsync(t2 -> {
                Bukkit.getConsoleSender().sendMessage(getMessage());
            }, 50L);
            t.cancel();
        }, 0, time);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (latest == null || newest) return;
        if (!onJoin) return;
        if (!event.getPlayer().hasPermission(AxGraves.getInstance().getName().toLowerCase() + ".update-notify")) return;
        Scheduler.get().runLaterAsync(t -> {
            event.getPlayer().sendMessage(getMessage());
        }, 50L);
    }

    private String getMessage() {
        HashMap<String, String> map = new HashMap<>();
        map.put("%current%", current);
        map.put("%latest%", latest);
        return StringUtils.formatToString(String.format("%s %s", prefix, updateNotifier), map);
    }

    @Nullable
    private String readVersion() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://www.artillex-studios.com/api/v1/resource/%s/latest-version".formatted(AxGraves.getInstance().getName())))
                    .timeout(Duration.of(10, SECONDS))
                    .GET()
                    .build();

            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            return response.body().toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getLatest() {
        return latest;
    }

    public boolean isOutdated(String current) {
        if (latest == null) return false;
        int[] parts1 = parseVersion(latest);
        int[] parts2 = parseVersion(current);
        for (int i = 0; i < 3; i++) {
            if (parts1[i] > parts2[i]) {
                return true;
            } else if (parts1[i] < parts2[i]) {
                return false;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        int[] result = new int[3];
        if (version == null) return result;
        String[] parts = version.split("\\.");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            StringBuilder digits = new StringBuilder();
            for (char c : parts[i].toCharArray()) {
                if (Character.isDigit(c)) {
                    digits.append(c);
                } else {
                    break;
                }
            }
            if (digits.length() == 0) {
                result[i] = 0;
            } else {
                try {
                    result[i] = Integer.parseInt(digits.toString());
                } catch (NumberFormatException ex) {
                    result[i] = 0;
                }
            }
        }
        return result;
    }
}