package org.c191239.PlayerLoginExtension;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main extends JavaPlugin {
    public static Main self;
    public static void main(String[] args){
        System.out.println("This is a Spigot plugin. If you want to load it, please put it in the plugins folder of your Minecraft Spigot server directory.");
    }

    @Override
    public void onEnable() {
        self = this;
        Bukkit.getPluginManager().registerEvents(new EventProcessor(), this);
        Objects.requireNonNull(Bukkit.getPluginCommand("register")).setExecutor(new RegisterCommandHandler());
        Objects.requireNonNull(Bukkit.getPluginCommand("login")).setExecutor(new LoginCommandHandler());
        Objects.requireNonNull(Bukkit.getPluginCommand("changepassword")).setExecutor(new ChangePasswordCommandHandler());
        log("§3Player Login Extension enabled successfully.");
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        saveConfig();
        log("§6Player Login Extension disabled successfully.");
    }

    public void log(String s){
        CommandSender cs = Bukkit.getConsoleSender();
        cs.sendMessage(s);
    }
}


final class EventProcessor implements Listener{
    public void cancelAndInformIfNotLoggedIn(Cancellable c){
        if (c instanceof PlayerEvent){
            if (PlayerNotLoginQuery.verifyPlayer(((PlayerEvent) c).getPlayer())){
                c.setCancelled(true);
                if (PlayerPasswordQuery.isPlayerRegistered(((PlayerEvent) c).getPlayer())){
                    ((PlayerEvent) c).getPlayer().sendMessage(ChatColor.GOLD+"Type /l <password> to login.");
                } else {
                    ((PlayerEvent) c).getPlayer().sendMessage(ChatColor.GOLD+"Type /reg <password> <confirm password> to register.");
                }
            }
        } else if (c instanceof InventoryOpenEvent) {
            if (PlayerNotLoginQuery.verifyPlayer((Player) ((InventoryOpenEvent) c).getPlayer())){
                c.setCancelled(true);
                if (PlayerPasswordQuery.isPlayerRegistered((Player) ((InventoryOpenEvent) c).getPlayer())){
                    ((InventoryOpenEvent) c).getPlayer().sendMessage(ChatColor.GOLD+"Type /l <password> to login.");
                } else {
                    ((InventoryOpenEvent) c).getPlayer().sendMessage(ChatColor.GOLD+"Type /reg <password> <confirm password> to register.");
                }
            }
        } else if (c instanceof EntityDamageEvent){
            if (((EntityDamageEvent) c).getEntity() instanceof Player){
                if (PlayerNotLoginQuery.verifyPlayer((Player) ((EntityDamageEvent) c).getEntity())){
                    c.setCancelled(true);
                    if (PlayerPasswordQuery.isPlayerRegistered((Player) ((EntityDamageEvent) c).getEntity())){
                        ((EntityDamageEvent) c).getEntity().sendMessage(ChatColor.GOLD+"Type /l <password> to login.");
                    } else {
                        ((EntityDamageEvent) c).getEntity().sendMessage(ChatColor.GOLD+"Type /reg <password> <confirm password> to register.");
                    }
                }
            } else {
                c.setCancelled(false);
            }
        } else {
            c.setCancelled(false);
        }
    }

    @EventHandler
    public void preventMove(PlayerMoveEvent e){
        cancelAndInformIfNotLoggedIn(e);
    }

    @EventHandler
    public void preventInteract(PlayerInteractEvent e){
        cancelAndInformIfNotLoggedIn(e);
    }

    @EventHandler
    public void preventInteractAtEntity(PlayerInteractAtEntityEvent e){
        cancelAndInformIfNotLoggedIn(e);
    }

    @EventHandler
    public void preventPortal(PlayerPortalEvent e){
        cancelAndInformIfNotLoggedIn(e);
    }

    @EventHandler
    public void preventTeleport(PlayerTeleportEvent e){
        cancelAndInformIfNotLoggedIn(e);
    }

    @EventHandler
    public void preventOpenInventory(InventoryOpenEvent e){
        cancelAndInformIfNotLoggedIn(e);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e){
        PlayerNotLoginQuery.addPlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        PlayerNotLoginQuery.removePlayer(e.getPlayer());
    }

