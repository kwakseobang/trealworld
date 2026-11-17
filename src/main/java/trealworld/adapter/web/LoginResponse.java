package trealworld.adapter.web;

import trealworld.application.port.in.AccessToken;

/**
 * 로그인의 응답용 객체. 액세스 토큰만 있다.
 */
record LoginResponse(String accessToken) {

	static LoginResponse of(AccessToken accessToken) {
		return new LoginResponse(accessToken.value());
	}

	/** 토큰이 로그에 남지 않게 한다. */
	@Override
	public String toString() {
		return "LoginResponse[accessToken=****]";
	}

}
