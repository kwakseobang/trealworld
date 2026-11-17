package trealworld.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.PasswordHash;
import trealworld.domain.Username;

/**
 * 보안 어댑터가 액세스 토큰을 검사한다. (ADR-0003, AC-3.3)
 * 발급하는 쪽(JwtAccessTokenIssuer)과 검사하는 쪽이 같은 키와 같은 Clock을 쓴다.
 */
class AccessTokenDecoderTests {

	private static final Instant ISSUED = Instant.parse("2026-11-17T09:00:00Z");

	private final JwtSigningKey key = new JwtSigningKey("test-secret-test-secret-test-secret-0123");
	private final String token = new JwtAccessTokenIssuer(key, at(ISSUED)).issue(member(7L)).value();

	@Test
	void ac3_3_a_token_that_this_server_issued_is_accepted() {
		Jwt jwt = decoderAt(ISSUED.plusSeconds(1)).decode(token);

		assertThat(jwt.getSubject()).isEqualTo("7");
		assertThat(jwt.getExpiresAt()).isEqualTo(ISSUED.plus(Duration.ofHours(1)));
	}

	/** ADR-0003: 유효 시간은 1시간이다. 여유 시간을 두지 않는다. */
	@Test
	void ac3_3_a_token_is_accepted_until_it_expires() {
		assertThat(decoderAt(ISSUED.plus(Duration.ofMinutes(59)).plusSeconds(59)).decode(token)).isNotNull();
		assertThat(decoderAt(ISSUED.plus(Duration.ofHours(1))).decode(token)).isNotNull();
	}

	@Test
	void ac3_3_an_expired_token_is_rejected() {
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoderAt(ISSUED.plus(Duration.ofHours(1)).plusSeconds(1)).decode(token));
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoderAt(ISSUED.plus(Duration.ofDays(1))).decode(token));
	}

	/** 시각은 주입한 Clock에서 읽는다. 실제 시각으로는 이 토큰은 이미 만료됐거나 아직 먼 미래다. */
	@Test
	void adr0003_the_time_is_read_from_the_injected_clock() {
		Instant longAgo = Instant.parse("2001-01-01T00:00:00Z");
		String old = new JwtAccessTokenIssuer(key, at(longAgo)).issue(member(7L)).value();

		assertThat(decoderAt(longAgo.plusSeconds(60)).decode(old).getSubject()).isEqualTo("7");
	}

	@Test
	void ac3_3_a_token_signed_with_another_key_is_rejected() {
		JwtSigningKey another = new JwtSigningKey("another-secret-another-secret-0123456789");
		String forged = new JwtAccessTokenIssuer(another, at(ISSUED)).issue(member(7L)).value();

		assertThatExceptionOfType(JwtException.class).isThrownBy(() -> decoderAt(ISSUED).decode(forged));
	}

	@Test
	void ac3_3_a_token_without_a_signature_is_rejected() {
		String unsigned = new PlainJWT(new JWTClaimsSet.Builder().subject("7")
				.expirationTime(java.util.Date.from(ISSUED.plusSeconds(3600))).build()).serialize();

		assertThatExceptionOfType(JwtException.class).isThrownBy(() -> decoderAt(ISSUED).decode(unsigned));
	}

	@Test
	void ac3_3_a_token_with_a_changed_signature_is_rejected() {
		String changed = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

		assertThatExceptionOfType(JwtException.class).isThrownBy(() -> decoderAt(ISSUED).decode(changed));
		assertThatExceptionOfType(JwtException.class).isThrownBy(() -> decoderAt(ISSUED).decode("not-a-token"));
	}

	private JwtDecoder decoderAt(Instant now) {
		return SecurityConfig.accessTokenDecoder(key, at(now));
	}

	private static Clock at(Instant instant) {
		return Clock.fixed(instant, ZoneOffset.UTC);
	}

	private static Member member(Long id) {
		Member member = Member.register(new Username("jake"), new Email("jake@example.com"),
				new PasswordHash("stored-hash"));
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

}
