package trealworld.adapter.web;

/**
 * 가입 요청의 본문.
 */
record RegisterMemberRequest(String username, String email, String password) {

	/** 비밀번호의 원문이 로그에 남지 않게 한다. */
	@Override
	public String toString() {
		return "RegisterMemberRequest[username=" + username + ", email=" + email + ", password=****]";
	}

}
