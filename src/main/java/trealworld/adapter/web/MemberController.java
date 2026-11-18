package trealworld.adapter.web;

import java.util.UUID;
import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import trealworld.application.port.in.GetMemberUseCase;
import trealworld.application.port.in.RegisterMemberCommand;
import trealworld.application.port.in.RegisterMemberUseCase;
import trealworld.domain.Member;

@RestController
@RequestMapping("/api/members")
class MemberController {

	private final RegisterMemberUseCase registerMember;
	private final GetMemberUseCase getMember;

	MemberController(RegisterMemberUseCase registerMember, GetMemberUseCase getMember) {
		this.registerMember = registerMember;
		this.getMember = getMember;
	}

	/** 가입. (ADR-0005: POST /api/members → 201) */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	MemberResponse register(@RequestBody RegisterMemberRequest request) {
		Member member = registerMember
				.register(new RegisterMemberCommand(request.username(), request.email(), request.password()));
		return MemberResponse.of(member);
	}

	/**
	 * 내 정보 조회. (ADR-0005: GET /api/members/me → 200)
	 * 조회의 대상은 항상 요청한 회원이다. 요청의 본문과 경로에서 대상을 읽지 않는다. (AC-3.4, R10)
	 *
	 * @param requester 보안 어댑터가 인증한 회원. 이름이 회원의 식별자다
	 */
	@GetMapping("/me")
	MemberResponse me(Principal requester) {
		return MemberResponse.of(getMember.getMember(UUID.fromString(requester.getName())));
	}

}
