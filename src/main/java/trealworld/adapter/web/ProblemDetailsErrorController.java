package trealworld.adapter.web;

import java.net.URI;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 오류의 화면. 컨트롤러에 닿지 않은 요청의 오류를 Problem Details로 바꾼다. (ADR-0005)
 * 서버는 그런 오류를 이 경로로 넘긴다. 예: 필터의 안에서 난 예외(500), 서버가 받아들이지 않는 경로(400).
 * 컨트롤러의 안에서 난 오류는 ApiExceptionHandler가 처리한다. 여기에 오지 않는다.
 * 이 클래스가 있으면 Spring Boot의 기본 오류 화면은 쓰이지 않는다.
 */
@RestController
class ProblemDetailsErrorController implements ErrorController {

	@RequestMapping("/error")
	ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
		HttpStatus status = statusOf(request);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detailOf(status));
		URI instance = instanceOf(request);
		if (instance != null) {
			problem.setInstance(instance);
		}
		return ResponseEntity.status(status).body(problem);
	}

	/** 오류가 없는데 이 경로를 직접 부른 요청에는 상태가 없다. 그런 자원은 없다 (404). */
	private static HttpStatus statusOf(HttpServletRequest request) {
		Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		HttpStatus status = (code instanceof Integer value) ? HttpStatus.resolve(value) : null;
		return (status != null) ? status : HttpStatus.NOT_FOUND;
	}

	/** 글은 고정이다. 원인을 넣지 않는다. 원인의 글에 저장된 값이 들어 있는 경우가 있다. (AC-1.7) */
	private static String detailOf(HttpStatus status) {
		if (status.is5xxServerError()) {
			return "서버에서 오류가 났다";
		}
		if (status == HttpStatus.NOT_FOUND) {
			return "요청한 자원이 없다";
		}
		return "요청을 받아들일 수 없다";
	}

	/** 오류가 난 요청의 경로. /error가 아니다. */
	private static URI instanceOf(HttpServletRequest request) {
		Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		if (!(uri instanceof String path)) {
			return null;
		}
		try {
			return URI.create(path);
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

}
