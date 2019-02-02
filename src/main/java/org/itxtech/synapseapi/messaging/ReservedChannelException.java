package org.itxtech.synapseapi.messaging;

@SuppressWarnings("serial")
public class ReservedChannelException extends RuntimeException {

    public ReservedChannelException() {
        this("Attempted to register for a reserved channel name.");
    }

    public ReservedChannelException(String name) {
        super("Attempted to register for a reserved channel name ('" + name + "')");
    }
}
