package trealworld.domain;

import lombok.Getter;

/**
 * 회원. 사용자 이름, 이메일, 비밀번호 해시가 항상 있다. (R1)
 * 비밀번호의 원문은 가지지 않는다. (R5)
 */
@Getter
public class Member {

	private final Username username;
	private final Email email;
	private final PasswordHash passwordHash;

	private Member(Username username, Email email, PasswordHash passwordHash) {
		this.username = required(username, "회원에게 사용자 이름이 없다");
		this.email = required(email, "회원에게 이메일이 없다");
		this.passwordHash = required(passwordHash, "회원에게 비밀번호 해시가 없다");
	}

	public static Member register(Username username, Email email, PasswordHash passwordHash) {
		return new Member(username, email, passwordHash);
	}

	private static <T> T required(T value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

}
