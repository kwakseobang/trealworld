package trealworld.application.port.out;

import java.util.Optional;

import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.Username;

/**
 * 회원을 저장하고 찾는다.
 */
public interface MemberRepository {

	/**
	 * @throws MemberNotUniqueException 이메일이나 사용자 이름이 이미 있다.
	 * existsByEmail과 existsByUsername이 "없다"고 답한 뒤에도 일어난다. 그 사이에 다른 가입이 저장했을 때다
	 */
	Member save(Member member);

	Optional<Member> findById(Long id);

	Optional<Member> findByEmail(Email email);

	boolean existsByEmail(Email email);

	boolean existsByUsername(Username username);

}
