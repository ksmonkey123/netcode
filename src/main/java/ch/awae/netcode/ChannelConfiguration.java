package ch.awae.netcode;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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
