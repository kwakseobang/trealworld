package trealworld.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * ADR-0005: 오류의 본문은 Problem Details 형식이다. 컨트롤러에 닿지 않은 요청의 오류도 같다.
 * 서버는 그런 오류를 오류의 화면(/error)으로 넘긴다. 그 응답이 401로 바뀌면 안 된다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "trealworld.jwt.secret=" + TestTokens.SECRET)
@AutoConfigureRestTestClient
class ErrorResponseAcceptanceTests {

	@Autowired
	private RestTestClient client;

	private final String id = UUID.randomUUID().toString().substring(0, 8);

	/** 유효한 액세스 토큰. */
	private String token;

	@BeforeEach
	void a_member_is_logged_in() {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("username", "jake-" + id);
		body.put("email", "jake-" + id + "@example.com");
		body.put("password", "correct-horse-battery");
		client.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).body(body).exchange()
				.expectStatus().isCreated();
		body.remove("username");
		String response = client.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).body(body)
				.exchange().expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
		token = new JsonMapper().readValue(response, new TypeReference<Map<String, String>>() {
		}).get("accessToken");
		get("/api/members/me", token).expectStatus().isOk();
	}

	/**
	 * Spring Security의 방화벽은 이런 경로를 거부한다. 요청이 틀린 것이다 (400). 토큰이 틀린 것이 아니다 (401).
	 */
	@ParameterizedTest
	@ValueSource(strings = { "/api/members/me;x=1", "/api//members/me", "/api/members/%2e%2e/me" })
	void adr0005_a_path_that_the_server_does_not_accept_is_400_and_not_401(String path) {
		client.get().uri(java.net.URI.create(path)).header("Authorization", "Bearer " + token).exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectHeader().doesNotExist("WWW-Authenticate")
				.expectBody()
				.jsonPath("$.status").isEqualTo(400)
				.jsonPath("$.title").isEqualTo("Bad Request")
				.jsonPath("$.detail").isNotEmpty()
				.jsonPath("$.instance").value(instance -> assertThat(instance).isNotEqualTo("/error"));
	}

	@Test
	void adr0005_a_path_that_the_server_does_not_accept_is_400_without_a_token_too() {
		client.get().uri(java.net.URI.create("/api/members/me;x=1")).exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	/** 오류의 화면을 밖에서 직접 부르는 요청은 다른 요청과 같다. 인증이 필요하다. (ADR-0003) */
	@Test
	void adr0003_the_error_page_requested_directly_needs_an_access_token() {
		client.get().uri("/error").exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().valueEquals("WWW-Authenticate", "Bearer");
	}

	/** 오류가 없는데 오류의 화면을 부르면 그런 자원은 없다. Spring Boot의 기본 본문이 나가지 않는다. */
	@Test
	void adr0005_the_error_page_requested_directly_with_a_token_is_problem_details() {
		String body = get("/error", token)
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody(String.class).returnResult().getResponseBody();

		assertThat(body).doesNotContain("timestamp").doesNotContain("999");
	}

	/** 만료된 토큰은 그대로 401이다. 오류의 화면을 허용한 것이 인증을 느슨하게 하지 않는다. */
	@Test
	void ac3_3_an_expired_token_is_still_rejected() {
		get("/api/members/me", TestTokens.signed(TestTokens.SECRET, "1", Instant.now().minusSeconds(10)))
				.expectStatus().isUnauthorized()
				.expectBody().jsonPath("$.instance").isEqualTo("/api/members/me");
	}

	private RestTestClient.ResponseSpec get(String uri, String accessToken) {
		return client.get().uri(uri).header("Authorization", "Bearer " + accessToken).exchange();
	}

}
