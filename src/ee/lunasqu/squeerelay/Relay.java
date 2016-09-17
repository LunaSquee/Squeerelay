package ee.lunasqu.squeerelay;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Created by diamond on 9/16/16.
 */
public class Relay extends BukkitRunnable {

    private boolean dead;
    private final Main plugin;
    private Configuration config;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    private String ircTarget;

    private boolean muted;

    public Relay(Main instance) {
        this.plugin = instance;
        this.config = plugin.getConfiguration();
        this.ircTarget = config.getString("relay_target");

        String addr = config.getString("relay_host");
        int port = config.getInt("relay_port");
        String password = config.getString("relay_password");

        plugin.broadcast("[Relay] Attempting relay connection " + addr + ":" + port, "manage");

        try {
            socket = new Socket(addr, port);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.write(password);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void die() {
        this.dead = true;
        this.muted = true;
    }

    private void sendToIRC(String message) {
        if(dead) return;
        if(config.getString("relay_target") != "") {
            try {
                writer.write("msg:" + config.getString("relay_target") + ":" +
                        "\u00037[\u0003\u00039Minecraft\u0003\u00037]\u0003 "+
                        message);
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendToIRC(Player p, String message, MessageType type) {
        if(type == MessageType.BROADCAST) {
            sendToIRC("\u00036[Broadcast]\u0003 "+message);
        } else if(config.getBoolean("relay_to_irc")) {
            if(type == MessageType.PLAYER_SENT) {
                sendToIRC("\u000314<\u0003\u000315"+p.getDisplayName()+"\u0003\u000314>\u0003 "+message);
            } else if(type == MessageType.PLAYER_JOIN) {
                sendToIRC("\u00038"+p.getDisplayName()+" joined the server");
            } else if(type == MessageType.PLAYER_QUIT) {
                sendToIRC("\u00038"+p.getDisplayName()+" left the server");
            } else if(type == MessageType.PLAYER_DIED) {
                sendToIRC("\u00038"+message);
            }
        }
    }

    public boolean isDead() {
        return this.dead;
    }

    public void setMuted(boolean muted) { this.muted = muted; }
    public boolean getMuted() { return this.muted; }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if(line.equals("ping")) {
                    writer.write("pong\r\n");
                    writer.flush();
                } else if(line.indexOf(">") != -1) {
                    String[] m = line.split(":", 3);
                    String type = m[0].split(">")[0];
                    String from = m[0].split(">")[1];

                    if(!muted) {
                        if (type.equals("PRIVMSG")) {
                            plugin.broadcast(ChatColor.GREEN+"[" + m[1] + "] "+ChatColor.RESET+"<" + from + "> " + m[2]);
                        } else if (type.equals("JOIN")) {
                            plugin.broadcast(ChatColor.GREEN+"[" + m[1] + "] "+ChatColor.DARK_GREEN + "--> " + ChatColor.GREEN + from + m[2] + " the channel");
                        } else if (type.equals("PART")) {
                            plugin.broadcast(ChatColor.GREEN+"[" + m[1] + "] "+ChatColor.DARK_RED+"<-- " + ChatColor.RED+ from + m[2] + " the channel");
                        } else if (type.equals("QUIT")) {
                            plugin.broadcast(ChatColor.GREEN+"[IRC] "+ChatColor.DARK_RED+"<-- " + ChatColor.RED+ from + m[2]);
                        } else if (type.equals("ACTION")) {
                            plugin.broadcast(ChatColor.GREEN+"[" + m[1] + "] "+ ChatColor.DARK_PURPLE + ChatColor.ITALIC +"* " + from + " " + m[2]);
                        } else if (type.equals("NICK")) {
                            plugin.broadcast(ChatColor.GREEN+"[IRC] "+ ChatColor.YELLOW +" * " + from + m[2]);
                        } else if (type.equals("KICK")) {
                            plugin.broadcast(ChatColor.GREEN+"[" + m[1] + "] "+ChatColor.DARK_RED+"<-- "+ChatColor.RED + from + m[2]);
                        }
                    }
                } else {
                    if(line.toLowerCase().equals("password accepted")) {
                        plugin.broadcast("[Relay] Password accepted", "manage");
                    } else if(line.toLowerCase().equals("wrong password")) {
                        plugin.broadcast("[Relay] Password denied", "manage");
                    }
                }

                if(this.dead) break;
            }
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.die();
        }
        this.dead = true;
        if(!this.muted)
            plugin.broadcast("[Relay] Relay died.", "manage");
    }
}