    @EventHandler
    public void preventHurt(EntityDamageEvent e){
        cancelAndInformIfNotLoggedIn(e);
    }
}

final class PlayerPasswordQuery {
    public static FileConfiguration config = Main.self.getConfig();
    public static boolean isPlayerRegistered(Player p){
        return config.contains(p.getUniqueId().toString());
    }
    public static boolean verifyPassword(Player p, String pwd){
        String pwdHash = String.valueOf(pwd.hashCode());
        return pwdHash.equals(config.getString(p.getUniqueId().toString()));
    }
    public static void registerPlayer(Player p, String pwd){
        config.set(p.getUniqueId().toString(), String.valueOf(pwd.hashCode()));
        Main.self.saveConfig();
    }
}

final class PlayerNotLoginQuery {
    private static final List<String> playerNotLoginList = new ArrayList<>();
    public static void addPlayer(Player p){
        playerNotLoginList.add(p.getUniqueId().toString());
    }
    public static void removePlayer(Player p){
        playerNotLoginList.remove(p.getUniqueId().toString());
    }
    public static boolean verifyPlayer(Player p){
        return playerNotLoginList.contains(p.getUniqueId().toString());
    }
}

final class LoginCommandHandler implements CommandExecutor{
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)){
            return false;
        }
        if (strings.length != 1){
            return false;
        }
        if (!PlayerPasswordQuery.isPlayerRegistered((Player) commandSender)) {
            commandSender.sendMessage(ChatColor.RED+"You have not registered yet. Type /reg <password> <confirm password> to register.");
            return true;
        }
        if (PlayerNotLoginQuery.verifyPlayer((Player) commandSender)){
            if (PlayerPasswordQuery.verifyPassword(((Player) commandSender), strings[0])){
                PlayerNotLoginQuery.removePlayer((Player) commandSender);
                commandSender.sendMessage(ChatColor.GREEN+"Welcome back, "+((Player) commandSender).getDisplayName()+"!");
            } else {
                commandSender.sendMessage(ChatColor.RED+"Your password is wrong. Please try again.");
            }
        } else {
            commandSender.sendMessage(ChatColor.RED+"You have already logged in!");
        }
        return true;
    }
}

final class RegisterCommandHandler implements CommandExecutor{
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)){
            return false;
        }
        if (strings.length != 2){
            return false;
        }
        if (PlayerPasswordQuery.isPlayerRegistered((Player) commandSender)){
            commandSender.sendMessage(ChatColor.RED+"You have already registered!");
            return true;
        } else if (PlayerNotLoginQuery.verifyPlayer((Player) commandSender)) {
            if (strings[0].equals(strings[1])){
                PlayerPasswordQuery.registerPlayer((Player) commandSender, strings[0]);
                commandSender.sendMessage(ChatColor.GREEN+"Congratulations! You have registered successfully.");
            } else {
                commandSender.sendMessage(ChatColor.RED+"Please confirm your password.");
            }
            return true;
        } else {
            commandSender.sendMessage(ChatColor.AQUA+"How do you login but not register at the same time?");
            return true;
        }
    }
}

final class ChangePasswordCommandHandler implements CommandExecutor{
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)){
            return false;
        }
        if (strings.length != 3) {
            return false;
        }
        if (PlayerPasswordQuery.isPlayerRegistered((Player) commandSender)){
            if (PlayerPasswordQuery.verifyPassword((Player) commandSender, strings[0])){
                if (strings[1].equals(strings[2])){
                    PlayerPasswordQuery.registerPlayer((Player) commandSender, strings[1]);
                    commandSender.sendMessage(ChatColor.GREEN+"Congratulations! You have changed your password successfully.");
                } else {
                    commandSender.sendMessage(ChatColor.RED+"Please confirm your new password.");
                }
            } else {
                commandSender.sendMessage(ChatColor.RED+"Your old password is wrong. Please try again.");
            }
        } else {
            commandSender.sendMessage(ChatColor.RED+"You have not registered yet. Type /reg <password> <confirm password> to register.");
        }
        return true;
    }
}