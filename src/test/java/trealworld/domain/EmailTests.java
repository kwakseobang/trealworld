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
			"ja ke@example.com", " jake@example.com", "jake@example..com", "jake@.example.com", "jake@example.com.",
			"jake@exam\u3000ple.com", "ja\u00A0ke@example.com" })
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

	/** 길이는 저장하는 값(소문자)으로 센다. İ(U+0130)는 소문자로 바꾸면 코드 포인트 2개가 된다. */
	@Test
	void r2_the_length_is_counted_on_the_stored_value() {
		String email = "\u0130" + "a".repeat(253 - "@example.com".length()) + "@example.com";

		assertThat(email.codePointCount(0, email.length())).isEqualTo(254);
		assertThatIllegalArgumentException().isThrownBy(() -> new Email(email));
	}

	/** 저장한 값으로 다시 만들면 같은 이메일이다. */
	@Test
	void r2_a_stored_value_makes_the_same_email_again() {
		Email email = new Email("\u0130stanbul@Example.com");

		assertThat(new Email(email.value())).isEqualTo(email);
	}

}
