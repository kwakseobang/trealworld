package trealworld.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Domain 01의 R1, R5.
 */
class MemberTests {

	private static final Username USERNAME = new Username("jake");
	private static final Email EMAIL = new Email("jake@example.com");
	private static final PasswordHash PASSWORD_HASH = new PasswordHash("$2a$10$abcdefghijklmnopqrstuv");

	@Test
	void r1_member_has_username_email_and_password_hash() {
		Member member = Member.register(USERNAME, EMAIL, PASSWORD_HASH);

		assertThat(member.getUsername()).isEqualTo(USERNAME);
		assertThat(member.getEmail()).isEqualTo(EMAIL);
		assertThat(member.getPasswordHash()).isEqualTo(PASSWORD_HASH);
	}

	@Test
	void r1_member_without_username_is_rejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> Member.register(null, EMAIL, PASSWORD_HASH));
	}

	@Test
	void r1_member_without_email_is_rejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> Member.register(USERNAME, null, PASSWORD_HASH));
	}

	@Test
	void r1_member_without_password_hash_is_rejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> Member.register(USERNAME, EMAIL, null));
	}

	/**
	 * 회원에는 글자를 그대로 담는 필드가 없다. 비밀번호의 원문을 둘 자리가 없다.
	 */
	@Test
	void r5_member_has_no_field_that_can_hold_a_raw_password() {
		assertThat(Arrays.stream(Member.class.getDeclaredFields()).map(Field::getType))
				.isNotEmpty()
				.noneMatch(type -> CharSequence.class.isAssignableFrom(type) || type == char[].class
						|| type == byte[].class);
	}

	@Test
	void r5_member_keeps_the_password_only_as_a_password_hash() {
		assertThat(Arrays.stream(Member.class.getDeclaredFields()).filter(field -> field.getType() == PasswordHash.class))
				.hasSize(1);
	}

}
