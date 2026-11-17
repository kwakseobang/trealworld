package trealworld.application.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import trealworld.application.port.in.DuplicateMemberException;
import trealworld.application.port.in.InvalidValuesException;
import trealworld.application.port.in.RegisterMemberCommand;
import trealworld.application.port.in.RegisterMemberUseCase;
import trealworld.application.port.out.MemberNotUniqueException;
import trealworld.application.port.out.MemberRepository;
import trealworld.application.port.out.PasswordHasher;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.Password;
import trealworld.domain.Username;

/**
 * 가입을 처리한다. R4, R6, R7을 지키는 곳이다.
 */
public class RegisterMemberService implements RegisterMemberUseCase {

	private final MemberRepository members;
	private final PasswordHasher passwordHasher;

	public RegisterMemberService(MemberRepository members, PasswordHasher passwordHasher) {
		this.members = members;
		this.passwordHasher = passwordHasher;
	}

	@Override
	public Member register(RegisterMemberCommand command) {
		Map<String, String> errors = new LinkedHashMap<>();
		Username username = value("username", () -> new Username(command.username()), errors);
		Email email = value("email", () -> new Email(command.email()), errors);
		Password password = value("password", () -> new Password(command.password()), errors);
		if (!errors.isEmpty()) {
			throw new InvalidValuesException(errors);
		}

		rejectDuplicate(email, username);

		Member member = Member.register(username, email, passwordHasher.hash(password));
		try {
			return members.save(member);
		}
		catch (MemberNotUniqueException exception) {
			// 확인과 저장의 사이에 다른 가입이 같은 값을 저장했다. 어느 값인지 다시 묻는다.
			rejectDuplicate(email, username);
			// 중복이 아니다. 숨기지 않는다. 나가는 포트의 예외는 유스케이스의 밖으로 내보내지 않는다. (ADR-0002)
			throw new IllegalStateException("회원을 저장하지 못했다", exception);
		}
	}

	/**
	 * 이메일(R6)이나 사용자 이름(R7)이 이미 있으면 거부한다.
	 */
	private void rejectDuplicate(Email email, Username username) {
		if (members.existsByEmail(email)) {
			throw new DuplicateMemberException("email", "이미 있는 이메일이다");
		}
		if (members.existsByUsername(username)) {
			throw new DuplicateMemberException("username", "이미 있는 사용자 이름이다");
		}
	}

	/**
	 * 값 객체를 만든다. 규칙을 어기면 값의 이름과 이유를 errors에 적는다.
	 */
	private static <T> T value(String name, Supplier<T> constructor, Map<String, String> errors) {
		try {
			return constructor.get();
		}
		catch (IllegalArgumentException exception) {
			errors.put(name, exception.getMessage());
			return null;
		}
	}

}
