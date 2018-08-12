package ch.awae.netcode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
class ServerCapabilities {

	private final boolean enablePublicChannels;
	private final boolean enableServerCommands;

	String getFeaturesString(String versionString) {
		StringBuilder sb = new StringBuilder(versionString);
		if (enablePublicChannels)
			sb.append("," + Parser.PUBLIC_CHANNELS);
		if (enableServerCommands)
			sb.append("," + Parser.SERVER_COMMANDS);
		return sb.toString();
	}

}
