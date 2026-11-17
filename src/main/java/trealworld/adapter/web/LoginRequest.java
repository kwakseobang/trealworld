package trealworld.adapter.web;

/**
 * 로그인 요청의 본문.
 */
record LoginRequest(String email, String password) {

	/** 비밀번호의 원문이 로그에 남지 않게 한다. */
	@Override
	public String toString() {
		return "LoginRequest[email=" + email + ", password=****]";
	}

}
