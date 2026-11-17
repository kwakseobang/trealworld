package trealworld.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Domain 01의 R4.
 */
class PasswordTests {

	@ParameterizedTest
	@MethodSource("passwordsAtTheLimits")
	void r4_password_at_the_limits_is_accepted(String value) {
		assertThat(new Password(value).value()).isEqualTo(value);
	}

	static Stream<String> passwordsAtTheLimits() {
		return Stream.of("12345678", "a".repeat(72), "가".repeat(24));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "1234567" })
	void r4_password_shorter_than_8_characters_is_rejected(String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> new Password(value));
	}

	@ParameterizedTest
	@MethodSource("passwordsOverTheByteLimit")
	void r4_password_longer_than_72_bytes_is_rejected(String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> new Password(value));
	}

	static Stream<String> passwordsOverTheByteLimit() {
		return Stream.of("a".repeat(73), "가".repeat(25));
	}

	/** 한 자는 코드 포인트 하나다. 😀는 한 자이고, UTF-16으로는 char 2개다. */
	@Test
	void r4_characters_are_counted_as_code_points() {
		assertThatNoException().isThrownBy(() -> new Password("😀".repeat(8)));
		assertThatIllegalArgumentException().isThrownBy(() -> new Password("😀".repeat(4)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "        ", "aaaaaaaa", "ㅋㅋㅋㅋㅋㅋㅋㅋ" })
	void r4_there_is_no_condition_on_the_kind_of_characters(String value) {
		assertThatNoException().isThrownBy(() -> new Password(value));
	}

	@Test
	void r4_password_is_not_changed() {
		assertThat(new Password("  Pass word  ").value()).isEqualTo("  Pass word  ");
	}

	@Test
	void password_does_not_show_its_value_as_text() {
		assertThat(new Password("correct-horse-battery").toString()).doesNotContain("correct-horse-battery");
	}

}
