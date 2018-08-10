package ch.awae.netcode;

import java.sql.Timestamp;

public interface Message {

	String getUserId();

	Timestamp getTime();

	Object getPayload();

	boolean isPrivateMessage();

}
