package ch.awae.netcode;

import java.sql.Timestamp;

final class MessageFactory {

	static MessageImpl serverMessage(Object data) {
		return new MessageImpl(null, null, new Timestamp(System.currentTimeMillis()), false, true, data);
	}

	static MessageImpl normalMessage(String userId, Object data) {
		return new MessageImpl(userId, null, new Timestamp(System.currentTimeMillis()), false, false, data);
	}

	static MessageImpl privateMessage(String userId, String targetId, Object data) {
		return new MessageImpl(userId, targetId, new Timestamp(System.currentTimeMillis()), true, false, data);
	}

}
