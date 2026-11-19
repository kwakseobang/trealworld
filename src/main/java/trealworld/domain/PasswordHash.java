package trealworld.domain;

import jakarta.persistence.Embeddable;

/**
 * 비밀번호 해시. 비밀번호를 되돌릴 수 없게 바꾼 값이다. 비밀번호의 원문이 아니다. (R5)
 */
@Embeddable
public record PasswordHash(String value) {

	public PasswordHash {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("비밀번호 해시가 비어 있다");
		}
	}

	/** 해시가 로그에 남지 않게 한다. */
	@Override
	public String toString() {
		return "PasswordHash[****]";
	}

}
