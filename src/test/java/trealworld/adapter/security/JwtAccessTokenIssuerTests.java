package trealworld.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

	private static final String SECRET = "test-secret-test-secret-test-secret-0123";
	private static final Instant NOW = Instant.parse("2026-11-17T09:00:00Z");

	private final JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void adr0003_the_token_is_a_jwt_signed_with_the_secret() throws Exception {
		SignedJWT jwt = SignedJWT.parse(issuer.issue(member(7L)).value());

		assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
		assertThat(jwt.verify(new MACVerifier(SECRET.getBytes(StandardCharsets.UTF_8)))).isTrue();
		assertThat(jwt.verify(new MACVerifier("another-secret-another-secret-012345".getBytes(StandardCharsets.UTF_8))))
				.isFalse();
	}

	/** ADR-0003: 토큰에는 회원의 식별자와 만료 시각만 넣는다. */
	@Test
	void adr0003_the_token_has_only_the_id_of_the_member_and_the_expiry() throws Exception {
		SignedJWT jwt = SignedJWT.parse(issuer.issue(member(7L)).value());

		assertThat(jwt.getJWTClaimsSet().getClaims()).containsOnlyKeys("sub", "exp");
		assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("7");
	}

	/** ADR-0003: 유효 시간은 1시간이다. 시각은 주입한 Clock에서 읽는다. */
	@Test
	void adr0003_the_token_expires_one_hour_after_it_is_issued() throws Exception {
		SignedJWT jwt = SignedJWT.parse(issuer.issue(member(7L)).value());

		assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(NOW.plusSeconds(3600));
	}

	/** AC-1.7과 같은 뜻: 토큰에 사용자 이름, 이메일, 비밀번호 해시가 없다. */
	@Test
	void adr0003_the_token_does_not_carry_the_values_of_the_member() throws Exception {
		String payload = SignedJWT.parse(issuer.issue(member(7L)).value()).getPayload().toString();

		assertThat(payload).doesNotContain("jake").doesNotContain("example.com").doesNotContain("stored-hash");
	}

	@Test
	void adr0003_tokens_of_different_members_differ() {
		AccessToken jake = issuer.issue(member(7L));
		AccessToken finn = issuer.issue(member(8L));

		assertThat(jake).isNotEqualTo(finn);
	}

	@Test
	void adr0003_a_member_that_is_not_saved_gets_no_token() {
		assertThatIllegalArgumentException().isThrownBy(() -> issuer.issue(member(null)));
	}

	@Test
	void adr0003_a_secret_shorter_than_32_bytes_is_rejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JwtAccessTokenIssuer("too-short", Clock.systemUTC()));
	}

	/** 키가 없으면 임의의 키를 만든다. 인스턴스마다 키가 다르다. */
	@Test
	void adr0003_without_a_secret_a_random_key_is_used() throws Exception {
		JwtAccessTokenIssuer first = new JwtAccessTokenIssuer("", Clock.fixed(NOW, ZoneOffset.UTC));
		JwtAccessTokenIssuer second = new JwtAccessTokenIssuer("", Clock.fixed(NOW, ZoneOffset.UTC));

		assertThat(first.issue(member(7L))).isNotEqualTo(second.issue(member(7L)));
	}

	private static Member member(Long id) {
		Member member = Member.register(new Username("jake"), new Email("jake@example.com"),
				new PasswordHash("stored-hash"));
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

}
