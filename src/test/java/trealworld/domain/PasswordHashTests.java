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
	void password_hash_keeps_its_value() {
		assertThat(new PasswordHash("$2a$10$abcdefghijklmnopqrstuv").value()).isEqualTo("$2a$10$abcdefghijklmnopqrstuv");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " " })
	void password_hash_that_is_empty_is_rejected(String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> new PasswordHash(value));
	}

}
