package ch.awae.netcode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
class NetcodeHandshakeRequest {

	private String appId, channelId, userId;
	private boolean master;
	private ChannelConfiguration config;

}
