package ch.awae.netcode;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Configuration Object for channels.
 * 
 * To set up a channel the following data is required:
 * <ul>
 * <li>maxClients - the number of clients that can join a channel (default:
 * Integer.MAX_VALUE)</li>
 * <li>bounceMessages - if set to true the sending client receives his own
 * messages too (default: true)</li>
 * </ul>
 * When connecting the server sends an instance of this class to each client.
 * These instances also contain the ID of the connected channel.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 */
@Builder
@Getter
@ToString
public class ChannelConfiguration implements Serializable {
	private static final long serialVersionUID = 1L;

	@Builder.Default
	private int maxClients = Integer.MAX_VALUE;
	@Builder.Default
	private boolean bounceMessages = true;

	private String channelId;

	public static ChannelConfiguration getDefault() {
		return builder().build();
	}

	void setChannelId(String channelId) {
		this.channelId = channelId;
	}

}
