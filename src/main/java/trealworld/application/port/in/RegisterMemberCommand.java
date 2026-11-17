package trealworld.application.port.in;

/**
 * 가입에 내는 값. 검사하기 전의 글자다.
 */
public record RegisterMemberCommand(String username, String email, String password) {

	/** 비밀번호의 원문이 로그에 남지 않게 한다. */
	@Override
	public String toString() {
		return "RegisterMemberCommand[username=" + username + ", email=" + email + ", password=****]";
	}

}
