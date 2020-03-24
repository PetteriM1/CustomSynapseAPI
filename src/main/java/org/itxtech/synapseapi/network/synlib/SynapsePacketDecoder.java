package org.itxtech.synapseapi.network.synlib;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.itxtech.synapseapi.SynapseAPI;
import org.itxtech.synapseapi.network.SynapseInterface;

import java.util.List;

/**
 * SynapsePacketDecoder
 * ===============
 * author: boybook
 * Nemisys Project
 * ===============
 */
public class SynapsePacketDecoder extends ReplayingDecoder<SynapsePacketDecoder.State> {

    private final SynapseProtocolHeader header = new SynapseProtocolHeader();

    public SynapsePacketDecoder() {
        super(State.HEADER_MAGIC);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case HEADER_MAGIC:
                checkMagic(in.readShort());
                checkpoint(State.HEADER_ID);
            case HEADER_ID:
                header.pid(in.readByte());
                checkpoint(State.HEADER_BODY_LENGTH);
            case HEADER_BODY_LENGTH:
                header.bodyLength(in.readInt());
                checkpoint(State.BODY);
            case BODY:
                int bodyLength = header.bodyLength();
                if (bodyLength < 5242880) {
                    byte[] bytes = new byte[bodyLength];
                    in.readBytes(bytes);
                    out.add(SynapseInterface.getPacket((byte) header.pid(), bytes));
                    break;
                } else {
                    SynapseAPI.getInstance().getLogger().warning("Ignoring packet with body length " + bodyLength);
                    return;
                }
            default:
                break;
        }
        checkpoint(State.HEADER_MAGIC);
    }

    private int checkBodyLength(int bodyLength) throws SynapseContextException {
        if (bodyLength > 5242880) {
            throw new SynapseContextException("Body of request is bigger than limit value 5242880");
        }
        return bodyLength;
    }

    private void checkMagic(short magic) throws SynapseContextException {
        if (SynapseProtocolHeader.MAGIC != magic) {
            throw new SynapseContextException("Magic value is not equal -17730");
        }
    }

    enum State {
        HEADER_MAGIC, HEADER_ID, HEADER_BODY_LENGTH, BODY
    }
}
