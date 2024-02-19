package org.itxtech.synapseapi;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.server.BatchPacketsEvent;
import cn.nukkit.event.server.ServerStopEvent;
import cn.nukkit.network.RakNetInterface;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;
import cn.nukkit.utils.VarInt;
import org.itxtech.synapseapi.messaging.Messenger;
import org.itxtech.synapseapi.messaging.StandardMessenger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author boybook
 */
public class SynapseAPI extends PluginBase implements Listener {

    public static boolean canReconnect = true;
    private static SynapseAPI instance;
    private final Map<String, SynapseEntry> synapseEntries = new HashMap<>();
    private Messenger messenger;
    public static boolean playerCountUpdates;
    public static boolean alwaysSpawn;
    public static Map<String, Integer> playerCountData = new ConcurrentHashMap<>();

    public static SynapseAPI getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        if (!getServer().getName().equals("Nukkit PetteriM1 Edition") && getServer().getCodename().equals("PM1E")) {
            getServer().getLogger().error("This build of SynapseAPI can only be used on Nukkit PetteriM1 Edition. Please download correct build.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;
        canReconnect = true;
        this.getServer().getPluginManager().registerEvents(this, this);
        this.messenger = new StandardMessenger();
        this.loadEntries();
    }

    public Map<String, SynapseEntry> getSynapseEntries() {
        return synapseEntries;
    }

    public void addSynapseAPI(SynapseEntry entry) {
        this.synapseEntries.put(entry.getHash(), entry);
    }

    public SynapseEntry getSynapseEntry(String hash) {
        return this.synapseEntries.get(hash);
    }

    public void shutdownAll() {
        for (SynapseEntry entry : new ArrayList<>(this.synapseEntries.values())) {
            entry.shutdown();
        }
    }

    @Override
    public void onDisable() {
        this.shutdownAll();
    }

    public DataPacket getPacket(byte[] buffer) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer);

        int header;
        try {
            header = (int) VarInt.readUnsignedVarInt(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to decode packet header", e);
        }

        // | Client ID | Sender ID | Packet ID |
        // |   2 bits  |   2 bits  |  10 bits  |
        int packetId = header & 0x3ff;

        DataPacket packet = this.getServer().getNetwork().getPacket(packetId == 0xfe ? 0xff : packetId);

        if (packet != null) {
            packet.setBuffer(buffer, buffer.length - inputStream.available());
        }

        return packet;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadEntries() {
        this.saveDefaultConfig();

        for (SourceInterface sourceInterface : this.getServer().getNetwork().getInterfaces()) {
            if (sourceInterface instanceof RakNetInterface) {
                sourceInterface.shutdown();
            }
        }

        List entries = this.getConfig().getList("entries");

        for (Object entry : entries) {
            ConfigSection section = new ConfigSection((LinkedHashMap) entry);
            String serverIp = section.getString("server-ip", "127.0.0.1");
            int port = section.getInt("server-port", 10305);
            boolean isLobbyServer = section.getBoolean("isLobbyServer");
            boolean transfer = section.getBoolean("transferOnShutdown", true);
            String password = section.getString("password");
            String serverDescription = section.getString("description");
            this.addSynapseAPI(new SynapseEntry(this, serverIp, port, isLobbyServer, transfer, password, serverDescription));
        }
    }

    public Messenger getMessenger() {
        return messenger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof SynapsePlayer) {
            SynapsePlayer p = (SynapsePlayer) sender;
            String c = cmd.getName().toLowerCase();
            if (c.equals("transfer") || c.equals("srv")) {
                if (args.length > 0) {
                    if (p.getSynapseEntry().getServerDescription().equals(args[0])) {
                        p.sendMessage("\u00A7cYou are already on this server");
                    } else {
                        if (p.transferCommand(args[0]) == 0) {
                            p.sendMessage("\u00A7cUnknown server");
                        }
                    }
                } else {
                    return false;
                }
            } else if (c.equals("hub") || c.equals("lobby")) {
                List<String> l = getConfig().getStringList("lobbies");
                if (l.size() == 0) return true;
                if (!l.contains(p.getSynapseEntry().getServerDescription()) && !p.getSynapseEntry().isLobbyServer()) {
                    p.transferByDescription(l.get(Utils.random.nextInt(l.size())));
                } else {
                    p.sendMessage("\u00A7cYou are already on a lobby server");
                }
            }
        }
        return true;
    }

    @EventHandler
    public void onBatchPackets(BatchPacketsEvent e) {
        e.setCancelled(true);

        for (Player p : e.getPlayers()) {
            SynapsePlayer player = (SynapsePlayer) p;
            for (DataPacket pk : e.getPackets()) {
                player.sendDataPacket(pk, false, false);
            }
        }
    }

    @EventHandler
    public void onServerShutdown(ServerStopEvent e) {
        canReconnect = false;
        List<String> l = SynapseAPI.getInstance().getConfig().getStringList("lobbies");
        int size = l.size();
        if (size == 0) {
            return;
        }
        for (Player p : this.getServer().getOnlinePlayers().values()) {
            if (p instanceof SynapsePlayer) {
                p.sendMessage(TextFormat.RED + "The server you were previously on went down and you have been connected to lobby");
                ((SynapsePlayer) p).transferByDescription(l.get(size == 1 ? 0 : Utils.random.nextInt(size)));
            }
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}
    }
}
