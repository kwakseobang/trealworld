package trealworld.adapter.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import trealworld.application.port.in.AccessToken;
import trealworld.application.port.out.AccessTokenIssuer;
import trealworld.domain.Member;

/**
 * 액세스 토큰을 JWT로 발급한다. HS256으로 서명한다. (ADR-0003)
 * 토큰에는 회원의 식별자(sub)와 만료 시각(exp)만 있다.
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

	static final Duration VALIDITY = Duration.ofHours(1);

	/** HS256의 키는 256비트 이상이어야 한다. */
	private static final int MIN_SECRET_BYTES = 32;

	private static final Log logger = LogFactory.getLog(JwtAccessTokenIssuer.class);

	private final JwtEncoder encoder;
	private final Clock clock;

	/**
	 * @param secret 서명 키. 설정 trealworld.jwt.secret에서 읽는다. 비어 있으면 임의의 키를 만든다
	 */
	JwtAccessTokenIssuer(@Value("${trealworld.jwt.secret:}") String secret, Clock clock) {
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
		this.clock = clock;
	}

	@Override
	public AccessToken issue(Member member) {
		if (member.getId() == null) {
			throw new IllegalArgumentException("저장하지 않은 회원에게는 토큰을 발급할 수 없다");
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(String.valueOf(member.getId()))
				.expiresAt(clock.instant().plus(VALIDITY))
				.build();
		return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue());
	}

	private static SecretKey key(String secret) {
		byte[] bytes;
		if (secret == null || secret.isBlank()) {
			logger.warn("trealworld.jwt.secret이 없다. 임의의 서명 키를 만든다. 서버를 다시 시작하면 발급한 토큰은 모두 무효가 된다.");
			bytes = new byte[MIN_SECRET_BYTES];
			new SecureRandom().nextBytes(bytes);
		}
		else {
			bytes = secret.getBytes(StandardCharsets.UTF_8);
			if (bytes.length < MIN_SECRET_BYTES) {
				throw new IllegalArgumentException("trealworld.jwt.secret은 UTF-8로 32바이트 이상이어야 한다");
			}
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

}
