package trealworld.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Domain 01의 R2.
 */
class EmailTests {

	@Test
	void r2_email_with_a_valid_format_is_accepted() {
		assertThat(new Email("jake@example.com").value()).isEqualTo("jake@example.com");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "jake", "jake@", "@example.com", "jake@example", "jake@@example.com",
			"ja ke@example.com", " jake@example.com" })
	void r2_email_with_an_invalid_format_is_rejected(String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> new Email(value));
	}

	@Test
	void r2_email_is_stored_in_lower_case() {
		assertThat(new Email("Jake@Example.COM").value()).isEqualTo("jake@example.com");
	}

	@Test
	void r2_emails_that_differ_only_in_case_are_equal() {
		assertThat(new Email("JAKE@example.com")).isEqualTo(new Email("jake@EXAMPLE.com"));
	}

	@Test
	void r2_email_of_254_characters_is_accepted() {
		String email = "a".repeat(254 - "@example.com".length()) + "@example.com";

		assertThat(new Email(email).value()).hasSize(254);
	}

	@Test
	void r2_email_of_255_characters_is_rejected() {
		String email = "a".repeat(255 - "@example.com".length()) + "@example.com";

		assertThatIllegalArgumentException().isThrownBy(() -> new Email(email));
	}

}
