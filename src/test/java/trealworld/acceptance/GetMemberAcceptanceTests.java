package trealworld.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spec 01의 목표 3 (내 정보 조회). AC-3.1 ~ AC-3.5. Domain 01의 R10.
 * 서버 전체를 띄우고 HTTP로 확인한다. (ADR-0004)
 * 서명 키를 설정으로 정한다. 테스트가 같은 키로 토큰을 직접 만든다.
 * 시각은 테스트의 Clock으로 바꾼다. (ADR-0003)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "trealworld.jwt.secret="
		+ GetMemberAcceptanceTests.SECRET)
@AutoConfigureRestTestClient
class GetMemberAcceptanceTests {

	static final String SECRET = TestTokens.SECRET;
	private static final String PASSWORD = "correct-horse-battery";
	private static final JsonMapper json = new JsonMapper();

	@Autowired
	private RestTestClient client;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private MovableClock clock;

	/** 테스트마다 다른 회원을 쓴다. DB는 테스트끼리 함께 쓴다. */
	private final String id = UUID.randomUUID().toString().substring(0, 8);
	private final String username = "jake-" + id;
	private final String email = "jake-" + id + "@example.com";

	/** jake의 액세스 토큰. 로그인해서 받는다. */
	private String token;

	@BeforeEach
	void a_member_is_logged_in() {
		clock.reset();
		token = registerAndLogin(username, email);
	}

	@Test
	void ac3_1_a_valid_access_token_returns_the_username_and_the_email_of_its_owner() {
		String body = bodyOf(me(token).expectStatus().isOk());

		assertThat(json.readValue(body, new TypeReference<Map<String, Object>>() {
		})).containsOnlyKeys("username", "email").containsEntry("username", username).containsEntry("email", email);
	}

	/** AC-1.7은 "어떤 결과에도"다. 조회의 결과도 들어간다. */
	@Test
	void ac1_7_the_result_has_no_password_and_no_password_hash() {
		String hash = new JdbcTemplate(dataSource).queryForObject("select password_hash from member where email = ?",
				String.class, email);

		assertThat(bodyOf(me(token).expectStatus().isOk())).doesNotContain(PASSWORD).doesNotContain(hash);
	}

	@Test
	void ac3_2_a_request_without_an_access_token_is_rejected() {
		client.get().uri("/api/members/me").exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().valueEquals("WWW-Authenticate", "Bearer");
	}

	/** ADR-0005: 필터가 거부한 요청(401)의 본문도 Problem Details 형식이다. */
	@Test
	void adr0005_the_body_of_a_rejection_is_problem_details() {
		client.get().uri("/api/members/me").exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(401)
				.jsonPath("$.title").isEqualTo("Unauthorized")
				.jsonPath("$.detail").isNotEmpty()
				.jsonPath("$.instance").isEqualTo("/api/members/me")
				.jsonPath("$.errors").doesNotExist();
	}

	@Test
	void ac3_3_a_token_signed_with_another_key_is_rejected() {
		String forged = signed("another-secret-another-secret-0123456789", subjectOf(token),
				clock.instant().plus(Duration.ofHours(1)));

		me(forged).expectStatus().isUnauthorized();
	}

