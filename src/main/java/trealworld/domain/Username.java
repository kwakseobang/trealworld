package trealworld.domain;

/**
 * 사용자 이름. 비어 있지 않고, 앞뒤 공백이 없다. (R3)
 */
public record Username(String value) {

	public Username {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("사용자 이름이 비어 있다");
		}
		value = value.strip();
	}

}
