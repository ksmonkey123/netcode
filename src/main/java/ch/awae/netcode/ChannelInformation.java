package ch.awae.netcode;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class ChannelInformation implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String id, name, createdBy;
	private final int memberCount, memberLimit;
	private final ChannelConfiguration configuration;

}
