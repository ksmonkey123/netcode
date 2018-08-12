package ch.awae.netcode;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
final class ServerCommand implements Serializable {
	private final static long serialVersionUID = 1L;
	private final long commandId;
	private final String verb;
	private final Serializable data;
}

@Getter
@AllArgsConstructor
final class ServerCommandResponse implements Serializable {
	private final static long serialVersionUID = 1L;
	private final long commandId;
	private final Serializable data;
}