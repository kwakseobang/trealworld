package trealworld.application.port.in;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 값이 비었거나 형식이 틀리다. 어느 값이 문제인지 알려준다. (AC-1.2)
 */
public class InvalidValuesException extends RuntimeException {

	private final Map<String, String> errors;

	/**
	 * @param errors 값의 이름과 이유. 순서를 지킨다
	 */
	public InvalidValuesException(Map<String, String> errors) {
		super("값이 올바르지 않다: " + errors.keySet());
		this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
	}

	public Map<String, String> getErrors() {
		return errors;
	}

}
