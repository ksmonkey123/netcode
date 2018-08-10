package ch.awae.netcode;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
final class GreetingMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	private final ChannelConfiguration config;
	private final String[] users;
}
