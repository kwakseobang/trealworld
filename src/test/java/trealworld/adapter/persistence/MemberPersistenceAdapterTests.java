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

	private static Member member(String username, String email) {
		return Member.register(new Username(username), new Email(email), new PasswordHash("hash"));
	}

}
