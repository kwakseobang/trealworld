package trealworld;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * ADR-0002의 규칙 1~5. 이름이 rule로 시작한다.
 * 다른 ADR의 "절대 ~해서는 안 된다"와 "반드시 ~해야 한다" 중에서 의존으로 확인하는 것. 이름이 adr로 시작한다.
 */
class ArchitectureTests {

	private static final String DOMAIN = "trealworld.domain..";
	private static final String APPLICATION = "trealworld.application..";
	private static final String INCOMING_PORT = "trealworld.application.port.in..";
	private static final String ADAPTER = "trealworld.adapter..";
	private static final String WEB_ADAPTER = "trealworld.adapter.web..";

	private static final JavaClasses classes = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("trealworld");

	private static final JavaClasses testClasses = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
			.importPackages("trealworld");

	@Test
	void rule1_domain_does_not_depend_on_application_or_adapter() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTER)
				.as("ADR-0002 규칙 1: domain은 application과 adapter에 의존하지 않는다")
				.check(classes);
	}

	@Test
	void rule2_application_does_not_depend_on_adapter() {
		noClasses().that().resideInAPackage(APPLICATION)
				.should().dependOnClassesThat().resideInAPackage(ADAPTER)
				.as("ADR-0002 규칙 2: application은 adapter에 의존하지 않는다")
				.check(classes);
	}

	@Test
	void rule3_domain_does_not_depend_on_spring() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().dependOnClassesThat().resideInAPackage("org.springframework..")
				.as("ADR-0002 규칙 3: domain은 Spring에 의존하지 않는다")
				.check(classes);
	}

	@Test
	void rule4_adapters_do_not_depend_on_each_other() {
		slices().matching("trealworld.adapter.(*)..")
				.should().notDependOnEachOther()
				.as("ADR-0002 규칙 4: adapter끼리는 서로 의존하지 않는다")
				.check(classes);
	}

	@Test
	void rule5_web_adapter_uses_application_only_through_incoming_ports() {
		noClasses().that().resideInAPackage(WEB_ADAPTER)
				.should().dependOnClassesThat(resideInAPackage(APPLICATION).and(not(resideInAPackage(INCOMING_PORT))))
				.as("ADR-0002 규칙 5: 웹 어댑터는 들어오는 포트를 통해서만 application을 쓴다")
				.check(classes);
	}

	@Test
	void adr0002_domain_depends_only_on_java_jpa_annotations_and_lombok() {
		classes().that().resideInAPackage(DOMAIN)
				.should().onlyDependOnClassesThat()
				.resideInAnyPackage(DOMAIN, "java..", "jakarta.persistence..", "lombok..")
				.as("ADR-0002: domain은 Java, JPA 애너테이션, Lombok에만 의존할 수 있다")
				.check(classes);
	}

	/** ADR-0002가 domain에 허용한 것은 JPA의 매핑 애너테이션이다. EntityManager 같은 실행 API가 아니다. */
	@Test
	void adr0002_domain_uses_only_the_mapping_annotations_of_jpa() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().dependOnClassesThat(resideInAPackage("jakarta.persistence..")
						.and(not(JavaClass.Predicates.ANNOTATIONS)).and(not(JavaClass.Predicates.ENUMS)))
				.as("ADR-0002: domain은 JPA의 애너테이션과 그 속성의 enum만 쓴다")
				.check(classes);
	}

	/** 규칙 4와 5는 패키지로 어댑터를 가른다. 그 패키지의 밖에 둔 어댑터는 규칙을 빠져나간다. */
	@Test
	void adr0002_every_adapter_is_in_one_of_the_adapter_packages() {
		classes().that().resideInAPackage(ADAPTER).and().doNotHaveSimpleName("package-info")
				.should().resideInAnyPackage(WEB_ADAPTER, "trealworld.adapter.persistence..",
						"trealworld.adapter.security..")
				.as("ADR-0002: 어댑터는 web, persistence, security의 안에 둔다")
				.allowEmptyShould(true)
				.check(classes);
		classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
				.should().resideInAPackage(WEB_ADAPTER)
				.as("ADR-0002: 컨트롤러는 웹 어댑터에 둔다")
				.check(classes);
	}

	/** application은 Spring을 쓰지 않는다. 이 규칙이 그것도 지킨다. */
	@Test
	void adr0002_application_depends_only_on_java_and_domain() {
		classes().that().resideInAPackage(APPLICATION)
				.should().onlyDependOnClassesThat().resideInAnyPackage(APPLICATION, DOMAIN, "java..")
				.as("ADR-0002: application은 domain에만 의존할 수 있다. Spring을 쓰지 않는다")
				.check(classes);
	}

	@Test
	void adr0002_domain_has_no_interfaces() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().beInterfaces()
				.as("ADR-0002: domain에는 인터페이스를 두지 않는다. 포트는 application에 둔다")
				.check(classes);
	}

	@Test
	void adr0001_bean_validation_is_not_used() {
		noClasses().should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
				.as("ADR-0001: Bean Validation은 쓰지 않는다. 값의 규칙은 domain에 둔다")
				.check(classes);
	}

	@Test
	void adr0003_spring_security_and_the_jwt_library_are_only_in_the_security_adapter() {
		noClasses().that().resideOutsideOfPackage("trealworld.adapter.security..")
				.should().dependOnClassesThat().resideInAnyPackage("org.springframework.security..", "com.nimbusds..")
				.as("ADR-0003: Spring Security와 JWT 라이브러리는 adapter의 보안 부분에만 둔다")
				.check(classes);
	}

	@Test
	void adr0003_the_current_time_is_read_only_from_the_injected_clock() {
		noClasses().should().callMethod(Instant.class, "now")
				.orShould().callMethod(LocalDate.class, "now")
				.orShould().callMethod(LocalDateTime.class, "now")
				.orShould().callMethod(OffsetDateTime.class, "now")
				.orShould().callMethod(ZonedDateTime.class, "now")
				.orShould().callMethod(System.class, "currentTimeMillis")
				.orShould().callConstructor(java.util.Date.class)
				.as("ADR-0003: 현재 시각은 주입하는 Clock에서 읽는다")
				.check(classes);
	}

	@Test
	void adr0004_domain_unit_tests_do_not_use_spring() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().dependOnClassesThat().resideInAPackage("org.springframework..")
				.as("ADR-0004: 도메인 단위 테스트는 Spring을 쓰지 않는다")
				.check(testClasses);
	}

	@Test
	void adr0004_every_test_name_has_an_id() {
		methods().that().areAnnotatedWith(Test.class).or().areAnnotatedWith(ParameterizedTest.class)
				.should().haveNameMatching("(ac\\d_\\d|r\\d+|rule\\d|adr\\d{4})_.+")
				.as("ADR-0004: 테스트의 이름에는 AC, R, ADR의 ID가 있다")
				.check(testClasses);
	}

}
