package ch.awae.netcode.internal;

import ch.awae.netcode.client.ChannelInformation;

public interface FullChannelInformation extends ChannelInformation {
    String[] getUsers();
}
