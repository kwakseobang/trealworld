package trealworld.application.port.out;

import trealworld.domain.Password;
import trealworld.domain.PasswordHash;

/**
 * 비밀번호를 해시로 바꾸고, 비밀번호가 해시와 맞는지 확인한다.
 */
public interface PasswordHasher {

	PasswordHash hash(Password password);

	boolean matches(Password password, PasswordHash hash);

}
