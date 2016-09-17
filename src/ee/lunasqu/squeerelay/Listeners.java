package ee.lunasqu.squeerelay;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;

/**
 * Created by diamond on 9/17/16.
 */
public class Listeners implements Listener, CommandExecutor {
    private final Main plugin;

    public Listeners(Main m) {
        this.plugin = m;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void dance(Player p, String message, MessageType type) {
        Relay relay = plugin.getRelay();
        if(relay == null) return;
        if(relay.isDead()) return;

        relay.sendToIRC(p, message, type);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        dance(p, "joined the game", MessageType.PLAYER_JOIN);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        dance(p, "quit the game", MessageType.PLAYER_QUIT);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChatLowest(AsyncPlayerChatEvent event) {

        if (event.isCancelled()) return;

        Player p = event.getPlayer();
        String message = event.getMessage();

        dance(p, message, MessageType.PLAYER_SENT);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.hasPermission("squeerelay.manage")) {
            commandSender.sendMessage(ChatColor.RED+"You do not have permission to manage the relay!");
            return false;
        }

        if(strings[0].equals("start")) {
            commandSender.sendMessage("Starting relay..");

            plugin.newRelay();

            return true;
        } else if(strings[0].equals("broadcast")) {
            if(strings[1] == null)
                return false;

            String[] va = Arrays.copyOfRange(strings, 1, strings.length);
            String vap = StringUtils.join(va, " ");

            Relay relay = plugin.getRelay();

            if(relay == null) {
                commandSender.sendMessage(ChatColor.RED+"There is no relay to broadcast to!");
                return false;
            } else {
                relay.sendToIRC(null, vap, MessageType.BROADCAST);
            }
        } else if(strings[0].equals("stop")) {
            if(plugin.getRelay() != null) {
                plugin.getRelay().die();
                return true;
            } else {
                commandSender.sendMessage(ChatColor.RED+"There is no relay to stop!");
            }
            return true;
        } else if(strings[0].equals("status")) {
            StringBuilder str = new StringBuilder();
            if(plugin.getRelay() == null) {
                str.append(ChatColor.RED+"Relay offline!");
            } else {
                Relay relay = plugin.getRelay();
                if(relay.isDead()) {
                    str.append(ChatColor.RED+"Relay offline!");
                } else {
                    str.append(ChatColor.GREEN+"Relay running; ");
                    if(relay.getMuted()) {
                        str.append(ChatColor.RED+"Muted;");
                    } else {
                        str.append(ChatColor.GREEN+"Not muted;");
                    }
                }
            }
            commandSender.sendMessage(str.toString());
            return true;
        } else if(strings[0].equals("mute")) {
            Relay relay = plugin.getRelay();
            if (relay != null) {
                if (relay.getMuted()) {
                    commandSender.sendMessage("Un-muting the relay");
                } else {
                    commandSender.sendMessage("Muting the relay");
                }

                relay.setMuted(!relay.getMuted());
                return true;
            } else {
                commandSender.sendMessage(ChatColor.RED + "There is no relay to mute!");
                return false;
            }
        } else {
            return false;
        }
        return false;
    }
}
