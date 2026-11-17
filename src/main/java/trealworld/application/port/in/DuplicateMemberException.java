package trealworld.application.port.in;

/**
 * 이메일이나 사용자 이름이 이미 있다. (R6, R7)
 */
public class DuplicateMemberException extends RuntimeException {

	private final String field;

	/**
	 * @param field 이미 있는 값의 이름
	 */
	public DuplicateMemberException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}

}
