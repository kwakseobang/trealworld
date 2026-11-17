package trealworld.application.port.out;

import trealworld.application.port.in.AccessToken;
import trealworld.domain.Member;

/**
 * 회원의 액세스 토큰을 발급한다.
 */
public interface AccessTokenIssuer {

	/**
	 * @param member 저장된 회원. 식별자가 있다
	 */
	AccessToken issue(Member member);

}
