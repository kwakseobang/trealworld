package trealworld.domain;

import jakarta.persistence.Embeddable;

/**
 * 사용자 이름. 앞뒤 공백이 없다. 한 자 이상이고 50자 이하다. (R3)
 * 한 자는 유니코드의 코드 포인트 하나다.
 */
@Embeddable
public record Username(String value) {

	public static final int MAX_CHARACTERS = 50;

	public Username {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("사용자 이름이 비어 있다");
		}
		value = value.strip();
		if (value.codePointCount(0, value.length()) > MAX_CHARACTERS) {
			throw new IllegalArgumentException("사용자 이름은 50자 이하여야 한다");
		}
	}

}
