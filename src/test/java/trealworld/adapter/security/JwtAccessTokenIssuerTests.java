package trealworld.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import trealworld.application.port.in.AccessToken;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.PasswordHash;
import trealworld.domain.Username;

/**
 * 보안 어댑터가 발급하는 액세스 토큰. (ADR-0003)
 */
class JwtAccessTokenIssuerTests {

	private static final UUID JAKE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
	private static final UUID FINN_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
	private static final String SECRET = "test-secret-test-secret-test-secret-0123";
	private static final Instant NOW = Instant.parse("2026-11-17T09:00:00Z");

	private final JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(new JwtSigningKey(SECRET),
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void adr0003_the_token_is_a_jwt_signed_with_the_secret() throws Exception {
		SignedJWT jwt = SignedJWT.parse(issuer.issue(member(JAKE_ID)).value());

		assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
		assertThat(jwt.verify(new MACVerifier(SECRET.getBytes(StandardCharsets.UTF_8)))).isTrue();
		assertThat(jwt.verify(new MACVerifier("another-secret-another-secret-012345".getBytes(StandardCharsets.UTF_8))))
				.isFalse();
	}

	/** ADR-0003: 토큰에는 회원의 식별자와 만료 시각만 넣는다. */
	@Test
	void adr0003_the_token_has_only_the_id_of_the_member_and_the_expiry() throws Exception {
		SignedJWT jwt = SignedJWT.parse(issuer.issue(member(JAKE_ID)).value());

		assertThat(jwt.getJWTClaimsSet().getClaims()).containsOnlyKeys("sub", "exp");
		assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(JAKE_ID.toString());
	}

	/** ADR-0003: 유효 시간은 1시간이다. 시각은 주입한 Clock에서 읽는다. */
	@Test
	void adr0003_the_token_expires_one_hour_after_it_is_issued() throws Exception {
		SignedJWT jwt = SignedJWT.parse(issuer.issue(member(JAKE_ID)).value());

		assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(NOW.plusSeconds(3600));
	}

	/** AC-1.7과 같은 뜻: 토큰에 사용자 이름, 이메일, 비밀번호 해시가 없다. */
	@Test
	void adr0003_the_token_does_not_carry_the_values_of_the_member() throws Exception {
		String payload = SignedJWT.parse(issuer.issue(member(JAKE_ID)).value()).getPayload().toString();

		assertThat(payload).doesNotContain("jake").doesNotContain("example.com").doesNotContain("stored-hash");
	}

	@Test
	void adr0003_tokens_of_different_members_differ() {
		AccessToken jake = issuer.issue(member(JAKE_ID));
		AccessToken finn = issuer.issue(member(FINN_ID));

		assertThat(jake).isNotEqualTo(finn);
	}

	@Test
	void adr0003_a_member_that_is_not_saved_gets_no_token() {
		assertThatIllegalArgumentException().isThrownBy(() -> issuer.issue(member(null)));
	}

	private static Member member(UUID id) {
		Member member = Member.register(new Username("jake"), new Email("jake@example.com"),
				new PasswordHash("stored-hash"));
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

}
