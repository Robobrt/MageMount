package com.magemount;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.magemount.listeners.RideListener;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MageMount extends JavaPlugin implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder().hexColors().character('&').build();
    private static final Pattern COLOR_ONLY_PATTERN = Pattern.compile("(&[0-9a-fA-F])|(&#[0-9a-fA-F]{6})");

    // Config values
    public Component rideHint, dismountHint, autoDismountMessage;
    public String onCooldownMessageString, noUsesLeftMessageString, reloadSuccessString, noPermissionString,
                   cmdUsage, cmdUsesUsage, cmdPlayerNotFound, cmdUsesCheck, cmdUsesReset,
                   cmdUsesNoAmount, cmdUsesSet, cmdUsesAdd, cmdUsesNotANumber;
    public boolean tempRideEnabled, rideLimiterEnabled, ridePlayersEnabled, rideMobsEnabled;
    public int tempRideDuration, maxRideUses, rideResetIntervalMinutes, checkDistance, updateIntervalTicks;
    public long rideCooldownSeconds, dismountCooldownSeconds;
    public Set<EntityType> blacklistedEntities = new HashSet<>();
    
    // Data storage
    public final Map<UUID, Long> rideCooldowns = new HashMap<>();
    public final Map<UUID, Long> dismountCooldowns = new HashMap<>();
    private final Map<UUID, ScheduledTask> playerTasks = new HashMap<>();
    public final Map<UUID, ScheduledTask> dismountTimers = new HashMap<>();
    public final Map<UUID, Integer> playerRideUses = new HashMap<>();
    public final Map<UUID, Long> rideUsesResetTime = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        if (!getConfig().getBoolean("settings.enabled", true)) {
            getLogger().info("MageMount отключен в config.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(new RideListener(this), this);
        getCommand("magemount").setExecutor(this);
        Bukkit.getOnlinePlayers().forEach(this::startActionBarTaskForPlayer);
        getLogger().info("MageMount успешно включен!");
    }

    @Override
    public void onDisable() {
        playerTasks.values().forEach(ScheduledTask::cancel);
        playerTasks.clear();
        dismountTimers.values().forEach(ScheduledTask::cancel);
        dismountTimers.clear();
        getLogger().info("MageMount выключен.");
    }
    
    public void loadConfigValues() {
        rideHint = formatMessage(getConfig().getString("messages.ride-hint"));
        dismountHint = formatMessage(getConfig().getString("messages.dismount-hint"));
        onCooldownMessageString = getConfig().getString("messages.on-cooldown");
        autoDismountMessage = formatMessage(getConfig().getString("messages.auto-dismount"));
        noUsesLeftMessageString = getConfig().getString("messages.no-uses-left");

        reloadSuccessString = getConfig().getString("messages.admin-commands.reload-success");
        noPermissionString = getConfig().getString("messages.admin-commands.no-permission");
        cmdUsage = getConfig().getString("messages.admin-commands.usage");
        cmdUsesUsage = getConfig().getString("messages.admin-commands.uses-usage");
        cmdPlayerNotFound = getConfig().getString("messages.admin-commands.player-not-found");
        cmdUsesCheck = getConfig().getString("messages.admin-commands.uses-check");
        cmdUsesReset = getConfig().getString("messages.admin-commands.uses-reset");
        cmdUsesNoAmount = getConfig().getString("messages.admin-commands.uses-no-amount");
        cmdUsesSet = getConfig().getString("messages.admin-commands.uses-set");
        cmdUsesAdd = getConfig().getString("messages.admin-commands.uses-add");
        cmdUsesNotANumber = getConfig().getString("messages.admin-commands.uses-not-a-number");

        tempRideEnabled = getConfig().getBoolean("temporary-ride.enabled");
        tempRideDuration = getConfig().getInt("temporary-ride.duration-seconds");
        rideLimiterEnabled = getConfig().getBoolean("ride-limiter.enabled");
        maxRideUses = getConfig().getInt("ride-limiter.max-uses");
        rideResetIntervalMinutes = getConfig().getInt("ride-limiter.reset-interval-minutes");
        rideCooldownSeconds = getConfig().getLong("cooldowns.ride-seconds");
        dismountCooldownSeconds = getConfig().getLong("cooldowns.dismount-seconds");
        checkDistance = getConfig().getInt("settings.check-distance");
        updateIntervalTicks = getConfig().getInt("settings.update-interval-ticks");
        ridePlayersEnabled = getConfig().getBoolean("features.ride-players");
        rideMobsEnabled = getConfig().getBoolean("features.ride-mobs");

        blacklistedEntities.clear();
        Set<String> blacklistNames = new HashSet<>(getConfig().getStringList("blacklist"));
        blacklistedEntities = blacklistNames.stream()
                .map(name -> { try { return EntityType.valueOf(name.toUpperCase()); } catch (IllegalArgumentException e) { getLogger().warning("Неверное имя сущности в черном списке: " + name); return null; }})
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(formatMessage(cmdUsage));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("magemount.admin.reload")) {
                sender.sendMessage(formatMessage(noPermissionString));
                return true;
            }
            reloadConfig();
            loadConfigValues();
            sender.sendMessage(formatMessage(reloadSuccessString));
            Bukkit.getOnlinePlayers().forEach(this::startActionBarTaskForPlayer);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("uses")) {
            if (!sender.hasPermission("magemount.admin.uses")) {
                sender.sendMessage(formatMessage(noPermissionString));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(formatMessage(cmdUsesUsage));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(formatMessage(cmdPlayerNotFound.replace("%player%", args[1])));
                return true;
            }

            if (args.length == 2 || args[2].equalsIgnoreCase("check")) {
                int currentUses = getRemainingUses(target.getUniqueId());
                String msg = cmdUsesCheck.replace("%player%", target.getName())
                                         .replace("%uses%", String.valueOf(currentUses))
                                         .replace("%max_uses%", String.valueOf(maxRideUses));
                sender.sendMessage(formatMessage(msg));
                return true;
            }

            if(args[2].equalsIgnoreCase("reset")) {
                playerRideUses.remove(target.getUniqueId());
                rideUsesResetTime.remove(target.getUniqueId());
                sender.sendMessage(formatMessage(cmdUsesReset.replace("%player%", target.getName())));
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(formatMessage(cmdUsesNoAmount));
                return true;
            }

            try {
                int amount = Integer.parseInt(args[3]);
                int current = getRemainingUses(target.getUniqueId());
                
                if (args[2].equalsIgnoreCase("set")) {
                    int finalAmount = Math.min(amount, maxRideUses);
                    playerRideUses.put(target.getUniqueId(), finalAmount);
                    String msg = cmdUsesSet.replace("%player%", target.getName())
                                           .replace("%amount%", String.valueOf(finalAmount));
                    sender.sendMessage(formatMessage(msg));
                } else if (args[2].equalsIgnoreCase("add")) {
                    int newAmount = Math.min(current + amount, maxRideUses);
                    playerRideUses.put(target.getUniqueId(), newAmount);
                    String msg = cmdUsesAdd.replace("%player%", target.getName())
                                           .replace("%amount%", String.valueOf(amount))
                                           .replace("%total%", String.valueOf(newAmount));
                    sender.sendMessage(formatMessage(msg));
                } else {
                    sender.sendMessage(formatMessage(cmdUsesUsage));
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(formatMessage(cmdUsesNotANumber));
            }
            return true;
        }
        
        return true;
    }
    
    public void startDismountTask(Player player) {
        if (!tempRideEnabled || tempRideDuration <= 0) return;
        cancelDismountTask(player.getUniqueId());
        ScheduledTask task = player.getScheduler().runDelayed(this, (st) -> {
            if (player.isOnline() && player.isInsideVehicle()) {
                player.leaveVehicle();
                if (autoDismountMessage != null) player.sendMessage(autoDismountMessage);
            }
            dismountTimers.remove(player.getUniqueId());
        }, null, tempRideDuration * 20L);
        dismountTimers.put(player.getUniqueId(), task);
    }
    
    public void cancelDismountTask(UUID uuid) {
        ScheduledTask task = dismountTimers.remove(uuid);
        if (task != null) task.cancel();
    }

    public int getRemainingUses(UUID uuid) {
        if (!rideLimiterEnabled) return maxRideUses;
        long resetTime = rideUsesResetTime.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() > resetTime) {
            playerRideUses.put(uuid, maxRideUses);
            rideUsesResetTime.put(uuid, System.currentTimeMillis() + (rideResetIntervalMinutes * 60000L));
        }
        return playerRideUses.getOrDefault(uuid, maxRideUses);
    }

    public void useRide(UUID uuid) {
        if (!rideLimiterEnabled) return;
        int current = getRemainingUses(uuid);
        playerRideUses.put(uuid, Math.max(0, current - 1));
    }
    
    public void startActionBarTaskForPlayer(Player player) {
        stopActionBarTaskForPlayer(player);
        ScheduledTask task = player.getScheduler().runAtFixedRate(this, (st) -> {
            if (!player.isOnline() || !getConfig().getBoolean("settings.enabled")) {
                st.cancel();
                return;
            }
            if (player.isInsideVehicle()) {
                if (dismountHint != null) player.sendActionBar(dismountHint);
                return;
            }
            Entity target = getTargetEntity(player, checkDistance);
            if (target instanceof Player && ridePlayersEnabled && player.hasPermission("magemount.player")) {
                if (rideHint != null) player.sendActionBar(rideHint);
            } else if (target instanceof LivingEntity && rideMobsEnabled && player.hasPermission("magemount.pets")) {
                if (rideHint != null) player.sendActionBar(rideHint);
            }
        }, null, 1L, this.updateIntervalTicks);
        playerTasks.put(player.getUniqueId(), task);
    }
    public void stopActionBarTaskForPlayer(Player player) {
        ScheduledTask existingTask = playerTasks.remove(player.getUniqueId());
        if (existingTask != null) existingTask.cancel();
    }
    
    public Component formatMessage(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        Matcher matcher = COLOR_ONLY_PATTERN.matcher(text);
        String cleanText = text;
        if (matcher.find()) {
            String restOfString = text.substring(matcher.end());
            cleanText = text.substring(0, matcher.end()) + COLOR_ONLY_PATTERN.matcher(restOfString).replaceAll("");
        }
        return LEGACY_SERIALIZER.deserialize(cleanText);
    }
    private Entity getTargetEntity(Player player, int range) {
        var result = player.rayTraceEntities(range, false);
        return result != null ? result.getHitEntity() : null;
    }
    public void sendCooldownMessage(Player player, long expiryTime) {
        long timeLeft = (expiryTime - System.currentTimeMillis()) / 1000 + 1;
        String message = onCooldownMessageString.replace("%time%", String.valueOf(timeLeft));
        player.sendActionBar(LEGACY_SERIALIZER.deserialize(message));
    }
    public void sendNoUsesLeftMessage(Player player) {
        int uses = getRemainingUses(player.getUniqueId());
        String message = noUsesLeftMessageString.replace("%uses%", String.valueOf(uses))
                                                .replace("%max_uses%", String.valueOf(maxRideUses));
        player.sendActionBar(LEGACY_SERIALIZER.deserialize(message));
    }
}