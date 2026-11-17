package trealworld.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 이메일. 올바른 형식이고, 소문자로 저장한다. (R2)
 */
public record Email(String value) {

	private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

	public Email {
		if (value == null || !FORMAT.matcher(value).matches()) {
			throw new IllegalArgumentException("이메일의 형식이 올바르지 않다");
		}
		value = value.toLowerCase(Locale.ROOT);
	}

}
