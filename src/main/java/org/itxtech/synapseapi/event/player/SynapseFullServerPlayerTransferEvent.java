package org.itxtech.synapseapi.event.player;

import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import org.itxtech.synapseapi.SynapsePlayer;

public class SynapseFullServerPlayerTransferEvent extends SynapsePlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean kick;

    public SynapseFullServerPlayerTransferEvent(SynapsePlayer player) {
        super(player);
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

    public boolean getKick() {
        return kick;
    }

    public void setKick(boolean kick) {
        this.kick = kick;
    }
}
