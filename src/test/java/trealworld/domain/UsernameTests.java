package trealworld.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Domain 01의 R3.
 */
class UsernameTests {

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "\t\n", "\u00A0", "\u3000", " \u00A0\u2003" })
	void r3_username_that_is_empty_is_rejected(String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> new Username(value));
	}

	@Test
	void r3_username_is_stored_without_surrounding_whitespace() {
		assertThat(new Username("  jake \t").value()).isEqualTo("jake");
	}

	@Test
	void r3_whitespace_inside_a_username_is_kept() {
		assertThat(new Username("jake the dog").value()).isEqualTo("jake the dog");
	}

	@Test
	void r7_usernames_that_differ_in_case_are_different() {
		assertThat(new Username("Jake")).isNotEqualTo(new Username("jake"));
	}

	@Test
	void r3_username_of_50_characters_is_accepted() {
		assertThat(new Username("a".repeat(50)).value()).hasSize(50);
	}

	@Test
	void r3_username_of_51_characters_is_rejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Username("a".repeat(51)));
	}

	/** 길이는 앞뒤 공백을 지운 뒤에 센다. */
	@Test
	void r3_the_length_is_counted_after_the_surrounding_whitespace_is_removed() {
		assertThat(new Username("  " + "a".repeat(50) + "  ").value()).hasSize(50);
	}

	/** 한 자는 코드 포인트 하나다. 이모지 하나는 한 자다. */
	@Test
	void r3_characters_are_counted_as_code_points() {
		assertThat(new Username("😀".repeat(50)).value()).isEqualTo("😀".repeat(50));
		assertThatIllegalArgumentException().isThrownBy(() -> new Username("😀".repeat(51)));
	}

	/** 공백은 유니코드의 공백이다. 줄을 바꾸지 않는 공백(U+00A0)과 전각 공백(U+3000)도 지운다. */
	@Test
	void r3_unicode_whitespace_around_a_username_is_removed() {
		assertThat(new Username("\u00A0jake\u3000").value()).isEqualTo("jake");
		assertThat(new Username("\u00A0jake")).isEqualTo(new Username("jake"));
	}

}