	/** 본문만 바꾸고 서명은 그대로 둔다. 다른 회원인 척한다. */
	@Test
	void ac3_3_a_token_with_a_changed_payload_is_rejected() {
		String[] parts = token.split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		String changed = payload.replace("\"sub\":\"" + subjectOf(token) + "\"", "\"sub\":\"1\"")
				.replace("\"exp\":", "\"exp\":1");
		assertThat(changed).isNotEqualTo(payload);
		String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
				.encodeToString(changed.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

		me(tampered).expectStatus().isUnauthorized();
	}

	/** 서명이 없는 토큰 ("alg": "none"). */
	@Test
	void ac3_3_a_token_without_a_signature_is_rejected() throws Exception {
		String unsigned = new PlainJWT(new JWTClaimsSet.Builder().subject(subjectOf(token))
				.expirationTime(Date.from(clock.instant().plus(Duration.ofHours(1)))).build()).serialize();

		me(unsigned).expectStatus().isUnauthorized();
	}

	@Test
	void ac3_3_a_value_that_is_not_a_token_is_rejected() {
		me("not-a-token").expectStatus().isUnauthorized();
		me(token + "x").expectStatus().isUnauthorized();
		client.get().uri("/api/members/me").header("Authorization", token).exchange()
				.expectStatus().isUnauthorized();
		client.get().uri("/api/members/me").header("Authorization", "Basic " + token).exchange()
				.expectStatus().isUnauthorized();
	}

	/** ADR-0003: 유효 시간은 1시간이다. 시각은 주입한 Clock에서 읽는다. */
	@Test
	void ac3_3_a_token_is_valid_just_before_it_expires_and_rejected_after_it_expires() {
		clock.move(Duration.ofMinutes(59).plusSeconds(50));
		me(token).expectStatus().isOk();

		clock.move(Duration.ofMinutes(60).plusSeconds(10));
		me(token).expectStatus().isUnauthorized();
	}

	/** 올바른 키로 서명했어도 만료 시각이 지났으면 거부한다. */
	@Test
	void ac3_3_an_expired_token_signed_with_the_right_key_is_rejected() {
		me(signed(SECRET, subjectOf(token), clock.instant().minusSeconds(10))).expectStatus().isUnauthorized();
		me(signed(SECRET, subjectOf(token), clock.instant().plusSeconds(600))).expectStatus().isOk();
	}

	@Test
	void ac3_4_r10_the_result_is_always_the_owner_of_the_token() {
		String finnToken = registerAndLogin("finn-" + id, "finn-" + id + "@example.com");

		me(token).expectStatus().isOk().expectBody().jsonPath("$.username").isEqualTo(username);
		me(finnToken).expectStatus().isOk().expectBody().jsonPath("$.username").isEqualTo("finn-" + id);
	}

	/** 다른 회원을 가리키는 방법이 없다. 요청에 넣은 값은 조회의 대상을 바꾸지 못한다. */
	@Test
	void ac3_4_r10_another_member_cannot_be_named_in_the_request() {
		String finnToken = registerAndLogin("finn-" + id, "finn-" + id + "@example.com");
		String finnId = subjectOf(finnToken);

		get("/api/members/me?id=" + finnId + "&username=finn-" + id + "&email=finn-" + id + "@example.com", token)
				.expectStatus().isOk().expectBody().jsonPath("$.username").isEqualTo(username);
		assertThat(statusOf(get("/api/members/" + finnId, token))).isNotEqualTo(200);
		assertThat(statusOf(get("/api/members/finn-" + id, token))).isNotEqualTo(200);
		assertThat(statusOf(get("/api/members", token))).isNotEqualTo(200);
	}

	/** 토큰이 유효해도 그 주인인 회원이 없으면 거부한다. */
	@Test
	void ac3_5_a_valid_token_of_a_member_that_does_not_exist_is_rejected() {
		me(token).expectStatus().isOk();
		new JdbcTemplate(dataSource).update("delete from member where email = ?", email);

		me(token)
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void ac3_5_a_valid_token_with_a_subject_that_is_not_a_member_is_rejected() {
		Instant expiry = clock.instant().plus(Duration.ofHours(1));

		String noToken = bodyOf(client.get().uri("/api/members/me").exchange().expectStatus().isUnauthorized());

		// 상태만 보지 않는다. 본문의 instance가 /error이면 서버의 오류가 401에 숨은 것이다
		for (String subject : new String[] { UUID.randomUUID().toString(), "999999999", "1", "not-a-number", "-1", "7.0", null }) {
			assertThat(bodyOf(me(signed(SECRET, subject, expiry)).expectStatus().isUnauthorized()))
					.as("sub = %s", subject).isEqualTo(noToken).contains("\"instance\":\"/api/members/me\"");
		}
	}

	/** 거부의 이유(없다, 위조, 만료, 회원이 없다)를 본문으로 구분하지 못한다. */
	@Test
	void adr0005_every_rejection_has_the_same_body() {
		String noToken = bodyOf(client.get().uri("/api/members/me").exchange().expectStatus().isUnauthorized());
		String forged = bodyOf(me(signed("another-secret-another-secret-0123456789", subjectOf(token),
				clock.instant().plus(Duration.ofHours(1)))).expectStatus().isUnauthorized());
		String expired = bodyOf(me(signed(SECRET, subjectOf(token), clock.instant().minusSeconds(10)))
				.expectStatus().isUnauthorized());
		String noMember = bodyOf(me(signed(SECRET, UUID.randomUUID().toString(), clock.instant().plus(Duration.ofHours(1))))
				.expectStatus().isUnauthorized());

		assertThat(noToken).isNotBlank().contains("액세스 토큰");
		assertThat(forged).isEqualTo(noToken);
		assertThat(expired).isEqualTo(noToken);
		assertThat(noMember).isEqualTo(noToken);
	}

	/** ADR-0003: 가입과 로그인만 인증 없이 받아들인다. 나머지 요청에는 인증을 요구한다. */
	@Test
	void adr0003_every_other_request_needs_an_access_token() {
		assertThat(statusOf(client.get().uri("/api/anything").exchange())).isEqualTo(401);
		assertThat(statusOf(client.get().uri("/").exchange())).isEqualTo(401);
		assertThat(statusOf(client.get().uri("/api/members").exchange())).isEqualTo(401);
		assertThat(statusOf(client.get().uri("/api/auth/login").exchange())).isEqualTo(401);
		assertThat(statusOf(client.delete().uri("/api/members/me").exchange())).isEqualTo(401);
	}

	/** ADR-0003: 토큰은 Authorization 헤더로만 받는다. */
	@Test
	void adr0003_a_token_in_the_query_string_is_not_accepted() {
		client.get().uri("/api/members/me?access_token=" + token).exchange().expectStatus().isUnauthorized();
	}

	/** ADR-0003: 세션을 쓰지 않는다. 서버는 쿠키를 주지 않는다. */
	@Test
	void adr0003_the_server_does_not_create_a_session() {
		assertThat(me(token).expectStatus().isOk().returnResult().getResponseHeaders().get("Set-Cookie")).isNull();
		assertThat(client.get().uri("/api/members/me").exchange().returnResult().getResponseHeaders()
				.get("Set-Cookie")).isNull();
	}

	private RestTestClient.ResponseSpec me(String accessToken) {
		return get("/api/members/me", accessToken);
	}

	private RestTestClient.ResponseSpec get(String uri, String accessToken) {
		return client.get().uri(uri).header("Authorization", "Bearer " + accessToken).exchange();
	}

	private String registerAndLogin(String username, String email) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("username", username);
		body.put("email", email);
		body.put("password", PASSWORD);
		client.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).body(body).exchange()
				.expectStatus().isCreated();
		body.remove("username");
		String response = bodyOf(client.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.body(body).exchange().expectStatus().isOk());
		return json.readValue(response, new TypeReference<Map<String, String>>() {
		}).get("accessToken");
	}

	/** 토큰의 본문에 있는 회원의 식별자. */
	private static String subjectOf(String accessToken) {
		String payload = new String(Base64.getUrlDecoder().decode(accessToken.split("\\.")[1]),
				StandardCharsets.UTF_8);
		return json.readValue(payload, new TypeReference<Map<String, Object>>() {
		}).get("sub").toString();
	}

	private static String signed(String secret, String subject, Instant expiry) {
		return TestTokens.signed(secret, subject, expiry);
	}

	private static String bodyOf(RestTestClient.ResponseSpec response) {
		return response.expectBody(String.class).returnResult().getResponseBody();
	}

	private static int statusOf(RestTestClient.ResponseSpec response) {
		return response.returnResult().getStatus().value();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ClockForTests {

		@Bean
		@Primary
		MovableClock movableClock() {
			return new MovableClock();
		}

	}

	/** 실제 시각에서 정한 만큼 앞으로 간 시각을 준다. */
	static class MovableClock extends Clock {

		private volatile Duration offset = Duration.ZERO;

		void move(Duration offset) {
			this.offset = offset;
		}

		void reset() {
			this.offset = Duration.ZERO;
		}

		@Override
		public Instant instant() {
			return Instant.now().plus(offset);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

	}

}
