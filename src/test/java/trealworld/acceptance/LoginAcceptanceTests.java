package trealworld.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spec 01의 목표 2 (로그인). AC-2.1 ~ AC-2.5.
 * 서버 전체를 띄우고 HTTP로 확인한다. (ADR-0004)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class LoginAcceptanceTests {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private RestTestClient client;

	@Autowired
	private DataSource dataSource;

	/** 테스트마다 다른 회원을 쓴다. DB는 테스트끼리 함께 쓴다. */
	private final String id = UUID.randomUUID().toString().substring(0, 8);
	private final String email = "jake-" + id + "@example.com";

	@BeforeEach
	void a_member_exists() {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("username", "jake-" + id);
		body.put("email", email);
		body.put("password", PASSWORD);
		client.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).body(body).exchange()
				.expectStatus().isCreated();
	}

	@Test
	void ac2_1_logging_in_with_the_right_email_and_password_returns_an_access_token() {
		String body = bodyOf(login(email, PASSWORD).expectStatus().isOk());

		Map<String, Object> json = new JsonMapper().readValue(body, new TypeReference<>() {
		});
		assertThat(json).containsOnlyKeys("accessToken");
		assertThat(json.get("accessToken")).asString().isNotBlank();
	}

	@Test
	void ac2_2_a_wrong_password_is_rejected() {
		login(email, "wrong-horse-battery")
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.accessToken").doesNotExist();
	}

	@Test
	void ac2_3_an_email_that_does_not_exist_is_rejected() {
		login("nobody-" + id + "@example.com", PASSWORD)
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	/** 없는 이메일의 결과와 틀린 비밀번호의 결과는 구분할 수 없다. (R9) */
	@Test
	void ac2_3_an_email_that_does_not_exist_cannot_be_told_from_a_wrong_password() {
		RestTestClient.ResponseSpec wrongPassword = login(email, "wrong-horse-battery");
		RestTestClient.ResponseSpec noSuchEmail = login("nobody-" + id + "@example.com", PASSWORD);

		String wrongPasswordType = contentTypeOf(wrongPassword);
		String noSuchEmailType = contentTypeOf(noSuchEmail);
		String wrongPasswordBody = bodyOf(wrongPassword.expectStatus().isUnauthorized());
		String noSuchEmailBody = bodyOf(noSuchEmail.expectStatus().isUnauthorized());

		assertThat(noSuchEmailBody).isNotBlank().isEqualTo(wrongPasswordBody);
		assertThat(noSuchEmailType).isEqualTo(wrongPasswordType);
	}

	@ParameterizedTest
	@MethodSource("emptyValues")
	void ac2_4_an_empty_value_is_rejected_as_an_invalid_value(String field, String value) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("email", email);
		body.put("password", PASSWORD);
		if (value == null) {
			body.remove(field);
		}
		else {
			body.put(field, value);
		}

		post(body)
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.errors." + field).exists()
				.jsonPath("$.errors.length()").isEqualTo(1);
	}

	static Stream<Arguments> emptyValues() {
		return Stream.of(Arguments.of("email", null), Arguments.of("email", ""), Arguments.of("email", "   "),
				Arguments.of("password", null), Arguments.of("password", ""));
	}

	@Test
	void ac2_4_every_empty_value_is_named() {
		post(Map.of())
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.email").exists()
				.jsonPath("$.errors.password").exists();
	}

	@ParameterizedTest
	@ValueSource(strings = { "jake", "jake@", "@example.com", "jake@example" })
	void ac2_4_an_email_with_an_invalid_format_is_rejected_as_an_invalid_value(String invalidEmail) {
		login(invalidEmail, PASSWORD)
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.email").exists();
	}

	/**
	 * 가입할 수 없는 길이의 비밀번호는 값의 오류가 아니다. 틀린 비밀번호다. 누구의 비밀번호도 아니기 때문이다.
	 * 로그인은 가입의 길이 규칙(R4)을 알려주지 않는다.
	 */
	@ParameterizedTest
	@MethodSource("passwordsThatNobodyHas")
	void ac2_2_a_password_that_nobody_can_have_is_a_wrong_password(String password) {
		String wrongPassword = bodyOf(login(email, "wrong-horse-battery").expectStatus().isUnauthorized());

		assertThat(bodyOf(login(email, password).expectStatus().isUnauthorized())).isEqualTo(wrongPassword);
	}

	static Stream<String> passwordsThatNobodyHas() {
		return Stream.of("short", "   ", "a".repeat(73), "a".repeat(1000));
	}

	@Test
	void ac2_5_an_email_in_another_case_logs_in() {
		login(email.toUpperCase(), PASSWORD)
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.accessToken").isNotEmpty();
	}

	/** 로그인의 어떤 결과에도 비밀번호와 비밀번호 해시가 없다. 목표 1의 AC-1.7과 같은 뜻이다. */
	@Test
	void no_result_of_a_login_has_the_password_or_the_password_hash() {
		String hash = new JdbcTemplate(dataSource).queryForObject("select password_hash from member where email = ?",
				String.class, email);

		String success = bodyOf(login(email, PASSWORD).expectStatus().isOk());
		String wrongPassword = bodyOf(login(email, "wrong-horse-battery").expectStatus().isUnauthorized());
		String invalid = bodyOf(login("not-an-email", PASSWORD).expectStatus().isBadRequest());

		assertThat(success).doesNotContain(PASSWORD).doesNotContain(hash);
		assertThat(wrongPassword).doesNotContain("wrong-horse-battery").doesNotContain(hash);
		assertThat(invalid).doesNotContain(PASSWORD);
	}

	private RestTestClient.ResponseSpec login(String email, String password) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("email", email);
		body.put("password", password);
		return post(body);
	}

	private RestTestClient.ResponseSpec post(Map<String, String> body) {
		return client.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	private static String bodyOf(RestTestClient.ResponseSpec response) {
		return response.expectBody(String.class).returnResult().getResponseBody();
	}

	private static String contentTypeOf(RestTestClient.ResponseSpec response) {
		return String.valueOf(response.returnResult().getResponseHeaders().getContentType());
	}

}
