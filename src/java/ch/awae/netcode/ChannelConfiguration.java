package ch.awae.netcode;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
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

}
