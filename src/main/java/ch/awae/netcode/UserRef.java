package ch.awae.netcode;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class UserRef {

	private final NetcodeClient owner;
	private final @Getter String userId;

	public void send(Serializable payload) {
		owner.sendPrivately(userId, payload);
	}

	public Serializable ask(Serializable payload) throws InterruptedException, TimeoutException {
		return owner.ask(userId, payload);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof UserRef))
			return false;
		UserRef other = (UserRef) obj;
		return this.owner == other.owner && this.userId.equals(other.userId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(owner, userId);
	}

}
