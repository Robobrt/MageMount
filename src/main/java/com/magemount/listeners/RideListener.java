package com.magemount.listeners;

import com.magemount.MageMount;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
// =========================================================================================
// ИСПРАВЛЕНО: Правильный импорт для Spigot API
// =========================================================================================
import org.spigotmc.event.entity.EntityDismountEvent;

public class RideListener implements Listener {

    private final MageMount plugin;

    public RideListener(MageMount plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.startActionBarTaskForPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.stopActionBarTaskForPlayer(event.getPlayer());
        plugin.cancelDismountTask(event.getPlayer().getUniqueId());
        plugin.rideCooldowns.remove(event.getPlayer().getUniqueId());
        plugin.dismountCooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if (plugin.rideLimiterEnabled) {
            if (plugin.getRemainingUses(player.getUniqueId()) <= 0) {
                plugin.sendNoUsesLeftMessage(player);
                return;
            }
        }
        
        long rideCooldown = plugin.rideCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() < rideCooldown) {
            plugin.sendCooldownMessage(player, rideCooldown);
            return;
        }

        if (player.isInsideVehicle() || !(event.getRightClicked() instanceof LivingEntity) || !event.getRightClicked().getPassengers().isEmpty()) {
            return;
        }
        
        LivingEntity target = (LivingEntity) event.getRightClicked();
        if (plugin.blacklistedEntities.contains(target.getType())) {
            return;
        }
        
        boolean success = false;
        if (target instanceof Player && plugin.ridePlayersEnabled && player.hasPermission("magemount.player")) {
            target.addPassenger(player);
            success = true;
        } else if (!(target instanceof Player) && plugin.rideMobsEnabled && player.hasPermission("magemount.pets")) {
            target.addPassenger(player);
            success = true;
        }

        if (success) {
            plugin.useRide(player.getUniqueId());
            long now = System.currentTimeMillis();
            plugin.rideCooldowns.put(player.getUniqueId(), now + (plugin.rideCooldownSeconds * 1000));
            plugin.dismountCooldowns.put(player.getUniqueId(), now + (plugin.dismountCooldownSeconds * 1000));
            plugin.startDismountTask(player);
        }
    }
    
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking() || !player.isInsideVehicle()) return;
        
        long dismountCooldown = plugin.dismountCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() < dismountCooldown) {
            plugin.sendCooldownMessage(player, dismountCooldown);
            return;
        }
        
        player.leaveVehicle();
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player) {
            plugin.cancelDismountTask(event.getEntity().getUniqueId());
        }
    }
}