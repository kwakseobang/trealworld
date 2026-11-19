package trealworld.domain;

import java.nio.charset.StandardCharsets;

/**
 * 비밀번호. 회원만 아는 원문이다. 8자 이상이고, UTF-8로 72바이트 이하다. (R4)
 * 저장하지 않는다. 회원은 이 값을 가지지 않는다. (R5)
 */
public record Password(String value) {

	private static final int MIN_CHARACTERS = 8;
	private static final int MAX_BYTES = 72;

	public Password {
		// char의 수는 바이트의 수보다 많지 않다. 72를 넘는 입력은 바이트로 바꾸지 않고 거부한다.
		if (value == null || value.length() > MAX_BYTES
				|| value.codePointCount(0, value.length()) < MIN_CHARACTERS
				|| value.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
			throw new IllegalArgumentException("비밀번호는 8자 이상이고 72바이트 이하여야 한다");
		}
		// 짝이 없는 서로게이트는 UTF-8로 바꾸면 "?"가 된다. 서로 다른 비밀번호가 같은 해시가 된다.
		if (!StandardCharsets.UTF_8.newEncoder().canEncode(value)) {
			throw new IllegalArgumentException("비밀번호가 올바른 유니코드가 아니다");
		}
	}

	/** 원문이 로그에 남지 않게 한다. */
	@Override
	public String toString() {
		return "Password[****]";
	}

}
