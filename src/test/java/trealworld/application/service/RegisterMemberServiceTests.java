package trealworld.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import trealworld.application.port.in.DuplicateMemberException;
import trealworld.application.port.in.InvalidValuesException;
import trealworld.application.port.in.RegisterMemberCommand;
import trealworld.application.port.out.MemberNotUniqueException;
import trealworld.application.port.out.MemberRepository;
import trealworld.application.port.out.PasswordHasher;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.Password;
import trealworld.domain.PasswordHash;
import trealworld.domain.Username;

/**
 * Domain 01의 R4, R5, R6, R7. "가입"이 지키는 규칙이다. Spring 없이 돈다.
 */
class RegisterMemberServiceTests {

	private static final String PASSWORD = "correct-horse-battery";

	private final FakeMemberRepository members = new FakeMemberRepository();
	private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
	private final RegisterMemberService service = new RegisterMemberService(members, passwordHasher);

	@Test
	void ac1_1_registering_saves_a_member_and_returns_it() {
		Member member = service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));

		assertThat(member.getUsername()).isEqualTo(new Username("jake"));
		assertThat(member.getEmail()).isEqualTo(new Email("jake@example.com"));
		assertThat(members.saved).containsExactly(member);
	}

	@Test
	void r4_registering_with_an_invalid_password_is_rejected_and_nothing_is_saved() {
		InvalidValuesException exception = catchThrowableOfType(InvalidValuesException.class,
				() -> service.register(new RegisterMemberCommand("jake", "jake@example.com", "1234567")));

		assertThat(exception.getErrors()).containsOnlyKeys("password");
		assertThat(members.saved).isEmpty();
		assertThat(passwordHasher.hashed).isEmpty();
	}

	@Test
	void r5_the_saved_member_has_the_hash_and_not_the_raw_password() {
		Member member = service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));

		assertThat(passwordHasher.hashed).containsExactly(PASSWORD);
		assertThat(member.getPasswordHash()).isEqualTo(new PasswordHash("hash-of-" + PASSWORD.length()));
		assertThat(member.getPasswordHash().value()).doesNotContain(PASSWORD);
	}

	@Test
	void r6_registering_with_an_email_that_exists_is_rejected() {
		service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));

		DuplicateMemberException exception = catchThrowableOfType(DuplicateMemberException.class,
				() -> service.register(new RegisterMemberCommand("finn", "jake@example.com", PASSWORD)));

		assertThat(exception.getField()).isEqualTo("email");
		assertThat(members.saved).hasSize(1);
	}

	@Test
	void r6_an_email_that_differs_only_in_case_exists() {
		service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));

		assertThatExceptionOfType(DuplicateMemberException.class)
				.isThrownBy(() -> service.register(new RegisterMemberCommand("finn", "JAKE@Example.com", PASSWORD)));
	}

	@Test
	void r7_registering_with_a_username_that_exists_is_rejected() {
		service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));

		DuplicateMemberException exception = catchThrowableOfType(DuplicateMemberException.class,
				() -> service.register(new RegisterMemberCommand("jake", "finn@example.com", PASSWORD)));

		assertThat(exception.getField()).isEqualTo("username");
		assertThat(members.saved).hasSize(1);
	}

	/** Spec 01의 가정 2: 사용자 이름은 대소문자를 구분한다. */
	@Test
	void r7_a_username_that_differs_in_case_does_not_exist() {
		service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));
		service.register(new RegisterMemberCommand("Jake", "finn@example.com", PASSWORD));

		assertThat(members.saved).hasSize(2);
	}

	@Test
	void ac1_2_every_invalid_value_is_named() {
		InvalidValuesException exception = catchThrowableOfType(InvalidValuesException.class,
				() -> service.register(new RegisterMemberCommand(" ", "jake@", null)));

		assertThat(exception.getErrors().keySet()).containsExactly("username", "email", "password");
	}

	/** 값의 오류가 먼저다. 값이 틀리면 저장소에 묻지 않는다. */
	@Test
	void ac1_4_invalid_values_are_reported_before_duplicates() {
		service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));

		assertThatExceptionOfType(InvalidValuesException.class)
				.isThrownBy(() -> service.register(new RegisterMemberCommand("jake", "jake@example.com", "short")));
	}

	/** 확인과 저장의 사이에 다른 가입이 같은 이메일을 저장했다. */
	@Test
	void r6_an_email_saved_by_another_registration_before_the_save_is_rejected() {
		members.savedByAnotherRegistration = Member.register(new Username("finn"), new Email("jake@example.com"),
				new PasswordHash("hash"));

		DuplicateMemberException exception = catchThrowableOfType(DuplicateMemberException.class,
				() -> service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD)));

		assertThat(exception.getField()).isEqualTo("email");
	}

	/** 확인과 저장의 사이에 다른 가입이 같은 사용자 이름을 저장했다. */
	@Test
	void r7_a_username_saved_by_another_registration_before_the_save_is_rejected() {
		members.savedByAnotherRegistration = Member.register(new Username("jake"), new Email("finn@example.com"),
				new PasswordHash("hash"));

		DuplicateMemberException exception = catchThrowableOfType(DuplicateMemberException.class,
				() -> service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD)));

		assertThat(exception.getField()).isEqualTo("username");
	}

	/**
	 * 저장소가 거부했지만 이메일도 사용자 이름도 없다. 중복이 아니다. 숨기지 않는다.
	 * 나가는 포트의 예외는 유스케이스의 밖으로 나가지 않는다. (ADR-0002)
	 */
	@Test
	void r6_r7_a_rejected_save_that_is_not_a_duplicate_is_not_reported_as_a_duplicate() {
		members.rejectsNextSave = true;

		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD)))
				.withCauseInstanceOf(MemberNotUniqueException.class);
	}

	private static class FakeMemberRepository implements MemberRepository {

		private final List<Member> saved = new ArrayList<>();

		/** 다음의 save()의 직전에 다른 가입이 저장하는 회원. */
		private Member savedByAnotherRegistration;

		/** 다음의 save()를 이유 없이 거부한다. */
		private boolean rejectsNextSave;

		@Override
		public Member save(Member member) {
			if (savedByAnotherRegistration != null) {
				saved.add(savedByAnotherRegistration);
				savedByAnotherRegistration = null;
			}
			if (rejectsNextSave || existsByEmail(member.getEmail()) || existsByUsername(member.getUsername())) {
				throw new MemberNotUniqueException(null);
			}
			saved.add(member);
			return member;
		}

		@Override
		public Optional<Member> findById(UUID id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Member> findByEmail(Email email) {
			return saved.stream().filter(member -> member.getEmail().equals(email)).findFirst();
		}

		@Override
		public boolean existsByEmail(Email email) {
			return saved.stream().anyMatch(member -> member.getEmail().equals(email));
		}

		@Override
		public boolean existsByUsername(Username username) {
			return saved.stream().anyMatch(member -> member.getUsername().equals(username));
		}

	}

	private static class FakePasswordHasher implements PasswordHasher {

		private final List<String> hashed = new ArrayList<>();

		@Override
		public PasswordHash hash(Password password) {
			hashed.add(password.value());
			return new PasswordHash("hash-of-" + password.value().length());
		}

		@Override
		public boolean matches(Password password, PasswordHash hash) {
			return hash(password).equals(hash);
		}

	}

	/** 중복은 해시를 만들기 전에 거부한다. 해시(BCrypt)는 비싸다. 저장도 시도하지 않는다. */
	@Test
	void r6_r7_a_duplicate_is_rejected_before_the_password_is_hashed() {
		service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));

		assertThatExceptionOfType(DuplicateMemberException.class).isThrownBy(
				() -> service.register(new RegisterMemberCommand("finn", "jake@example.com", "another-horse-battery")));
		assertThatExceptionOfType(DuplicateMemberException.class).isThrownBy(
				() -> service.register(new RegisterMemberCommand("jake", "finn@example.com", "another-horse-battery")));

		assertThat(passwordHasher.hashed).containsExactly(PASSWORD);
	}

}
