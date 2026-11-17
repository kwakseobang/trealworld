package trealworld.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import trealworld.application.port.in.MemberNotFoundException;
import trealworld.application.port.out.MemberRepository;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.PasswordHash;
import trealworld.domain.Username;

/**
 * Domain 01의 R10. "조회"가 지키는 규칙이다. Spring 없이 돈다.
 */
class GetMemberServiceTests {

	private final FakeMemberRepository members = new FakeMemberRepository();
	private final GetMemberService service = new GetMemberService(members);

	private final Member jake = members.add(7L, "jake", "jake@example.com");
	private final Member finn = members.add(8L, "finn", "finn@example.com");

	@Test
	void ac3_1_the_member_with_the_given_id_is_returned() {
		assertThat(service.getMember(7L)).isSameAs(jake);
	}

	/** 조회의 대상은 받은 식별자의 회원 하나다. 다른 회원을 찾는 방법이 없다. */
	@Test
	void r10_only_the_member_with_the_given_id_is_returned() {
		assertThat(service.getMember(8L)).isSameAs(finn);
		assertThat(members.lookedUp).containsExactly(8L);
	}

	@Test
	void ac3_5_a_member_that_does_not_exist_is_reported() {
		assertThatExceptionOfType(MemberNotFoundException.class).isThrownBy(() -> service.getMember(9L));
	}

	@Test
	void ac3_5_a_missing_id_is_reported_as_a_member_that_does_not_exist() {
		assertThatExceptionOfType(MemberNotFoundException.class).isThrownBy(() -> service.getMember(null));
		assertThat(members.lookedUp).isEmpty();
	}

	private static class FakeMemberRepository implements MemberRepository {

		private final Map<Long, Member> saved = new LinkedHashMap<>();
		private final java.util.List<Long> lookedUp = new java.util.ArrayList<>();

		Member add(Long id, String username, String email) {
			Member member = Member.register(new Username(username), new Email(email), new PasswordHash("hash"));
			saved.put(id, member);
			return member;
		}

		@Override
		public Optional<Member> findById(Long id) {
			lookedUp.add(id);
			return Optional.ofNullable(saved.get(id));
		}

		@Override
		public Member save(Member member) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Member> findByEmail(Email email) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean existsByEmail(Email email) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean existsByUsername(Username username) {
			throw new UnsupportedOperationException();
		}

	}

}
