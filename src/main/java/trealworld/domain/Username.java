package trealworld.domain;

import jakarta.persistence.Embeddable;

/**
 * 사용자 이름. 앞뒤 공백이 없다. 한 자 이상이고 50자 이하다. (R3)
 * 한 자는 유니코드의 코드 포인트 하나다. 공백은 유니코드의 공백이다.
 */
@Embeddable
public record Username(String value) {

	public static final int MAX_CHARACTERS = 50;

	public Username {
		value = value == null ? "" : withoutSurroundingWhitespace(value);
		if (value.isEmpty()) {
			throw new IllegalArgumentException("사용자 이름이 비어 있다");
		}
		if (value.codePointCount(0, value.length()) > MAX_CHARACTERS) {
			throw new IllegalArgumentException("사용자 이름은 50자 이하여야 한다");
		}
	}

	private static String withoutSurroundingWhitespace(String value) {
		int start = 0;
		int end = value.length();
		while (start < end && isWhitespace(value.codePointAt(start))) {
			start += Character.charCount(value.codePointAt(start));
		}
		while (end > start && isWhitespace(value.codePointBefore(end))) {
			end -= Character.charCount(value.codePointBefore(end));
		}
		return value.substring(start, end);
	}

	/**
	 * String.strip()은 줄을 바꾸지 않는 공백(U+00A0, U+2007, U+202F)을 공백으로 보지 않는다.
	 * 화면에서는 같은 공백이다. 그래서 둘을 함께 본다.
	 */
	private static boolean isWhitespace(int codePoint) {
		return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
	}

}
