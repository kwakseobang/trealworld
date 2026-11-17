package trealworld.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import trealworld.application.port.out.MemberNotUniqueException;
import trealworld.application.port.out.MemberRepository;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.PasswordHash;
import trealworld.domain.Username;

/**
 * 영속성 어댑터가 나가는 포트(MemberRepository)의 약속을 지키는지 확인한다.
 * R6, R7의 마지막 방어선은 DB의 unique 제약이다. 유스케이스의 확인을 거치지 않고 저장한다.
 */
@SpringBootTest
class MemberPersistenceAdapterTests {

	@Autowired
	private MemberRepository members;

	private final String id = UUID.randomUUID().toString().substring(0, 8);

	@Test
	void r6_saving_an_email_that_exists_is_rejected() {
		members.save(member("jake-" + id, "jake-" + id + "@example.com"));

		assertThatExceptionOfType(MemberNotUniqueException.class)
				.isThrownBy(() -> members.save(member("finn-" + id, "jake-" + id + "@example.com")));
	}

	@Test
	void r7_saving_a_username_that_exists_is_rejected() {
		members.save(member("jake-" + id, "jake-" + id + "@example.com"));

		assertThatExceptionOfType(MemberNotUniqueException.class)
				.isThrownBy(() -> members.save(member("jake-" + id, "finn-" + id + "@example.com")));
	}

	/** Spec 01의 가정 2: 사용자 이름은 대소문자를 구분한다. DB도 구분한다. */
	@Test
	void r7_saving_a_username_that_differs_in_case_is_accepted() {
		members.save(member("jake-" + id, "jake-" + id + "@example.com"));
		Member saved = members.save(member("JAKE-" + id, "finn-" + id + "@example.com"));

		assertThat(saved.getId()).isNotNull();
		assertThat(members.existsByUsername(new Username("JAKE-" + id))).isTrue();
		assertThat(members.existsByUsername(new Username("Jake-" + id))).isFalse();
	}

	/** R8: 로그인은 이메일로 회원을 찾는다. */
	@Test
	void r8_a_saved_member_is_found_by_email() {
		Member saved = members.save(member("jake-" + id, "jake-" + id + "@example.com"));

		assertThat(members.findByEmail(new Email("jake-" + id + "@example.com")))
				.hasValueSatisfying(found -> {
					assertThat(found.getId()).isEqualTo(saved.getId());
					assertThat(found.getUsername()).isEqualTo(new Username("jake-" + id));
					assertThat(found.getPasswordHash()).isEqualTo(new PasswordHash("hash"));
				});
	}

	/** 이메일(R2)은 소문자로 바뀐다. 그래서 대소문자가 다른 이메일로도 찾는다. (AC-2.5) */
	@Test
	void r8_a_saved_member_is_found_by_an_email_in_another_case() {
		members.save(member("jake-" + id, "Jake-" + id + "@Example.com"));

		assertThat(members.findByEmail(new Email("JAKE-" + id + "@example.COM"))).isPresent();
	}

	@Test
	void r8_an_email_that_is_not_saved_is_not_found() {
		assertThat(members.findByEmail(new Email("nobody-" + id + "@example.com"))).isEmpty();
	}

	/** 조회는 식별자로 회원을 찾는다. (AC-3.1) */
	@Test
	void ac3_1_a_saved_member_is_found_by_id() {
		Member saved = members.save(member("jake-" + id, "jake-" + id + "@example.com"));

		assertThat(members.findById(saved.getId())).hasValueSatisfying(
				found -> assertThat(found.getEmail()).isEqualTo(new Email("jake-" + id + "@example.com")));
	}

	@Test
	void ac3_5_an_id_that_is_not_saved_is_not_found() {
		assertThat(members.findById(Long.MAX_VALUE)).isEmpty();
	}

	private static Member member(String username, String email) {
		return Member.register(new Username(username), new Email(email), new PasswordHash("hash"));
	}

	/** R2, R3의 가장 긴 값을 DB가 받아들인다. */
	@Test
	void r2_r3_the_longest_username_and_email_are_saved() {
		String username = "가".repeat(50 - id.length()) + id;
		String email = "a".repeat(254 - ("-" + id + "@example.com").length()) + "-" + id + "@example.com";

		Member saved = members.save(member(username, email));

		assertThat(members.findByEmail(new Email(email))).hasValueSatisfying(
				found -> assertThat(found.getUsername()).isEqualTo(saved.getUsername()));
	}

}
