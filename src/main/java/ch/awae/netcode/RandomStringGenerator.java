package ch.awae.netcode;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.function.Supplier;

/**
 * A generator for generating strings of the pattern [a-zA-Z0-9]{n}.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 */
public final class RandomStringGenerator implements Supplier<String> {

	private final SecureRandom random = new SecureRandom();
	private final int LENGTH;
	private final int SEED_LENGTH;

	public RandomStringGenerator(int length) {
		if (length < 0)
			throw new IllegalArgumentException("length may not be negative");
		this.LENGTH = length;
		this.SEED_LENGTH = (int) Math.ceil(length / 4f) * 3;
	}

	@Override
	public String get() {
		while (true) {
			// 24 bytes yield 32 chars
			byte bytes[] = new byte[SEED_LENGTH];
			random.nextBytes(bytes);
			Encoder encoder = Base64.getUrlEncoder().withoutPadding();
			String candidate = encoder.encodeToString(bytes);
			if (candidate.contains("_") || candidate.contains("-"))
				continue;
			return candidate.substring(0, LENGTH);
		}
	}

}
