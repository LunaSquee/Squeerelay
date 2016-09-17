package ee.lunasqu.squeerelay;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Created by diamond on 9/16/16.
 */
public class Main extends JavaPlugin {

    private Relay relay;
    private FileConfiguration config;
    private Listeners listeners;

    public void broadcast(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }
    }

    public void broadcast(String message, String permission) {
        this.getLogger().log(Level.INFO, message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if(p.hasPermission("squeerelay."+permission) || p.isOp()) {
                p.sendMessage(message);
            }
        }
    }

    public void newRelay() {
        if(relay != null) relay.die();
        relay = null;

        if (config.getBoolean("enabled") == true) {
            relay = new Relay(this);
            relay.runTaskAsynchronously(this);
        }
    }

    @Override
    public void onEnable() {
        config = this.getConfig();

        config.addDefault("enabled", true);
        config.addDefault("relay_to_irc", false);
        config.addDefault("relay_host", "192.168.8.130");
        config.addDefault("relay_port", 5666);
        config.addDefault("relay_password", "<3");
        config.addDefault("relay_target", "#diamond");

        config.options().copyDefaults(true);
        saveConfig();

        newRelay();

        listeners = new Listeners(this);
        this.getCommand("relay").setExecutor(listeners);
    }

    @Override
    public void onDisable() {
        relay.die();
    }

    public Relay getRelay() {
        return this.relay;
    }

    public FileConfiguration getConfiguration() {
        return this.getConfig();
    }
}
