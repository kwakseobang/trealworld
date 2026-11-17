package trealworld.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * 서명 키. (ADR-0003)
 */
class JwtSigningKeyTests {

	@Test
	void adr0003_the_key_is_made_from_the_secret() {
		assertThat(new JwtSigningKey("0123456789abcdef0123456789abcdef").secretKey().getEncoded())
				.isEqualTo("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void adr0003_a_secret_shorter_than_32_bytes_is_rejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtSigningKey("too-short"));
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtSigningKey("0123456789abcdef0123456789abcde"));
	}

	/** 키가 없으면 임의의 키를 만든다. 인스턴스마다 키가 다르다. */
	@Test
	void adr0003_without_a_secret_a_random_key_of_32_bytes_is_used() {
		JwtSigningKey first = new JwtSigningKey("");
		JwtSigningKey second = new JwtSigningKey(null);

		assertThat(first.secretKey().getEncoded()).hasSize(32).isNotEqualTo(second.secretKey().getEncoded());
	}

}
