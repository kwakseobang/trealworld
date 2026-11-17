package trealworld.adapter.persistence;

import org.springframework.stereotype.Repository;

import trealworld.application.port.out.MemberRepository;
import trealworld.domain.Email;
import trealworld.domain.Member;
import trealworld.domain.Username;

/**
 * 회원을 JPA로 저장하고 찾는다.
 */
@Repository
class MemberPersistenceAdapter implements MemberRepository {

	private final MemberJpaRepository jpaRepository;

	MemberPersistenceAdapter(MemberJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Member save(Member member) {
		return jpaRepository.save(member);
	}

	@Override
	public boolean existsByEmail(Email email) {
		return jpaRepository.existsByEmailValue(email.value());
	}

	@Override
	public boolean existsByUsername(Username username) {
		return jpaRepository.existsByUsernameValue(username.value());
	}

}
