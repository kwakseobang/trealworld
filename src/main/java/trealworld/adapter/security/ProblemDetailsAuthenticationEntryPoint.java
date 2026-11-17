package trealworld.adapter.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * 인증에 실패한 요청에 401을 응답한다. 본문은 Problem Details 형식이다. (ADR-0003, ADR-0005)
 * 컨트롤러의 예외 처리는 필터에 닿지 않는다. 그래서 여기에서 본문을 만든다.
 * 실패의 이유(토큰이 없다, 위조됐다, 만료됐다, 회원이 없다)를 본문에 넣지 않는다.
 */
@Component
class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final JsonMapper jsonMapper;

	ProblemDetailsAuthenticationEntryPoint(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		Map<String, Object> problem = new LinkedHashMap<>();
		problem.put("detail", "액세스 토큰이 없거나 유효하지 않다");
		problem.put("instance", request.getRequestURI());
		problem.put("status", HttpStatus.UNAUTHORIZED.value());
		problem.put("title", HttpStatus.UNAUTHORIZED.getReasonPhrase());

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		// JsonMapper는 OutputStream에 UTF-8로 쓴다
		jsonMapper.writeValue(response.getOutputStream(), problem);
	}

}
