package org.itxtech.synapseapi.network;

import cn.nukkit.Server;
import org.itxtech.synapseapi.SynapseEntry;
import org.itxtech.synapseapi.network.protocol.spp.*;
import org.itxtech.synapseapi.network.synlib.SynapseClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by boybook on 16/6/24.
 */
public class SynapseInterface {

    private static final Map<Byte, SynapseDataPacket> packetPool = new HashMap<>();
    private final SynapseEntry synapse;
    private final SynapseClient client;
    private boolean connected;

    public SynapseInterface(SynapseEntry server, String ip, int port) {
        this.synapse = server;
        this.registerPackets();
        this.client = new SynapseClient(Server.getInstance().getLogger(), port, ip);
    }

    public static SynapseDataPacket getPacket(byte pid, byte[] buffer) {
        SynapseDataPacket clazz = packetPool.get(pid);
        if (clazz != null) {
            SynapseDataPacket pk = clazz.clone();
            pk.setBuffer(buffer, 0);
            return pk;
        }

        return null;
    }

    public static void registerPacket(byte id, SynapseDataPacket packet) {
        packetPool.put(id, packet);
    }

    public SynapseEntry getSynapse() {
        return synapse;
    }

    public void reconnect() {
        this.client.reconnect();
    }

    public void shutdown() {
        this.client.shutdown();
    }

    public void putPacket(SynapseDataPacket pk) {
        if (!pk.isEncoded) {
            pk.encode();
        }
        this.client.pushMainToThreadPacket(pk);
    }

    public boolean isConnected() {
        return connected;
    }

    public void process() {
        SynapseDataPacket pk = this.client.readThreadToMainPacket();

        while (pk != null) {
            try {
                this.handlePacket(pk);
            } catch (Throwable e) {
                getSynapse().getSynapse().getLogger().error("Exception while handling incoming packet", e);
            }
            pk = this.client.readThreadToMainPacket();
        }

        this.connected = this.client.isConnected();
        if (this.connected && this.client.isNeedAuth()) {
            this.synapse.connect();
            this.client.setNeedAuth(false);
        }
    }

    public void handlePacket(SynapseDataPacket pk) {
        if (pk != null) {
            pk.decode();
            this.synapse.handleDataPacket(pk);
        }
    }

    private void registerPackets() {
        packetPool.clear();
        registerPacket(SynapseInfo.HEARTBEAT_PACKET, new HeartbeatPacket());
        registerPacket(SynapseInfo.CONNECT_PACKET, new ConnectPacket());
        registerPacket(SynapseInfo.DISCONNECT_PACKET, new DisconnectPacket());
        registerPacket(SynapseInfo.REDIRECT_PACKET, new RedirectPacket());
        registerPacket(SynapseInfo.PLAYER_LOGIN_PACKET, new PlayerLoginPacket());
        registerPacket(SynapseInfo.PLAYER_LOGOUT_PACKET, new PlayerLogoutPacket());
        registerPacket(SynapseInfo.INFORMATION_PACKET, new InformationPacket());
        registerPacket(SynapseInfo.TRANSFER_PACKET, new TransferPacket());
        registerPacket(SynapseInfo.BROADCAST_PACKET, new BroadcastPacket());
        registerPacket(SynapseInfo.PLUGIN_MESSAGE_PACKET, new PluginMessagePacket());
        registerPacket(SynapseInfo.PLAYER_COUNT_PACKET, new PlayerCountPacket());
    }
}
