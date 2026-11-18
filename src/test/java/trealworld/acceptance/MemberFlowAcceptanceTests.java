package trealworld.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plan 01의 작업 7. Spec 01의 목표 1, 2, 3이 이어진다: 가입 → 로그인 → 내 정보 조회.
 * 서버 전체를 띄우고 HTTP로 확인한다. (ADR-0004)
 * 서명 키(trealworld.jwt.secret)를 정하지 않는다. 기본 설정의 서버와 같다.
 * 서버가 만든 임의의 키로 발급한 토큰을 같은 서버가 받아들이는지 본다. (ADR-0003)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class MemberFlowAcceptanceTests {

	private static final String PASSWORD = "correct-horse-battery";
	private static final JsonMapper json = new JsonMapper();

	@Autowired
	private RestTestClient client;

	/** 테스트마다 다른 회원을 쓴다. DB는 테스트끼리 함께 쓴다. */
	private final String id = UUID.randomUUID().toString().substring(0, 8);
	private final String username = "jake-" + id;
	private final String email = "jake-" + id + "@example.com";

	@Test
	void ac1_1_ac2_1_ac3_1_a_visitor_registers_logs_in_and_reads_their_own_information() {
		// 가입의 전에는 로그인할 수 없다. 뒤의 로그인의 성공은 가입의 결과다.
		login().expectStatus().isUnauthorized();

		// 목표 1. 방문자가 회원이 된다.
		assertThat(mapOf(register().expectStatus().isCreated()))
				.containsOnlyKeys("username", "email")
				.containsEntry("username", username)
				.containsEntry("email", email);

		// 목표 2. 회원이 액세스 토큰을 받는다.
		Map<String, Object> loggedIn = mapOf(login().expectStatus().isOk());
		assertThat(loggedIn).containsOnlyKeys("accessToken");
		String accessToken = (String) loggedIn.get("accessToken");
		assertThat(accessToken).isNotBlank();

		// 목표 3. 로그인이 준 토큰으로 가입한 회원의 정보를 받는다.
		assertThat(mapOf(me(accessToken).expectStatus().isOk()))
				.containsOnlyKeys("username", "email")
				.containsEntry("username", username)
				.containsEntry("email", email);
	}

	private RestTestClient.ResponseSpec register() {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("username", username);
		body.put("email", email);
		body.put("password", PASSWORD);
		return client.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	private RestTestClient.ResponseSpec login() {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("email", email);
		body.put("password", PASSWORD);
		return client.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	private RestTestClient.ResponseSpec me(String accessToken) {
		return client.get().uri("/api/members/me").header("Authorization", "Bearer " + accessToken).exchange();
	}

	private static Map<String, Object> mapOf(RestTestClient.ResponseSpec response) {
		String body = response.expectBody(String.class).returnResult().getResponseBody();
		return json.readValue(body, new TypeReference<Map<String, Object>>() {
		});
	}

}
