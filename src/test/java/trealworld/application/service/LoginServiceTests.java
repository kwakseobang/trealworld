package trealworld.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import trealworld.application.port.in.AccessToken;
import trealworld.application.port.in.InvalidCredentialsException;
import trealworld.application.port.in.InvalidValuesException;
import trealworld.application.port.in.LoginCommand;
import trealworld.application.port.out.AccessTokenIssuer;
import trealworld.application.port.out.MemberRepository;
import trealworld.application.port.out.PasswordHasher;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.Password;
import trealworld.domain.PasswordHash;
import trealworld.domain.Username;

/**
 * Domain 01의 R8, R9. "로그인"이 지키는 규칙이다. Spring 없이 돈다.
 */
class LoginServiceTests {

	private static final String PASSWORD = "correct-horse-battery";

	private final FakeMemberRepository members = new FakeMemberRepository();
	private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
	private final FakeAccessTokenIssuer accessTokenIssuer = new FakeAccessTokenIssuer();
	private final LoginService service = new LoginService(members, passwordHasher, accessTokenIssuer);

	private final Member jake = members.add("jake", "jake@example.com", PASSWORD);
	private final Member finn = members.add("finn", "finn@example.com", "another-password");

	@Test
	void r8_login_finds_the_member_by_email_and_issues_a_token_for_that_member() {
		AccessToken token = service.login(new LoginCommand("jake@example.com", PASSWORD));

		assertThat(token).isEqualTo(new AccessToken("token-of-jake"));
		assertThat(accessTokenIssuer.issuedTo).containsExactly(jake);
	}

	@Test
	void r8_login_checks_the_password_against_the_hash_of_that_member() {
		assertThatExceptionOfType(InvalidCredentialsException.class)
				.isThrownBy(() -> service.login(new LoginCommand("jake@example.com", "another-password")));

		assertThat(service.login(new LoginCommand("finn@example.com", "another-password")))
				.isEqualTo(new AccessToken("token-of-finn"));
		assertThat(accessTokenIssuer.issuedTo).containsExactly(finn);
	}

	/** Domain R2: 이메일은 소문자로 바꾼다. 그래서 대소문자가 달라도 찾는다. (AC-2.5) */
	@Test
	void r8_login_finds_the_member_by_an_email_in_another_case() {
		assertThat(service.login(new LoginCommand("JAKE@Example.com", PASSWORD)))
				.isEqualTo(new AccessToken("token-of-jake"));
	}

	@Test
	void r9_an_unknown_email_and_a_wrong_password_fail_in_the_same_way() {
		InvalidCredentialsException unknownEmail = catchThrowableOfType(InvalidCredentialsException.class,
				() -> service.login(new LoginCommand("nobody@example.com", PASSWORD)));
		InvalidCredentialsException wrongPassword = catchThrowableOfType(InvalidCredentialsException.class,
				() -> service.login(new LoginCommand("jake@example.com", "wrong-horse-battery")));

		assertThat(unknownEmail).isNotNull();
		assertThat(wrongPassword).isNotNull();
		assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
		assertThat(unknownEmail.getCause()).isNull();
		assertThat(wrongPassword.getCause()).isNull();
		assertThat(accessTokenIssuer.issuedTo).isEmpty();
	}

	/** 비밀번호(R4)가 될 수 없는 값은 틀린 비밀번호다. 해시와 비교하지 않는다. */
	@ParameterizedTest
	@ValueSource(strings = { "short", "   ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" })
	void r9_a_password_that_nobody_can_have_fails_in_the_same_way(String password) {
		assertThatExceptionOfType(InvalidCredentialsException.class)
				.isThrownBy(() -> service.login(new LoginCommand("jake@example.com", password)))
				.withMessage(new InvalidCredentialsException().getMessage());

		assertThat(passwordHasher.matched).isEmpty();
	}

	/** AC-2.4: 값의 오류는 로그인의 실패와 다른 예외다. */
	@ParameterizedTest
	@NullAndEmptySource
	void ac2_4_an_empty_password_is_an_invalid_value(String password) {
		InvalidValuesException exception = catchThrowableOfType(InvalidValuesException.class,
				() -> service.login(new LoginCommand("jake@example.com", password)));

		assertThat(exception.getErrors()).containsOnlyKeys("password");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "jake", "jake@example" })
	void ac2_4_an_empty_email_or_an_email_with_an_invalid_format_is_an_invalid_value(String email) {
		InvalidValuesException exception = catchThrowableOfType(InvalidValuesException.class,
				() -> service.login(new LoginCommand(email, PASSWORD)));

		assertThat(exception.getErrors()).containsOnlyKeys("email");
	}

	@Test
	void ac2_4_every_invalid_value_is_named() {
		InvalidValuesException exception = catchThrowableOfType(InvalidValuesException.class,
				() -> service.login(new LoginCommand(null, null)));

		assertThat(exception.getErrors().keySet()).containsExactly("email", "password");
	}

	/** 값이 틀리면 저장소에 묻지 않는다. */
	@Test
	void ac2_4_invalid_values_are_reported_before_the_member_is_looked_up() {
		assertThatExceptionOfType(InvalidValuesException.class)
				.isThrownBy(() -> service.login(new LoginCommand("jake@example.com", "")));

		assertThat(members.lookedUp).isEmpty();
	}

	private static class FakeMemberRepository implements MemberRepository {

		private final List<Member> saved = new ArrayList<>();
		private final List<Email> lookedUp = new ArrayList<>();

		Member add(String username, String email, String password) {
			return save(Member.register(new Username(username), new Email(email), FakePasswordHasher.hashOf(password)));
		}

		@Override
		public Member save(Member member) {
			saved.add(member);
			return member;
		}

		@Override
		public Optional<Member> findById(UUID id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Member> findByEmail(Email email) {
			lookedUp.add(email);
			return saved.stream().filter(member -> member.getEmail().equals(email)).findFirst();
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

	private static class FakePasswordHasher implements PasswordHasher {

		private final List<String> matched = new ArrayList<>();

		static PasswordHash hashOf(String password) {
			return new PasswordHash("hash-of-" + password);
		}

		@Override
		public PasswordHash hash(Password password) {
			return hashOf(password.value());
		}

		@Override
		public boolean matches(Password password, PasswordHash hash) {
			matched.add(password.value());
			return hash(password).equals(hash);
		}

	}

	private static class FakeAccessTokenIssuer implements AccessTokenIssuer {

		private final List<Member> issuedTo = new ArrayList<>();

		@Override
		public AccessToken issue(Member member) {
			issuedTo.add(member);
			return new AccessToken("token-of-" + member.getUsername().value());
		}

	}

}
