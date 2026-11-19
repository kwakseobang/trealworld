package trealworld.acceptance;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * 수용 테스트가 직접 만드는 액세스 토큰. 서버의 설정(trealworld.jwt.secret)과 같은 키를 쓴다.
 */
final class TestTokens {

	static final String SECRET = "acceptance-test-secret-0123456789abcdef";

	private TestTokens() {
	}

	static String signed(String secret, String subject, Instant expiry) {
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
					new JWTClaimsSet.Builder().subject(subject).expirationTime(Date.from(expiry)).build());
			jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
			return jwt.serialize();
		}
		catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	/** 토큰의 서명이 이 키로 만든 것인가. 서버에 묻지 않고 확인한다. */
	static boolean isSignedWith(String secret, String token) {
		try {
			return SignedJWT.parse(token).verify(new MACVerifier(secret.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	/** 토큰의 본문에 있는 회원의 식별자. */
	static String subjectOf(String token) {
		try {
			return SignedJWT.parse(token).getJWTClaimsSet().getSubject();
		}
		catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

}
