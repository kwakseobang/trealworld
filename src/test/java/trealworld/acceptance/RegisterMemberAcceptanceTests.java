package trealworld.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.sql.DataSource;

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

import tools.jackson.databind.json.JsonMapper;

/**
 * Spec 01의 목표 1 (가입). AC-1.1 ~ AC-1.8.
 * 서버 전체를 띄우고 HTTP로 확인한다. (ADR-0004)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class RegisterMemberAcceptanceTests {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	private RestTestClient client;

	@Autowired
	private DataSource dataSource;

	/** 테스트마다 다른 회원을 쓴다. DB는 테스트끼리 함께 쓴다. */
	private final String id = UUID.randomUUID().toString().substring(0, 8);
	private final String username = "jake-" + id;
	private final String email = "jake-" + id + "@example.com";

	@Test
	void ac1_1_registering_with_valid_values_creates_a_member() {
		register(username, email, PASSWORD)
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.username").isEqualTo(username)
				.jsonPath("$.email").isEqualTo(email);
	}

	@ParameterizedTest
	@MethodSource("emptyValues")
	void ac1_2_an_empty_value_is_rejected_and_the_field_is_named(String field, String value) {
		Map<String, String> body = validBody();
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
		return Stream.of("username", "email", "password")
				.flatMap(field -> Stream.of(Arguments.of(field, null), Arguments.of(field, ""), Arguments.of(field, "   ")));
	}

	@Test
	void ac1_2_every_empty_value_is_named() {
		post(Map.of())
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.username").exists()
				.jsonPath("$.errors.email").exists()
				.jsonPath("$.errors.password").exists();
	}

	@ParameterizedTest
	@ValueSource(strings = { "jake", "jake@", "@example.com", "jake@example" })
	void ac1_3_an_email_with_an_invalid_format_is_rejected(String invalidEmail) {
		register(username, invalidEmail, PASSWORD)
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.email").exists();
	}

	@ParameterizedTest
	@MethodSource("invalidPasswords")
	void ac1_4_a_password_that_is_too_short_or_too_long_is_rejected(String invalidPassword) {
		register(username, email, invalidPassword)
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.password").exists();
	}

	static Stream<String> invalidPasswords() {
		return Stream.of("1234567", "a".repeat(73), "가".repeat(25));
	}

	@ParameterizedTest
	@MethodSource("validPasswords")
	void ac1_4_a_password_at_the_limits_is_accepted(String validPassword) {
		register(username, email, validPassword).expectStatus().isCreated();
	}

	static Stream<String> validPasswords() {
		return Stream.of("12345678", "a".repeat(72), "가".repeat(24));
	}

	@Test
	void ac1_5_an_email_that_already_exists_is_rejected() {
		register(username, email, PASSWORD).expectStatus().isCreated();

		register("other-" + id, email, PASSWORD)
				.expectStatus().isEqualTo(409)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.errors.email").exists();
	}

	/** 같은 값의 가입이 동시에 와도 회원은 하나다. 나머지는 모두 409다. 500은 없다. */
	@Test
	void ac1_5_ac1_6_registrations_at_the_same_time_create_one_member() throws Exception {
		int requests = 12;
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
			for (int i = 0; i < requests; i++) {
				results.add(executor.submit(() -> {
					start.await();
					return register(username, email, PASSWORD).returnResult().getStatus().value();
				}));
			}
			start.countDown();
		}

		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> result : results) {
			statuses.add(result.get());
		}
		assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
		assertThat(statuses).filteredOn(status -> status == 409).hasSize(requests - 1);
	}

	@Test
	void ac1_6_a_username_that_already_exists_is_rejected() {
		register(username, email, PASSWORD).expectStatus().isCreated();

		register(username, "other-" + id + "@example.com", PASSWORD)
				.expectStatus().isEqualTo(409)
				.expectBody()
				.jsonPath("$.errors.username").exists();
	}

	@Test
	void ac1_7_the_result_of_a_registration_has_only_username_and_email() {
		String body = bodyOf(register(username, email, PASSWORD).expectStatus().isCreated());

		assertThat(new JsonMapper().readValue(body, Map.class).keySet()).containsExactlyInAnyOrder("username", "email");
		assertThat(body).doesNotContain(PASSWORD).doesNotContain(storedPasswordHash());
	}

	@Test
	void ac1_7_a_rejection_has_no_password_and_no_password_hash() {
		register(username, email, PASSWORD).expectStatus().isCreated();

		String invalid = bodyOf(register("other-" + id, "not-an-email", PASSWORD).expectStatus().isBadRequest());
		String tooShort = bodyOf(register("other-" + id, "other-" + id + "@example.com", "short-1").expectStatus().isBadRequest());
		String duplicate = bodyOf(register(username, email, PASSWORD).expectStatus().isEqualTo(409));

		assertThat(invalid).doesNotContain(PASSWORD);
		assertThat(tooShort).doesNotContain("short-1");
		assertThat(duplicate).doesNotContain(PASSWORD).doesNotContain(storedPasswordHash());
	}

	@Test
	void ac1_8_an_email_that_differs_only_in_case_already_exists() {
		register(username, email, PASSWORD).expectStatus().isCreated();

		register("other-" + id, email.toUpperCase(), PASSWORD)
				.expectStatus().isEqualTo(409)
				.expectBody()
				.jsonPath("$.errors.email").exists();
	}

	/** plan 01의 작업 4: 저장된 값이 비밀번호의 원문이 아니다. (R5) */
	@Test
	void r5_the_stored_value_is_not_the_raw_password() {
		register(username, email, PASSWORD).expectStatus().isCreated();

		assertThat(storedPasswordHash()).isNotBlank().doesNotContain(PASSWORD);
	}

	/** ADR-0005: 오류의 본문은 Problem Details 형식이다. */
	@Test
	void adr0005_a_body_that_is_not_json_is_rejected_as_problem_details() {
		client.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).body("not json").exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	private RestTestClient.ResponseSpec register(String username, String email, String password) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("username", username);
		body.put("email", email);
		body.put("password", password);
		return post(body);
	}

	private Map<String, String> validBody() {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("username", username);
		body.put("email", email);
		body.put("password", PASSWORD);
		return body;
	}

	private RestTestClient.ResponseSpec post(Map<String, String> body) {
		return client.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	private static String bodyOf(RestTestClient.ResponseSpec response) {
		return response.expectBody(String.class).returnResult().getResponseBody();
	}

	private String storedPasswordHash() {
		return new JdbcTemplate(dataSource).queryForObject("select password_hash from member where email = ?",
				String.class, email);
	}

	@Test
	void ac1_9_a_username_longer_than_50_characters_is_rejected_and_the_field_is_named() {
		register("a".repeat(51 - id.length()) + id, email, PASSWORD)
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.errors.username").exists()
				.jsonPath("$.errors.length()").isEqualTo(1);
	}

	@Test
	void ac1_9_an_email_longer_than_254_characters_is_rejected_and_the_field_is_named() {
		register(username, emailOfLength(255), PASSWORD)
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.errors.email").exists()
				.jsonPath("$.errors.length()").isEqualTo(1);
	}

	/** 한계의 길이는 받아들인다. 저장도 된다. (AC-1.1) */
	@Test
	void ac1_9_a_username_of_50_characters_and_an_email_of_254_characters_are_accepted() {
		String longestUsername = "a".repeat(50 - id.length()) + id;
		String longestEmail = emailOfLength(254);

		register(longestUsername, longestEmail, PASSWORD)
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.username").isEqualTo(longestUsername)
				.jsonPath("$.email").isEqualTo(longestEmail);
	}

	private String emailOfLength(int length) {
		String end = "-" + id + "@example.com";
		return "a".repeat(length - end.length()) + end;
	}

}
