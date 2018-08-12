package ch.awae.netcode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

final class Parser {

	final static String SERVER_VERSION = "NETCODE_1,SIMPLE_QUERY";
	final static String PUBLIC_CHANNELS = "PUBLIC_CHANNELS";
	final static String SERVER_COMMANDS = "SERVER_COMMANDS";
	final static String SIMPLE_TALK = "SIMPLE_QUERY";

	private final static ObjectMapper mapper = new ObjectMapper();

	@SneakyThrows(JsonProcessingException.class)
	static String pojo2json(Object object) {
		return mapper.writeValueAsString(object);
	}

	@SneakyThrows(IOException.class)
	static <T> T json2pojo(String string, Class<T> type) {
		return mapper.readValue(string, type);
	}

	@SneakyThrows(IOException.class)
	static byte[] pojo2array(Serializable object) {
		try (ByteArrayOutputStream bo = new ByteArrayOutputStream();
				ObjectOutputStream so = new ObjectOutputStream(bo)) {
			so.writeObject(object);
			so.flush();
			return bo.toByteArray();
		}
	}

	@SneakyThrows({ IOException.class, ClassNotFoundException.class })
	static Object array2pojo(byte[] array) {
		try (ByteArrayInputStream bi = new ByteArrayInputStream(array);
				ObjectInputStream si = new ObjectInputStream(bi)) {
			return si.readObject();
		}
	}

}
