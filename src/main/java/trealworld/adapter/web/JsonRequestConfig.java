package trealworld.adapter.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * 요청의 JSON을 읽는 방법. (ADR-0005)
 */
@Configuration(proxyBeanMethods = false)
class JsonRequestConfig {

	/**
	 * 요청의 값은 JSON의 문자열이다. 숫자와 참거짓을 글자로 바꿔서 받지 않는다.
	 * 바꿔서 받으면 {"password": 12345678}이 비밀번호 "12345678"이 된다.
	 * 문자열이 아닌 값은 읽지 못하는 본문이다 (400).
	 */
	@Bean
	JsonMapperBuilderCustomizer stringsAreReadOnlyFromJsonStrings() {
		return builder -> builder.withCoercionConfig(LogicalType.Textual, config -> config
				.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
	}

}
