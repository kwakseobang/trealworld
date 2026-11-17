package trealworld.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import trealworld.application.port.in.RegisterMemberUseCase;

/**
 * ADR-0005: 오류의 본문은 Problem Details 형식이다. 예상하지 못한 오류도 같다.
 * 유스케이스를 가짜로 바꿔서 오류를 만든다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ApiExceptionHandlerTests {

	@Autowired
	private RestTestClient client;

	@MockitoBean
	private RegisterMemberUseCase registerMember;

	@Test
	void adr0005_an_unexpected_error_is_problem_details() {
		given(registerMember.register(any())).willThrow(new IllegalStateException("cause-of-the-error"));

		register()
				.expectStatus().isEqualTo(500)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(500)
				.jsonPath("$.detail").isNotEmpty();
	}

	/** 원인의 글에는 저장된 값(비밀번호 해시)이 들어 있는 경우가 있다. 본문에 넣지 않는다. (AC-1.7) */
	@Test
	void ac1_7_an_unexpected_error_does_not_show_its_cause() {
		given(registerMember.register(any()))
				.willThrow(new IllegalStateException("insert into member values ('$2a$10$stored-hash')"));

		String body = register().expectStatus().isEqualTo(500).expectBody(String.class).returnResult()
				.getResponseBody();

		assertThat(body).doesNotContain("stored-hash").doesNotContain("insert into")
				.doesNotContain("IllegalStateException").doesNotContain("correct-horse-battery");
	}

	private RestTestClient.ResponseSpec register() {
		return client.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("username", "jake", "email", "jake@example.com", "password", "correct-horse-battery"))
				.exchange();
	}

}
