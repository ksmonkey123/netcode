package ch.awae.netcode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
class ServerCapabilities {

	private final boolean enablePublicChannels;

	String getFeaturesString(String versionString) {
		return enablePublicChannels ? (versionString + "," + Parser.PUBLIC_CHANNELS) : versionString;
	}

}
