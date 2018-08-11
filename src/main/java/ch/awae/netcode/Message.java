package ch.awae.netcode;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public interface Message {

	String getUserId();

	Timestamp getTime();

	Serializable getPayload();

	boolean isPrivateMessage();

}

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
final class MessageImpl implements Message {
	private String userId, targetId;
	private Timestamp time;
	private boolean privateMessage, managementMessage;

	@JsonIgnore
	private Serializable payload;

	public byte[] getData() throws IOException {
		return Parser.pojo2array(payload);
	}

	public void setData(byte[] array) throws ClassNotFoundException, IOException {
		payload = (Serializable) Parser.array2pojo(array);
	}

}

@Data
@AllArgsConstructor
final class UserChange implements Serializable {
	private static final long serialVersionUID = 1L;
	private String userId;
	private boolean joined;
}


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
		config = (ChannelConfiguration) Parser.array2pojo(array);
	}

}

@Getter
@AllArgsConstructor
final class GreetingMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	private final ChannelConfiguration config;
	private final String[] users;
}
