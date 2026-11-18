package trealworld.adapter.security;

import java.time.Clock;
import java.time.Duration;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
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

	private final JwtEncoder encoder;
	private final Clock clock;

	JwtAccessTokenIssuer(JwtSigningKey key, Clock clock) {
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key.secretKey()));
		this.clock = clock;
	}

	@Override
	public AccessToken issue(Member member) {
		if (member.getId() == null) {
			throw new IllegalArgumentException("저장하지 않은 회원에게는 토큰을 발급할 수 없다");
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(member.getId().toString())
				.expiresAt(clock.instant().plus(VALIDITY))
				.build();
		return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue());
	}

}
