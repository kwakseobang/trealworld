package trealworld.application.port.out;

/**
 * 저장하려는 회원의 이메일이나 사용자 이름이 이미 있다. 어느 값인지는 모른다.
 */
public class MemberNotUniqueException extends RuntimeException {

	public MemberNotUniqueException(Throwable cause) {
		super("회원의 이메일이나 사용자 이름이 이미 있다", cause);
	}

}
