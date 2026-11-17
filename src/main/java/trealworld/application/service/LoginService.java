package trealworld.application.service;

import java.util.LinkedHashMap;
import java.util.Map;

import trealworld.application.port.in.AccessToken;
import trealworld.application.port.in.InvalidCredentialsException;
import trealworld.application.port.in.InvalidValuesException;
import trealworld.application.port.in.LoginCommand;
import trealworld.application.port.in.LoginUseCase;
import trealworld.application.port.out.AccessTokenIssuer;
import trealworld.application.port.out.MemberRepository;
import trealworld.application.port.out.PasswordHasher;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.Password;

/**
 * 로그인을 처리한다. R8, R9를 지키는 곳이다.
 */
public class LoginService implements LoginUseCase {

	private final MemberRepository members;
	private final PasswordHasher passwordHasher;
	private final AccessTokenIssuer accessTokenIssuer;

	public LoginService(MemberRepository members, PasswordHasher passwordHasher, AccessTokenIssuer accessTokenIssuer) {
		this.members = members;
		this.passwordHasher = passwordHasher;
		this.accessTokenIssuer = accessTokenIssuer;
	}

	@Override
	public AccessToken login(LoginCommand command) {
		Email email = validEmail(command);

		// 여기부터의 실패는 모두 같은 예외다. 이유를 구분하지 않는다. (R9)
		Password password = passwordThatAMemberCanHave(command.password());
		Member member = members.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
		if (!passwordHasher.matches(password, member.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return accessTokenIssuer.issue(member);
	}

	/**
	 * 값의 오류를 확인한다: 이메일의 형식, 비어 있는 비밀번호. (AC-2.4)
	 * 비밀번호의 길이(R4)는 여기에서 확인하지 않는다. 가입의 규칙이다.
	 */
	private static Email validEmail(LoginCommand command) {
		Map<String, String> errors = new LinkedHashMap<>();
		Email email = null;
		try {
			email = new Email(command.email());
		}
		catch (IllegalArgumentException exception) {
			errors.put("email", exception.getMessage());
		}
		if (command.password() == null || command.password().isEmpty()) {
			errors.put("password", "비밀번호가 비어 있다");
		}
		if (!errors.isEmpty()) {
			throw new InvalidValuesException(errors);
		}
		return email;
	}

	/**
	 * 비밀번호(R4)가 될 수 없는 값은 누구의 비밀번호도 아니다. 틀린 비밀번호와 같다.
	 */
	private static Password passwordThatAMemberCanHave(String value) {
		try {
			return new Password(value);
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidCredentialsException();
		}
	}

}
