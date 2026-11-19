plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("info.solidsoft.pitest") version "1.19.0"
}

group = "trealworld"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.security:spring-security-crypto")
	implementation("org.springframework.security:spring-security-oauth2-resource-server")
	implementation("org.springframework.security:spring-security-oauth2-jose")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.h2database:h2")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("com.tngtech.archunit:archunit:1.5.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Mutation testing. 도구가 코드에 결함을 넣고, 테스트가 그 결함을 잡는지 본다. (ADR-0004)
// 대상은 Spring 없이 도는 테스트가 있는 곳이다: domain, application.
// 실행: ./gradlew pitest. 보고서: build/reports/pitest/index.html
pitest {
	pitestVersion.set("1.30.2")
	junit5PluginVersion.set("1.2.3")
	targetClasses.set(listOf("trealworld.domain.*", "trealworld.application.*"))
	targetTests.set(listOf("trealworld.domain.*", "trealworld.application.*"))
	threads.set(4)
	outputFormats.set(listOf("HTML", "XML"))
	timestampedReports.set(false)
	// 살아남은 결함이 1개라도 있으면 빌드가 실패한다.
	// 죽일 수 없는 결함(동치)이 생기면 코드를 단순하게 고친다. 고칠 수 없으면 여기서 그 결함만 뺀다.
	mutationThreshold.set(100)
}

// ./gradlew build와 check가 mutation testing을 함께 돌린다.
tasks.named("check") {
	dependsOn("pitest")
}
