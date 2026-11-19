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
		JwtSigningKey second = new JwtSigningKey("");
		JwtSigningKey third = new JwtSigningKey(null);

		assertThat(first.secretKey().getEncoded()).hasSize(32).isNotEqualTo(second.secretKey().getEncoded());
		assertThat(third.secretKey().getEncoded()).hasSize(32).isNotEqualTo(first.secretKey().getEncoded());
	}

	/** 공백만 있는 값은 "없다"가 아니다. 잘못 설정한 키다. 임의의 키로 바꾸지 않는다. */
	@Test
	void adr0003_a_secret_of_only_whitespace_is_rejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtSigningKey(" "));
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtSigningKey(" ".repeat(40)));
	}

	/** 길이는 UTF-8의 바이트로 센다. "가"는 3바이트다. */
	@Test
	void adr0003_the_length_of_the_secret_is_counted_in_utf8_bytes() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtSigningKey("가".repeat(10) + "a"));
		assertThat(new JwtSigningKey("가".repeat(10) + "ab").secretKey().getEncoded()).hasSize(32);
	}

}
