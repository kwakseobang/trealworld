package trealworld.adapter.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import trealworld.application.port.out.PasswordHasher;
import trealworld.domain.Password;
import trealworld.domain.PasswordHash;

/**
 * 비밀번호를 BCrypt로 해시하고, 해시와 맞는지 확인한다. (ADR-0003)
 */
@Component
class BCryptPasswordHasher implements PasswordHasher {

	private final PasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public PasswordHash hash(Password password) {
		return new PasswordHash(encoder.encode(password.value()));
	}

	@Override
	public boolean matches(Password password, PasswordHash hash) {
		return encoder.matches(password.value(), hash.value());
	}

}
