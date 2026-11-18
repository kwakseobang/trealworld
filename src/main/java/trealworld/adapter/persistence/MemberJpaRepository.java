package trealworld.adapter.persistence;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import trealworld.domain.Member;

interface MemberJpaRepository extends JpaRepository<Member, UUID> {

	Optional<Member> findByEmailValue(String email);

	boolean existsByEmailValue(String email);

	boolean existsByUsernameValue(String username);

}
