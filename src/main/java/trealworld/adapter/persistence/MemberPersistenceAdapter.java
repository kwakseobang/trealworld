package trealworld.adapter.persistence;

import java.util.UUID;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import trealworld.application.port.out.MemberNotUniqueException;
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

	/**
	 * DB의 제약이 저장을 거부하면 MemberNotUniqueException으로 바꾼다. 어느 제약인지는 읽지 않는다. DB마다 다르기 때문이다.
	 */
	@Override
	public Member save(Member member) {
		try {
			return jpaRepository.saveAndFlush(member);
		}
		catch (DataIntegrityViolationException exception) {
			throw new MemberNotUniqueException(exception);
		}
	}

	@Override
	public Optional<Member> findById(UUID id) {
		return jpaRepository.findById(id);
	}

	@Override
	public Optional<Member> findByEmail(Email email) {
		return jpaRepository.findByEmailValue(email.value());
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
