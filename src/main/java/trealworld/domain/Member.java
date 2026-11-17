package trealworld.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 사용자 이름, 이메일, 비밀번호 해시가 항상 있다. (R1)
 * 비밀번호의 원문은 가지지 않는다. (R5)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

	/** 회원의 식별자. 저장할 때 생긴다. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "username", nullable = false, unique = true))
	private Username username;

	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true))
	private Email email;

	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "password_hash", nullable = false))
	private PasswordHash passwordHash;

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
