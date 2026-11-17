package trealworld.adapter.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import trealworld.application.port.in.GetMemberUseCase;
import trealworld.application.port.in.MemberNotFoundException;

/**
 * 검사를 통과한 토큰을 "누가 요청했는가"로 바꾼다.
 * 토큰이 유효해도 토큰의 주인인 회원이 없으면 인증 실패다. (ADR-0003, AC-3.5)
 * 인증의 이름은 회원의 식별자다. 웹 어댑터는 그 이름을 java.security.Principal로 읽는다.
 */
@Component
class MemberAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final GetMemberUseCase getMember;

	MemberAuthenticationConverter(GetMemberUseCase getMember) {
		this.getMember = getMember;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Long memberId = memberId(jwt.getSubject());
		try {
			getMember.getMember(memberId);
		}
		catch (MemberNotFoundException exception) {
			throw new BadCredentialsException("토큰의 주인인 회원이 없다");
		}
		return new JwtAuthenticationToken(jwt, List.of(), String.valueOf(memberId));
	}

	/** 이 서버가 발급한 토큰의 sub는 숫자만 있는 글자다. 다른 모양은 받지 않는다. */
	private static Long memberId(String subject) {
		if (subject == null || !subject.matches("[0-9]{1,18}")) {
			throw new BadCredentialsException("토큰의 주인이 회원의 식별자가 아니다");
		}
		return Long.valueOf(subject);
	}

}
