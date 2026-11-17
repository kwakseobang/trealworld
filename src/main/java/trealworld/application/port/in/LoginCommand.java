package trealworld.application.port.in;

/**
 * 로그인에 내는 값. 검사하기 전의 글자다.
 */
public record LoginCommand(String email, String password) {

	/** 비밀번호의 원문이 로그에 남지 않게 한다. */
	@Override
	public String toString() {
		return "LoginCommand[email=" + email + ", password=****]";
	}

}
