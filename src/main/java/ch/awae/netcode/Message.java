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

/**
 * Base interface for received messages.
 * 
 * Whenever a message is sent the sending client attaches some meta-data to it.
 * This data is not visible to the sender.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 */
public interface Message {

	/**
	 * The userId of the sending client.
	 */
	String getUserId();

	/**
	 * The time the message was created.
	 */
	Timestamp getTime();

	/**
	 * The message payload.
	 */
	Serializable getPayload();

	/**
	 * Indicates if the message has been sent privately.
	 */
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

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
final class SSMessageImpl implements Message {
	private String userId, targetId;
	private Timestamp time;
	private boolean privateMessage;
	private boolean managementMessage;	
	private byte[] data;
	
	@Override
	@JsonIgnore
	public Serializable getPayload() {
		return (Serializable) Parser.array2pojo(data);
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
