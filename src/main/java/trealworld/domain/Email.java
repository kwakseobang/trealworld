package trealworld.domain;

import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.persistence.Embeddable;

/**
 * 이메일. 올바른 형식이고, 254자 이하다. 소문자로 저장한다. (R2)
 * 한 자는 유니코드의 코드 포인트 하나다.
 */
@Embeddable
public record Email(String value) {

	public static final int MAX_CHARACTERS = 254;

	private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

	public Email {
		if (value == null || !FORMAT.matcher(value).matches()) {
			throw new IllegalArgumentException("이메일의 형식이 올바르지 않다");
		}
		if (value.codePointCount(0, value.length()) > MAX_CHARACTERS) {
			throw new IllegalArgumentException("이메일은 254자 이하여야 한다");
		}
		value = value.toLowerCase(Locale.ROOT);
	}

}
