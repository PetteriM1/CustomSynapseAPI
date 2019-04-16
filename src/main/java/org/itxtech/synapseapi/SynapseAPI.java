package org.itxtech.synapseapi;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.BatchPacketsEvent;
import cn.nukkit.network.RakNetInterface;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.ConfigSection;
import org.itxtech.synapseapi.messaging.Messenger;
import org.itxtech.synapseapi.messaging.StandardMessenger;
import org.itxtech.synapseapi.utils.DataPacketEidReplacer;

import java.util.*;

/**
 * @author boybook
 */
public class SynapseAPI extends PluginBase implements Listener {

    public static boolean enable = true;
    private static SynapseAPI instance;
    private boolean autoConnect = true;
    private Map<String, SynapseEntry> synapseEntries = new HashMap<>();
    private Messenger messenger;

    public static SynapseAPI getInstance() {
        return instance;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.getServer().getScheduler().scheduleRepeatingTask(new cn.nukkit.scheduler.Task() {
            @Override
            public void onRun(int i) {
                for (Player player : Server.getInstance().getOnlinePlayers().values()) {
                    player.setHealth(player.getHealth());
                    player.setMaxHealth(player.getMaxHealth());
                    player.setExperience(player.getExperience(), player.getExperienceLevel());
                    player.getFoodData().setLevel(player.getFoodData().getLevel());
                }
            }
        }, 1, true);
        this.getServer().getPluginManager().registerEvents(this, this);
        this.messenger = new StandardMessenger();
        loadEntries();
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
        byte pid = buffer[0] == (byte) 0xfe ? (byte) 0xff : buffer[0];

        byte start = 1;
        DataPacket data;
        data = this.getServer().getNetwork().getPacket(pid);

        if (data == null) {
            return null;
        }
        data.setBuffer(buffer, start);
        return data;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadEntries() {
        this.saveDefaultConfig();
        enable = this.getConfig().getBoolean("enable", true);

        if (!enable) {
            this.getLogger().warning("The SynapseAPI is not be enabled!");
        } else {
            if (this.getConfig().getBoolean("disable-rak")) {
                for (SourceInterface sourceInterface : this.getServer().getNetwork().getInterfaces()) {
                    if (sourceInterface instanceof RakNetInterface) {
                        sourceInterface.shutdown();
                    }
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
                this.autoConnect = section.getBoolean("autoConnect", true);
                if (this.autoConnect) {
                    this.addSynapseAPI(new SynapseEntry(this, serverIp, port, isLobbyServer, transfer, password, serverDescription));
                }
            }
        }
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof SynapsePlayer) {
            SynapsePlayer p = (SynapsePlayer) sender;
            if (cmd.getName().equalsIgnoreCase("transfer")) {
                if (args.length > 0) {
                    p.transferByDescription(args[0]);
                } else {
                    p.sendMessage("Usage: /transfer <description>");
                }
            } else if (cmd.getName().equalsIgnoreCase("hub")) {
                List<String> l = getConfig().getStringList("lobbies");
                if (l.size() == 0) return true;
                if (!l.contains(p.getSynapseEntry().getServerDescription()) && !p.getSynapseEntry().isLobbyServer()) {
                    p.transferByDescription(l.get(new Random().nextInt(l.size())));
                } else {
                    p.sendMessage("\u00A7cYou are already on a lobby server");
                }
            }
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        String message = e.getQuitMessage().toString();
        if (message.equals("timeout") || message.equals("generic reason") || message.equals("client disconnect") || message.equals("unknown")) {
            e.setQuitMessage("");
        }
    }

    @EventHandler
    public void onBatchPackets(BatchPacketsEvent e) {
        e.setCancelled();
        Player[] players = e.getPlayers();

        DataPacket[] packets = e.getPackets();
        HashMap<SynapseEntry, Map<Player, DataPacket[]>> map = new HashMap<>();

        for (Player p : players) {
            if (p instanceof  SynapsePlayer) {
                SynapsePlayer player = (SynapsePlayer) p;

                SynapseEntry entry = player.getSynapseEntry();
                Map<Player, DataPacket[]> playerPackets = map.get(entry);
                if (playerPackets == null) {
                    playerPackets = new HashMap<>();
                }

                DataPacket[] replaced = Arrays.stream(packets)
                        .map(packet -> DataPacketEidReplacer.replace(packet, p.getId(), SynapsePlayer.REPLACE_ID))
                        .toArray(DataPacket[]::new);

                playerPackets.put(player, replaced);

                map.put(entry, playerPackets);
            } else {
                e.setCancelled(false);
            }
        }

        for (Map.Entry<SynapseEntry, Map<Player, DataPacket[]>> entry : map.entrySet()) {
            for (Map.Entry<Player, DataPacket[]> playerEntry : entry.getValue().entrySet()) {
                for (DataPacket pk : playerEntry.getValue()) {
                    playerEntry.getKey().dataPacket(pk);
                }
            }
        }
    }
}
