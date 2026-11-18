package trealworld.application.port.in;

import java.util.UUID;

import trealworld.domain.Member;

/**
 * 조회: 회원이 자기의 정보를 받는다. (Spec 01의 목표 3)
 */
public interface GetMemberUseCase {

	/**
	 * @param memberId 요청한 회원의 식별자. 액세스 토큰의 주인이다. 다른 회원의 식별자를 넘겨서는 안 된다 (R10)
	 * @throws MemberNotFoundException 그 회원이 없다
	 */
	Member getMember(UUID memberId);

}
