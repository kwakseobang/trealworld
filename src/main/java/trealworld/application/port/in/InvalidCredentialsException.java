package trealworld.application.port.in;

/**
 * 로그인이 실패했다. 이유(없는 이메일, 틀린 비밀번호)를 담지 않는다. (R9)
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("이메일 또는 비밀번호가 틀리다");
	}

}
