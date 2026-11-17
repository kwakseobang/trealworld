package trealworld.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import trealworld.domain.Password;
import trealworld.domain.PasswordHash;

/**
 * 보안 어댑터가 나가는 포트(PasswordHasher)의 약속을 지키는지 확인한다.
 */
class BCryptPasswordHasherTests {

	private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

	@Test
	void r5_the_hash_is_not_the_raw_password() {
		assertThat(hasher.hash(new Password("correct-horse-battery")).value()).doesNotContain("correct-horse-battery");
	}

	@Test
	void r8_the_password_matches_its_own_hash() {
		PasswordHash hash = hasher.hash(new Password("correct-horse-battery"));

		assertThat(hasher.matches(new Password("correct-horse-battery"), hash)).isTrue();
	}

	@Test
	void r8_another_password_does_not_match_the_hash() {
		PasswordHash hash = hasher.hash(new Password("correct-horse-battery"));

		assertThat(hasher.matches(new Password("wrong-horse-battery"), hash)).isFalse();
		assertThat(hasher.matches(new Password("Correct-horse-battery"), hash)).isFalse();
	}

	/** 같은 비밀번호도 해시는 매번 다르다 (salt). 그래서 해시끼리 비교하면 안 된다. matches를 쓴다. */
	@Test
	void r8_two_hashes_of_the_same_password_differ_and_both_match() {
		Password password = new Password("correct-horse-battery");
		PasswordHash first = hasher.hash(password);
		PasswordHash second = hasher.hash(password);

		assertThat(first).isNotEqualTo(second);
		assertThat(hasher.matches(password, first)).isTrue();
		assertThat(hasher.matches(password, second)).isTrue();
	}

	/** 저장된 값이 BCrypt의 해시가 아니면 맞지 않는다. 예외를 던지지 않는다. */
	@Test
	void r8_a_value_that_is_not_a_bcrypt_hash_does_not_match() {
		assertThat(hasher.matches(new Password("correct-horse-battery"), new PasswordHash("correct-horse-battery")))
				.isFalse();
	}

	/** 한계의 길이(72바이트)의 비밀번호도 해시하고 확인한다. (R4) */
	@Test
	void r4_a_password_of_72_bytes_is_hashed_and_matched() {
		Password longest = new Password("가".repeat(24));

		assertThat(hasher.matches(longest, hasher.hash(longest))).isTrue();
		assertThat(hasher.matches(new Password("가".repeat(23) + "나"), hasher.hash(longest))).isFalse();
	}

}
