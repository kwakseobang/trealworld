package trealworld.adapter.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰의 서명 키. 발급하는 쪽과 검사하는 쪽이 이 키 하나를 쓴다. (ADR-0003)
 */
@Component
class JwtSigningKey {

	/** HS256의 키는 256비트 이상이어야 한다. */
	private static final int MIN_SECRET_BYTES = 32;

	private static final Log logger = LogFactory.getLog(JwtSigningKey.class);

	private final SecretKey secretKey;

	/**
	 * @param secret 설정 trealworld.jwt.secret의 값. 비어 있으면 임의의 키를 만든다
	 */
	JwtSigningKey(@Value("${trealworld.jwt.secret:}") String secret) {
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
		this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
	}

	SecretKey secretKey() {
		return secretKey;
	}

}
