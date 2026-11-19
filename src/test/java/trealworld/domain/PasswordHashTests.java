package trealworld.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 비밀번호 해시는 자기의 형식 규칙을 스스로 검사한다 (Domain 01의 모델).
 */
class PasswordHashTests {

	@Test
	void r1_password_hash_keeps_its_value() {
		assertThat(new PasswordHash("$2a$10$abcdefghijklmnopqrstuv").value()).isEqualTo("$2a$10$abcdefghijklmnopqrstuv");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " " })
	void r1_password_hash_that_is_empty_is_rejected(String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> new PasswordHash(value));
	}

	/** 해시가 로그와 단언의 글에 남지 않게 한다. */
	@Test
	void r5_password_hash_does_not_show_its_value_as_text() {
		assertThat(new PasswordHash("$2a$10$abcdefghijklmnopqrstuv").toString()).doesNotContain("abcdefghijklmnopqrstuv");
	}

}
