package trealworld;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * ADR-0002의 규칙 1~5.
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
				.allowEmptyShould(true)
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
				.allowEmptyShould(true)
				.check(classes);
	}

	@Test
	void rule5_web_adapter_uses_application_only_through_incoming_ports() {
		noClasses().that().resideInAPackage(WEB_ADAPTER)
				.should().dependOnClassesThat(resideInAPackage(APPLICATION).and(not(resideInAPackage(INCOMING_PORT))))
				.as("ADR-0002 규칙 5: 웹 어댑터는 들어오는 포트를 통해서만 application을 쓴다")
				.allowEmptyShould(true)
				.check(classes);
	}

}
