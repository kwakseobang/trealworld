package trealworld.adapter.security;

import java.time.Clock;
import java.time.Duration;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 요청의 인증. 필터 체인이 액세스 토큰을 검사한다. (ADR-0003)
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
class SecurityConfig {

	/**
	 * 가입과 로그인만 인증 없이 받아들인다. 나머지 요청에는 인증을 요구한다.
	 * 세션과 CSRF 보호는 끈다. 토큰은 Authorization: Bearer 헤더로만 받는다.
	 * <p>
	 * 서버가 오류를 오류의 화면(/error)으로 넘기는 것은 허용한다. 클라이언트의 요청이 아니다. 서버의 안에서 넘기는 것이다.
	 * 허용하지 않으면 서버의 오류(500)와 틀린 요청(400)이 모두 401로 바뀐다.
	 * 클라이언트가 /error를 직접 부르는 요청은 다른 요청과 같다. 인증이 필요하다.
	 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder accessTokenDecoder,
			MemberAuthenticationConverter memberAuthenticationConverter,
			ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.POST, "/api/members", "/api/auth/login").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(server -> server
						.jwt(jwt -> jwt.decoder(accessTokenDecoder)
								.jwtAuthenticationConverter(memberAuthenticationConverter))
						.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint));
		return http.build();
	}

	@Bean
	JwtDecoder jwtDecoder(JwtSigningKey key, Clock clock) {
		return accessTokenDecoder(key, clock);
	}

	/**
	 * 서명(HS256)과 만료 시각을 검사한다. 시각은 주입한 Clock에서 읽는다. 여유 시간을 두지 않는다.
	 */
	static JwtDecoder accessTokenDecoder(JwtSigningKey key, Clock clock) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key.secretKey()).macAlgorithm(MacAlgorithm.HS256)
				.build();
		JwtTimestampValidator expiry = new JwtTimestampValidator(Duration.ZERO);
		expiry.setClock(clock);
		decoder.setJwtValidator(expiry);
		return decoder;
	}

}
