package ch.awae.netcode;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@AllArgsConstructor
@ToString
@NoArgsConstructor
final class MessageImpl implements Message {
	private String userId, targetId;
	private Timestamp time;
	private boolean privateMessage, managementMessage;

	@JsonIgnore
	private Object payload;

	public byte[] getData() throws IOException {
		return Parser.pojo2array((Serializable) payload);
	}

	public void setData(byte[] array) throws ClassNotFoundException, IOException {
		payload = Parser.array2pojo(array);
	}

}