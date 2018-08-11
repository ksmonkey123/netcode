package ch.awae.netcode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.fasterxml.jackson.databind.ObjectMapper;

final class Parser {

	final static String PROTOCOL_VERSION = "NETCODE_1";

	private final static ObjectMapper mapper = new ObjectMapper();

	static String pojo2json(Object object) throws IOException {
		return mapper.writeValueAsString(object);
	}

	static <T> T json2pojo(String string, Class<T> type) throws IOException {
		return mapper.readValue(string, type);
	}

	static byte[] pojo2array(Serializable object) throws IOException {
		try (ByteArrayOutputStream bo = new ByteArrayOutputStream();
				ObjectOutputStream so = new ObjectOutputStream(bo)) {
			so.writeObject(object);
			so.flush();
			return bo.toByteArray();
		}
	}

	static Object array2pojo(byte[] array) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bi = new ByteArrayInputStream(array);
				ObjectInputStream si = new ObjectInputStream(bi)) {
			return si.readObject();
		}
	}

}
