package trealworld.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import trealworld.application.port.in.GetMemberUseCase;

/**
 * 필터의 안에서 난 예상하지 못한 오류. (ADR-0005)
 * 보안 어댑터는 인증할 때 회원을 확인한다. 그 확인이 실패하면 (예: DB의 장애) 서버의 오류다 (500).
 * 토큰이 틀린 것이 아니다 (401). 유스케이스를 가짜로 바꿔서 오류를 만든다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "trealworld.jwt.secret=" + TestTokens.SECRET)
@AutoConfigureRestTestClient
class FilterErrorAcceptanceTests {

	@Autowired
	private RestTestClient client;

	@MockitoBean
	private GetMemberUseCase getMember;

	@Test
	void adr0005_an_unexpected_error_in_the_filter_is_500_and_not_401() {
		given(getMember.getMember(any())).willThrow(new IllegalStateException("cause-of-the-error"));

		me()
				.expectStatus().isEqualTo(500)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectHeader().doesNotExist("WWW-Authenticate")
				.expectBody()
				.jsonPath("$.status").isEqualTo(500)
				.jsonPath("$.title").isEqualTo("Internal Server Error")
				.jsonPath("$.detail").isNotEmpty()
				.jsonPath("$.instance").isEqualTo("/api/members/me");
	}

	/** 원인의 글에는 저장된 값이 들어 있는 경우가 있다. 본문에 넣지 않는다. (AC-1.7) */
	@Test
	void ac1_7_an_unexpected_error_in_the_filter_does_not_show_its_cause() {
		given(getMember.getMember(any()))
				.willThrow(new IllegalStateException("select * from member where password_hash = '$2a$10$stored-hash'"));

		String body = me().expectStatus().isEqualTo(500).expectBody(String.class).returnResult().getResponseBody();

		assertThat(body).doesNotContain("stored-hash").doesNotContain("select").doesNotContain("IllegalStateException")
				.doesNotContain("trace").doesNotContain("timestamp");
	}

	private RestTestClient.ResponseSpec me() {
		String token = TestTokens.signed(TestTokens.SECRET, "11111111-1111-4111-8111-111111111111", Instant.now().plusSeconds(600));
		return client.get().uri("/api/members/me").header("Authorization", "Bearer " + token).exchange();
	}

}
