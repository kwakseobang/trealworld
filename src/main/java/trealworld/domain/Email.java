package trealworld.domain;

import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.persistence.Embeddable;

/**
 * 이메일. 올바른 형식이고, 254자 이하다. 소문자로 저장한다. (R2)
 * 한 자는 유니코드의 코드 포인트 하나다. 길이는 저장하는 값(소문자)으로 센다.
 */
@Embeddable
public record Email(String value) {

	public static final int MAX_CHARACTERS = 254;

	/**
	 * 이름@도메인. 도메인은 점으로 나눈 2개 이상의 부분이다. 빈 부분은 없다.
	 * 공백(\s)은 유니코드의 공백이다. U+00A0과 U+3000을 포함한다.
	 */
	private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+",
			Pattern.UNICODE_CHARACTER_CLASS);

	public Email {
		if (value == null) {
			throw new IllegalArgumentException("이메일의 형식이 올바르지 않다");
		}
		value = value.toLowerCase(Locale.ROOT);
		if (value.codePointCount(0, value.length()) > MAX_CHARACTERS) {
			throw new IllegalArgumentException("이메일은 254자 이하여야 한다");
		}
		if (!FORMAT.matcher(value).matches()) {
			throw new IllegalArgumentException("이메일의 형식이 올바르지 않다");
		}
	}

}
