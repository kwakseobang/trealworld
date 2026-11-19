package trealworld.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import trealworld.TrealworldApplication;

/**
 * 서버를 다시 시작한 뒤의 옛 토큰. (AC-3.4, AC-3.5, ADR-0003)
 * 서명 키를 고정하면 옛 토큰의 서명은 다시 시작한 서버에서도 맞다. DB는 인메모리라서 회원은 사라진다.
 * 옛 토큰의 주인은 이제 없다. 새로 가입한 회원이 그 토큰의 주인이 되어서는 안 된다.
 * 서버를 두 번 띄워야 해서 @SpringBootTest를 쓰지 않는다.
 */
class MemberRestartAcceptanceTests {

	private static final String PASSWORD = "correct-horse-battery";
	private static final JsonMapper json = new JsonMapper();

	private final HttpClient http = HttpClient.newHttpClient();

	@Test
	void ac3_4_ac3_5_a_token_from_before_a_restart_does_not_read_the_member_who_registers_after_it() throws Exception {
		String jakeToken;
		try (ConfigurableApplicationContext first = start()) {
			assertThat(register(first, "jake").statusCode()).isEqualTo(201);
			jakeToken = (String) mapOf(login(first, "jake")).get("accessToken");
			// 다시 시작하기 전에는 jake의 토큰으로 jake가 나온다.
			assertThat(mapOf(me(first, jakeToken))).containsEntry("username", "jake");
		}

		try (ConfigurableApplicationContext second = start()) {
			// 다시 시작한 서버의 첫 회원은 finn이다. jake는 없다.
			assertThat(register(second, "finn").statusCode()).isEqualTo(201);

			String finnToken = (String) mapOf(login(second, "finn")).get("accessToken");

			// 두 서버가 설정한 키를 쓴다. 옛 토큰의 서명은 다시 시작한 서버에서도 맞다.
			// 이것이 없으면 "서버가 키를 무시하고 임의의 키를 쓴다"는 결함에서도 아래의 401이 나온다.
			assertThat(TestTokens.isSignedWith(TestTokens.SECRET, jakeToken)).isTrue();
			assertThat(TestTokens.isSignedWith(TestTokens.SECRET, finnToken)).isTrue();
			// 식별자는 다시 쓰이지 않는다.
			assertThat(TestTokens.subjectOf(jakeToken)).isNotEqualTo(TestTokens.subjectOf(finnToken));

			HttpResponse<String> response = me(second, jakeToken);

			assertThat(response.body()).doesNotContain("finn");
			assertThat(response.statusCode()).isEqualTo(401);
			// finn 자신의 토큰은 된다. 서버가 모든 토큰을 거부하는 것이 아니다.
			assertThat(mapOf(me(second, finnToken))).containsEntry("username", "finn");
		}
	}

	/** 서명 키를 고정하고 임의의 포트로 서버를 띄운다. 띄울 때마다 DB는 비어 있다. */
	private static ConfigurableApplicationContext start() {
		return new SpringApplicationBuilder(TrealworldApplication.class)
				.properties("server.port=0", "trealworld.jwt.secret=" + TestTokens.SECRET)
				.run();
	}

	private HttpResponse<String> register(ConfigurableApplicationContext server, String name) throws Exception {
		return post(server, "/api/members", Map.of("username", name, "email", name + "@example.com", "password", PASSWORD));
	}

	private HttpResponse<String> login(ConfigurableApplicationContext server, String name) throws Exception {
		return post(server, "/api/auth/login", Map.of("email", name + "@example.com", "password", PASSWORD));
	}

	private HttpResponse<String> me(ConfigurableApplicationContext server, String accessToken) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(uri(server, "/api/members/me"))
				.header("Authorization", "Bearer " + accessToken).GET().build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(ConfigurableApplicationContext server, String path, Map<String, String> body)
			throws Exception {
		HttpRequest request = HttpRequest.newBuilder(uri(server, path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static URI uri(ConfigurableApplicationContext server, String path) {
		return URI.create("http://localhost:" + server.getEnvironment().getProperty("local.server.port") + path);
	}

	private static Map<String, Object> mapOf(HttpResponse<String> response) {
		return json.readValue(response.body(), new TypeReference<Map<String, Object>>() {
		});
	}

}
