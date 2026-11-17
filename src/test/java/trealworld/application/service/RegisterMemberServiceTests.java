package trealworld.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import trealworld.application.port.in.DuplicateMemberException;
import trealworld.application.port.in.InvalidValuesException;
import trealworld.application.port.in.RegisterMemberCommand;
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
	void registering_saves_a_member_and_returns_it() {
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
	void every_invalid_value_is_named() {
		InvalidValuesException exception = catchThrowableOfType(InvalidValuesException.class,
				() -> service.register(new RegisterMemberCommand(" ", "jake@", null)));

		assertThat(exception.getErrors().keySet()).containsExactly("username", "email", "password");
	}

	/** 값의 오류가 먼저다. 값이 틀리면 저장소에 묻지 않는다. */
	@Test
	void invalid_values_are_reported_before_duplicates() {
		service.register(new RegisterMemberCommand("jake", "jake@example.com", PASSWORD));

		assertThatExceptionOfType(InvalidValuesException.class)
				.isThrownBy(() -> service.register(new RegisterMemberCommand("jake", "jake@example.com", "short")));
	}

	private static class FakeMemberRepository implements MemberRepository {

		private final List<Member> saved = new ArrayList<>();

		@Override
		public Member save(Member member) {
			saved.add(member);
			return member;
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

	}

}
