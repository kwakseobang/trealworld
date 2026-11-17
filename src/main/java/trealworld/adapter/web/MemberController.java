package trealworld.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import trealworld.application.port.in.RegisterMemberCommand;
import trealworld.application.port.in.RegisterMemberUseCase;
import trealworld.domain.Member;

@RestController
@RequestMapping("/api/members")
class MemberController {

	private final RegisterMemberUseCase registerMember;

	MemberController(RegisterMemberUseCase registerMember) {
		this.registerMember = registerMember;
	}

	/** 가입. (ADR-0005: POST /api/members → 201) */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	MemberResponse register(@RequestBody RegisterMemberRequest request) {
		Member member = registerMember
				.register(new RegisterMemberCommand(request.username(), request.email(), request.password()));
		return MemberResponse.of(member);
	}

}
