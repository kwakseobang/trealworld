package trealworld.application.port.in;

/**
 * 식별자의 회원이 없다. (AC-3.5)
 */
public class MemberNotFoundException extends RuntimeException {

	public MemberNotFoundException() {
		super("회원이 없다");
	}

}
