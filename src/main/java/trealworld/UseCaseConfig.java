package trealworld;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import trealworld.application.port.in.LoginUseCase;
import trealworld.application.port.in.RegisterMemberUseCase;
import trealworld.application.port.out.AccessTokenIssuer;
import trealworld.application.port.out.MemberRepository;
import trealworld.application.port.out.PasswordHasher;
import trealworld.application.service.LoginService;
import trealworld.application.service.RegisterMemberService;

/**
 * 유스케이스를 빈으로 등록한다. application은 Spring을 모른다. 그래서 조립을 여기에서 한다. (ADR-0002)
 */
@Configuration(proxyBeanMethods = false)
class UseCaseConfig {

	@Bean
	RegisterMemberUseCase registerMemberUseCase(MemberRepository members, PasswordHasher passwordHasher) {
		return new RegisterMemberService(members, passwordHasher);
	}

	@Bean
	LoginUseCase loginUseCase(MemberRepository members, PasswordHasher passwordHasher,
			AccessTokenIssuer accessTokenIssuer) {
		return new LoginService(members, passwordHasher, accessTokenIssuer);
	}

}
