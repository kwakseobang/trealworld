package trealworld.application.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import trealworld.application.port.in.DuplicateMemberException;
import trealworld.application.port.in.InvalidValuesException;
import trealworld.application.port.in.RegisterMemberCommand;
import trealworld.application.port.in.RegisterMemberUseCase;
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

		if (members.existsByEmail(email)) {
			throw new DuplicateMemberException("email", "이미 있는 이메일이다");
		}
		if (members.existsByUsername(username)) {
			throw new DuplicateMemberException("username", "이미 있는 사용자 이름이다");
		}

		return members.save(Member.register(username, email, passwordHasher.hash(password)));
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
