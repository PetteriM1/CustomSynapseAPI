package org.itxtech.synapseapi.messaging;

@SuppressWarnings("serial")
public class ChannelNameTooLongException extends RuntimeException {

    public ChannelNameTooLongException() {
        super("Attempted to send a Plugin Message to a channel that was too large. The maximum length is 20 chars.");
    }

    public ChannelNameTooLongException(String channel) {
        super("Attempted to send a Plugin Message to a channel that was too large. The maximum length is 20 chars (attempted " + channel.length() + " - '" + channel + '.');
    }
}
