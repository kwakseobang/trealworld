package trealworld.adapter.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import trealworld.application.port.in.DuplicateMemberException;
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

}
