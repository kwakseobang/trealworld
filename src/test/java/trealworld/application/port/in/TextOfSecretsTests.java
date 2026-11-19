package trealworld.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 비밀번호와 토큰을 가진 값은 글로 바꿀 때 그 값을 가린다. 로그와 예외의 글에 남지 않는다. (R5, ADR-0003)
 * 가린 뒤의 글을 그대로 비교한다. "값이 없다"만 보면 빈 글도 통과한다.
 */
class TextOfSecretsTests {

	@Test
	void r5_the_text_of_a_register_command_hides_the_password() {
		assertThat(new RegisterMemberCommand("jake", "jake@example.com", "correct-horse-battery").toString())
				.isEqualTo("RegisterMemberCommand[username=jake, email=jake@example.com, password=****]");
	}

	@Test
	void r5_the_text_of_a_login_command_hides_the_password() {
		assertThat(new LoginCommand("jake@example.com", "correct-horse-battery").toString())
				.isEqualTo("LoginCommand[email=jake@example.com, password=****]");
	}

	@Test
	void adr0003_the_text_of_an_access_token_hides_the_token() {
		assertThat(new AccessToken("header.payload.signature").toString()).isEqualTo("AccessToken[****]");
	}

}
