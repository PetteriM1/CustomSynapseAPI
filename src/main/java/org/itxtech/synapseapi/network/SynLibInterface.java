package org.itxtech.synapseapi.network;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.protocol.BatchPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.session.NetworkPlayerSession;
import cn.nukkit.utils.Binary;
import org.itxtech.synapseapi.network.protocol.spp.RedirectPacket;

import java.net.InetSocketAddress;

/**
 * Created by boybook on 16/6/24.
 */
public class SynLibInterface implements SourceInterface {

    private final SynapseInterface synapseInterface;

    public SynLibInterface(SynapseInterface synapseInterface) {
        this.synapseInterface = synapseInterface;
    }

    @Override
    public int getNetworkLatency(Player player) {
        return 0;
    }

    @Override
    public void emergencyShutdown() {
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public Integer putPacket(Player player, DataPacket packet) {
        return this.putPacket(player, packet, false);
    }

    @Override
    public Integer putPacket(Player player, DataPacket packet, boolean needACK) {
        return this.putPacket(player, packet, needACK, false);
    }

    @Override
    public Integer putPacket(Player player, DataPacket packet, boolean needACK, boolean immediate) {
        RedirectPacket pk = new RedirectPacket();
        pk.uuid = player.getUniqueId();
        pk.direct = immediate;
        pk.mcpeBuffer = packet instanceof BatchPacket ? Binary.appendBytes((byte) 0xfe, ((BatchPacket) packet).payload) : packet.getBuffer();

        if (pk.mcpeBuffer.length >= 5242880) {
            Server.getInstance().getLogger().error("[Synapse] Too big packet! (pid: " + packet.pid() + ", player: " + player.getName() + ')');
        } else {
            this.synapseInterface.putPacket(pk);
        }
        return 0;
    }

    @Override
    public NetworkPlayerSession getSession(InetSocketAddress inetSocketAddress) {
        return null;
    }

    @Override
    public boolean process() {
        return false;
    }

    @Override
    public void close(Player player, String reason) {
    }

    @Override
    public void close(Player player) {
    }

    @Override
    public void shutdown() {
    }
}
