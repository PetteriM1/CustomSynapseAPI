package org.itxtech.synapseapi.event.player;

import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import org.itxtech.synapseapi.SynapsePlayer;

public class SynapseFullServerPlayerTransferEvent extends SynapsePlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public SynapseFullServerPlayerTransferEvent(SynapsePlayer player) {
        super(player);
    }

    public static HandlerList getHandlers() {
        return handlers;
    }
}
