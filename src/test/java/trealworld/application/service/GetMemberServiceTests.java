package trealworld.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;
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

	private static final UUID JAKE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
	private static final UUID FINN_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

	private final FakeMemberRepository members = new FakeMemberRepository();
	private final GetMemberService service = new GetMemberService(members);

	private final Member jake = members.add(JAKE_ID, "jake", "jake@example.com");
	private final Member finn = members.add(FINN_ID, "finn", "finn@example.com");

	@Test
	void ac3_1_the_member_with_the_given_id_is_returned() {
		assertThat(service.getMember(JAKE_ID)).isSameAs(jake);
	}

	/** 조회의 대상은 받은 식별자의 회원 하나다. 다른 회원을 찾는 방법이 없다. */
	@Test
	void r10_only_the_member_with_the_given_id_is_returned() {
		assertThat(service.getMember(FINN_ID)).isSameAs(finn);
		assertThat(members.lookedUp).containsExactly(FINN_ID);
	}

	@Test
	void ac3_5_a_member_that_does_not_exist_is_reported() {
		assertThatExceptionOfType(MemberNotFoundException.class).isThrownBy(() -> service.getMember(UUID.fromString("99999999-9999-4999-8999-999999999999")));
	}

	@Test
	void ac3_5_a_missing_id_is_reported_as_a_member_that_does_not_exist() {
		assertThatExceptionOfType(MemberNotFoundException.class).isThrownBy(() -> service.getMember(null));
		assertThat(members.lookedUp).isEmpty();
	}

	private static class FakeMemberRepository implements MemberRepository {

		private final Map<UUID, Member> saved = new LinkedHashMap<>();
		private final java.util.List<UUID> lookedUp = new java.util.ArrayList<>();

		Member add(UUID id, String username, String email) {
			Member member = Member.register(new Username(username), new Email(email), new PasswordHash("hash"));
			saved.put(id, member);
			return member;
		}

		@Override
		public Optional<Member> findById(UUID id) {
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
