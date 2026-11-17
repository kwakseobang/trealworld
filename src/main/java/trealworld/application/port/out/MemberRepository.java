package trealworld.application.port.out;

import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.Username;

/**
 * 회원을 저장하고 찾는다.
 */
public interface MemberRepository {

	Member save(Member member);

	boolean existsByEmail(Email email);

	boolean existsByUsername(Username username);

}
