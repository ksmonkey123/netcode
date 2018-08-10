package ch.awae.netcode;

import java.io.IOException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
class NetcodeHandshakeRequest {

	private String appId, channelId, userId;
	private boolean master;
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private ChannelConfiguration config;

	public byte[] getData() throws IOException {
		return Parser.pojo2array(config);
	}

	public void setData(byte[] array) throws ClassNotFoundException, IOException {
		config = Parser.array2pojo(array, ChannelConfiguration.class);
	}

}
