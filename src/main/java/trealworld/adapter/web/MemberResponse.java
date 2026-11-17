package trealworld.adapter.web;

import trealworld.domain.Member;

/**
 * 회원의 응답용 객체. 비밀번호 해시가 없다. (AC-1.7, ADR-0005)
 */
record MemberResponse(String username, String email) {

	static MemberResponse of(Member member) {
		return new MemberResponse(member.getUsername().value(), member.getEmail().value());
	}

}
