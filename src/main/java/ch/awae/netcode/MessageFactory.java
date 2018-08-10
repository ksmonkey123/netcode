package ch.awae.netcode;

import java.io.Serializable;
import java.sql.Timestamp;

final class MessageFactory {

	static MessageImpl serverMessage(Serializable data) {
		return new MessageImpl(null, null, new Timestamp(System.currentTimeMillis()), false, true, data);
	}

	static MessageImpl normalMessage(String userId, Serializable data) {
		return new MessageImpl(userId, null, new Timestamp(System.currentTimeMillis()), false, false, data);
	}

	static MessageImpl privateMessage(String userId, String targetId, Serializable data) {
		return new MessageImpl(userId, targetId, new Timestamp(System.currentTimeMillis()), true, false, data);
	}

}
