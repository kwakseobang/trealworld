package trealworld.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import trealworld.application.port.in.GetMemberUseCase;
import trealworld.application.port.in.MemberNotFoundException;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.PasswordHash;
import trealworld.domain.Username;

/**
 * 검사를 통과한 토큰을 "누가 요청했는가"로 바꾼다. 회원이 없으면 인증 실패다. (ADR-0003, AC-3.5)
 */
class MemberAuthenticationConverterTests {

	private final List<Long> asked = new ArrayList<>();

	/** 식별자가 7인 회원만 있다. */
	private final GetMemberUseCase getMember = memberId -> {
		asked.add(memberId);
		if (!Long.valueOf(7L).equals(memberId)) {
			throw new MemberNotFoundException();
		}
		return Member.register(new Username("jake"), new Email("jake@example.com"), new PasswordHash("hash"));
	};

	private final MemberAuthenticationConverter converter = new MemberAuthenticationConverter(getMember);

	/** 웹 어댑터는 요청한 회원을 이 이름으로 안다 (java.security.Principal). */
	@Test
	void ac3_4_the_name_of_the_authentication_is_the_id_of_the_owner_of_the_token() {
		Authentication authentication = converter.convert(jwt("7"));

		assertThat(authentication.getName()).isEqualTo("7");
		assertThat(authentication.isAuthenticated()).isTrue();
		assertThat(authentication.getAuthorities()).isEmpty();
	}

	@Test
	void ac3_5_a_token_of_a_member_that_does_not_exist_is_an_authentication_failure() {
		assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> converter.convert(jwt("8")));
		assertThat(asked).containsExactly(8L);
	}

	/** 식별자가 아닌 값은 회원을 찾지 않고 거부한다. 500이 아니다. */
	@Test
	void ac3_5_a_subject_that_is_not_an_id_is_an_authentication_failure() {
		assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> converter.convert(jwt("jake")));
		assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> converter.convert(jwt("7.0")));
		assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> converter.convert(jwt(" 7")));
		assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> converter.convert(jwt(null)));
		assertThat(asked).isEmpty();
	}

	private static Jwt jwt(String subject) {
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256")
				.expiresAt(Instant.parse("2026-11-17T10:00:00Z"));
		return (subject == null ? builder : builder.subject(subject)).build();
	}

}
