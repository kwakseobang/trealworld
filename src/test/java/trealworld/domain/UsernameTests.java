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
	@ValueSource(strings = { "", " ", "\t\n" })
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
	void usernames_that_differ_in_case_are_different() {
		assertThat(new Username("Jake")).isNotEqualTo(new Username("jake"));
	}

}
