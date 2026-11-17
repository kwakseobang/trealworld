package trealworld.application.port.out;

import trealworld.domain.Password;
import trealworld.domain.PasswordHash;

/**
 * 비밀번호를 해시로 바꾼다.
 */
public interface PasswordHasher {

	PasswordHash hash(Password password);

}
