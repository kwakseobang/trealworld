package trealworld.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import trealworld.domain.Member;

interface MemberJpaRepository extends JpaRepository<Member, Long> {

	boolean existsByEmailValue(String email);

	boolean existsByUsernameValue(String username);

}
