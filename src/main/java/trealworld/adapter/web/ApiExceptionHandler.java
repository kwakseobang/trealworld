package trealworld.adapter.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import trealworld.application.port.in.DuplicateMemberException;
import trealworld.application.port.in.InvalidCredentialsException;
import trealworld.application.port.in.InvalidValuesException;

/**
 * 오류를 Problem Details(RFC 9457)로 바꾼다. (ADR-0005)
 * Spring MVC가 던지는 오류는 부모 클래스가 같은 형식으로 바꾼다.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	/** 값이 비었거나 형식이 틀리다 → 400. 어느 값이 문제인지 errors에 넣는다. */
	@ExceptionHandler(InvalidValuesException.class)
	ProblemDetail invalidValues(InvalidValuesException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "값이 올바르지 않다");
		problem.setProperty("errors", exception.getErrors());
		return problem;
	}

	/** 이메일이나 사용자 이름이 이미 있다 → 409. */
	@ExceptionHandler(DuplicateMemberException.class)
	ProblemDetail duplicateMember(DuplicateMemberException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setProperty("errors", Map.of(exception.getField(), exception.getMessage()));
		return problem;
	}

	/** 로그인 실패 → 401. 이유(없는 이메일, 틀린 비밀번호)를 구분하지 않는다. (R9) */
	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail invalidCredentials(InvalidCredentialsException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
	}

	/**
	 * 예상하지 못한 오류 → 500. 본문은 같은 형식이다. (ADR-0005)
	 * 원인은 로그에만 남긴다. 본문에 넣지 않는다. 원인의 글에 저장된 값이 들어 있는 경우가 있다. (AC-1.7)
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail unexpected(Exception exception) {
		logger.error("예상하지 못한 오류", exception);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 났다");
	}

}
