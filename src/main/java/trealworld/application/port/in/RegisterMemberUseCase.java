package trealworld.application.port.in;

import trealworld.domain.Member;

/**
 * 가입: 방문자가 회원이 된다. (Spec 01의 목표 1)
 */
public interface RegisterMemberUseCase {

	/**
	 * @throws InvalidValuesException 값이 비었거나 형식이 틀리다
	 * @throws DuplicateMemberException 이메일이나 사용자 이름이 이미 있다
	 */
	Member register(RegisterMemberCommand command);

}
