package trealworld.application.service;

import java.util.UUID;

import trealworld.application.port.in.GetMemberUseCase;
import trealworld.application.port.in.MemberNotFoundException;
import trealworld.application.port.out.MemberRepository;
import trealworld.domain.Member;

/**
 * 조회를 처리한다. R10을 지키는 곳이다.
 * 조회의 대상은 받은 식별자의 회원 하나다. 다른 회원을 찾는 방법을 두지 않는다.
 */
public class GetMemberService implements GetMemberUseCase {

	private final MemberRepository members;

	public GetMemberService(MemberRepository members) {
		this.members = members;
	}

	@Override
	public Member getMember(UUID memberId) {
		if (memberId == null) {
			throw new MemberNotFoundException();
		}
		return members.findById(memberId).orElseThrow(MemberNotFoundException::new);
	}

}
