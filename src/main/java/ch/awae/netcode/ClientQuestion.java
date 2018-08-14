package ch.awae.netcode;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
final class ClientQuestion implements Serializable {
	private final static long serialVersionUID = 1L;
	private final long commandId;
	private final Serializable data;
}

@Getter
@AllArgsConstructor
final class ClientAnswer implements Serializable {
	private final static long serialVersionUID = 1L;
	private final long commandId;
	private final Serializable data;
}
