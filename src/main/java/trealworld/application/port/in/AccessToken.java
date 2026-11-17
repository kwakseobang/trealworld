package trealworld.application.port.in;

/**
 * 액세스 토큰. 로그인의 증거다. 이후의 요청에 낸다.
 * 도메인 모델이 아니다. (Domain 01)
 */
public record AccessToken(String value) {

	public AccessToken {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("액세스 토큰이 비어 있다");
		}
	}

	/** 토큰이 로그에 남지 않게 한다. 토큰을 가진 사람은 그 회원으로 요청할 수 있다. */
	@Override
	public String toString() {
		return "AccessToken[****]";
	}

}
