package org.itxtech.synapseapi.runnable;

import cn.nukkit.network.protocol.PlayStatusPacket;
import org.itxtech.synapseapi.SynapsePlayer;

/**
 * Created by NycuRO on 19/9/16.
 */
public class FastTransferHackRunnable implements Runnable {

    private SynapsePlayer player;

    public FastTransferHackRunnable(SynapsePlayer player) {
        this.player = player;
    }

    @Override
    public void run() {
        PlayStatusPacket playStatusPacket = new PlayStatusPacket();
        playStatusPacket.status = PlayStatusPacket.PLAYER_SPAWN;
        player.dataPacket(playStatusPacket);
    }
}
