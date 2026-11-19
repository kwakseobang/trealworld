# 아키텍처와 코드 리뷰

- 리뷰어: codex (gpt-6.2-astra), 날짜: 2026-11-19, 대상 커밋: `a088664`

## 요약

우선 고칠 곳은 회원 저장과 JWT 만료 검사다. 도메인이 허용한 50자 이름을 H2가 거부하고, JWT 검사기는 필수인 만료 시각을 생략해도 허용한다.
가입 경쟁에서 중복 저장은 DB가 막는다. 남은 문제는 저장 실패의 원인을 유스케이스가 다시 조회해서 알아내야 한다는 점이다.
소스, 테스트, 고정된 라이브러리 버전을 정적으로 검토했다. Gradle과 서버는 실행하지 않았다.

## 발견

### ARCH-01. 도메인이 허용한 50자 사용자 이름을 H2가 저장하지 못한다

- 심각도: 높음
- 종류: 확인
- 위치: `docs/domain/01-member.md:63`, `src/main/java/trealworld/domain/Username.java:19`, `src/main/java/trealworld/domain/Email.java:23`, `src/main/java/trealworld/domain/Member.java:34`, `src/main/java/trealworld/domain/Member.java:38`, `src/test/java/trealworld/domain/UsernameTests.java:57`, `src/test/java/trealworld/adapter/persistence/MemberPersistenceAdapterTests.java:105`
- 내용: R2·R3은 코드 포인트로 길이를 센다. H2 2.4.240은 `VARCHAR(n)`에 값을 넣을 때 UTF-16 코드 단위로 길이를 센다. 이모지 50개는 도메인에서 50자이고 H2에서는 100단위다. 따라서 유효한 이름으로 가입해도 AC-1.1의 회원을 만들 수 없다. 이메일 열에도 같은 차이가 있다(AC-1.9).
- 근거: 도메인 테스트는 `new Username("😀".repeat(50))`을 허용한다. 열은 `length = Username.MAX_CHARACTERS`이고, 저장 테스트는 한글 BMP 문자와 ASCII로만 상한을 채운다. H2는 열의 길이를 `s.length() > p`로 검사한다. [H2 2.4.240의 VARCHAR 변환](https://github.com/h2database/h2database/blob/version-2.4.240/h2/src/main/org/h2/value/Value.java#L1213-L1230)
- 제안: 도메인의 50·254 코드 포인트 제한을 유지하고, H2 열에 각각 최대 100·508 UTF-16 단위를 담을 공간을 준다. 보충 평면 문자가 든 최장값을 가입한 뒤 저장소에서 그대로 읽는 테스트를 추가한다.

### ARCH-02. JWT 검사기가 만료 시각의 누락과 만료 순간을 허용한다

- 심각도: 높음
- 종류: 확인
- 위치: `docs/adr/0003-authentication.md:16`, `docs/adr/0003-authentication.md:34`, `src/main/java/trealworld/adapter/security/JwtAccessTokenIssuer.java:44`, `src/main/java/trealworld/adapter/security/SecurityConfig.java:64`, `src/main/java/trealworld/adapter/security/MemberAuthenticationConverter.java:36`, `src/test/java/trealworld/adapter/security/AccessTokenDecoderTests.java:49`
- 내용: 발급기는 항상 `exp`를 넣는다. 수신 측은 서명이 유효하고 회원이 존재할 때 `exp`의 존재를 별도로 요구하지 않는다. 또 테스트가 `now == exp`에서 인증 성공을 기대한다. ADR-0003은 만료 시각을 필수로 하고 여유 시간을 0으로 정했다. AC-3.3과 JWT의 만료 경계를 함께 지키려면 현재 시각이 만료 시각보다 빨라야 한다.
- 근거: `new JwtTimestampValidator(Duration.ZERO)`에 `setClock(clock)`만 적용한다. Spring Security 7.1.2에서 `allowEmptyExpiryClaim`의 기본값은 `true`다. 테스트는 발급 1시간 뒤에도 `decode(token)).isNotNull()`을 기대한다. [Spring Security API](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/oauth2/jwt/JwtTimestampValidator.html#setAllowEmptyExpiryClaim(boolean)), [RFC 7519 §4.1.4](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.4)
- 제안: 검사기에 `setAllowEmptyExpiryClaim(false)`를 적용하고, 주입한 `Clock`으로 `now < exp`를 검사한다. 만료 직전만 통과하도록 기존 테스트를 수정하고, 만료 시각이 없는 경우도 거부하는지 확인한다.

### ARCH-03. 이메일을 소문자로 바꾸면 길이와 대소문자 동등성이 달라진다

- 심각도: 중간
- 종류: 확인
- 위치: `src/main/java/trealworld/domain/Email.java:23`, `src/main/java/trealworld/domain/Email.java:26`, `src/main/java/trealworld/adapter/persistence/MemberPersistenceAdapter.java:47`, `src/main/java/trealworld/adapter/persistence/MemberPersistenceAdapter.java:52`, `src/test/java/trealworld/domain/EmailTests.java:35`, `src/test/java/trealworld/domain/EmailTests.java:40`
- 내용: 입력이 정확히 254 코드 포인트여도 `İ`가 있으면 소문자 변환 뒤에는 255개 이상이 된다. 이렇게 만든 `Email`은 자기 값으로 다시 생성하면 길이 검사에 실패한다(R2, AC-1.9). 또한 그리스 문자 `Σ`와 `ς`로 각각 시작하는 한 글자 로컬 파트는 대소문자만 바꾼 관계여도 비교 키가 달라진다. 가입의 유일성(R6, AC-1.8)과 로그인 조회(AC-2.5)가 모두 이 키를 쓴다.
- 근거: `codePointCount(...)` 검사 다음에 `value.toLowerCase(Locale.ROOT)`를 호출한다. `İ`의 소문자는 `i`와 결합점 두 코드 포인트다. 단독 `Σ`의 소문자는 `σ`이고 `ς`는 그대로 남는다. Unicode의 대소문자 무시 비교에서는 두 시그마가 같은 값이다. [Unicode 소문자 매핑](https://www.unicode.org/Public/16.0.0/ucd/SpecialCasing.txt), [Unicode Case Folding](https://www.unicode.org/Public/16.0.0/ucd/CaseFolding.txt)
- 제안: R2에 입력과 정규형의 길이 상한을 명시하고 정규형도 검사한다. Unicode 주소를 지원하는 현재 범위에서는 가입과 로그인이 같은 case folding 규칙을 쓰게 한다. 대소문자 변형의 동등성, 중복 가입, 로그인, 정규형 재생성을 함께 검증한다.

### ARCH-04. 기본 로그아웃 필터가 공개 경로를 하나 더 만든다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:72`, `docs/adr/0003-authentication.md:35`, `src/main/java/trealworld/adapter/security/SecurityConfig.java:39`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:235`
- 내용: ADR-0003은 가입과 로그인 외의 클라이언트 요청에 인증을 요구한다. Spring Security의 기본 `LogoutFilter`는 인가 필터보다 먼저 `/logout`을 처리한다. 현재 설정에서는 익명 요청에도 기본 로그아웃 성공 리다이렉트가 적용된다. 이 리다이렉트는 이미 발급한 JWT를 취소하지도 않는다.
- 근거: 체인은 `csrf(AbstractHttpConfigurer::disable)`와 `anyRequest().authenticated()`를 설정하고 기본 로그아웃은 켜 둔다. 경로별 인가 테스트 목록에는 `/logout`이 없다. [Spring Security 로그아웃 필터의 순서와 기본 동작](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html)
- 제안: 이 슬라이스의 필터 체인에서 `logout(AbstractHttpConfigurer::disable)`을 적용한다. 익명의 `/logout` 요청도 다른 비공개 경로와 같은 401 Problem Details를 받는지 수용 테스트에 넣는다.

### ARCH-05. 저장 어댑터가 모든 무결성 오류를 회원 중복으로 번역한다

- 심각도: 중간
- 종류: 확인
- 위치: `src/main/java/trealworld/application/port/out/MemberRepository.java:16`, `src/main/java/trealworld/adapter/persistence/MemberPersistenceAdapter.java:33`, `src/main/java/trealworld/application/service/RegisterMemberService.java:48`, `src/test/java/trealworld/application/service/RegisterMemberServiceTests.java:150`, `docs/adr/0005-api-conventions.md:24`
- 내용: 나가는 포트에서 `MemberNotUniqueException`은 이메일이나 사용자 이름의 중복을 뜻한다(R6·R7). 구현은 길이 초과 등 다른 무결성 오류에도 이 예외를 던진다. 유스케이스는 실패 원인을 알아내려고 이메일과 사용자 이름을 다시 조회한다. 그 시점에 값이 존재한다는 사실만으로 원래 실패한 제약을 식별할 수는 없다. 이 구분은 409와 500을 결정한다(ADR-0005).
- 근거: 어댑터는 `catch (DataIntegrityViolationException exception)` 전체를 변환한다. 서비스는 `rejectDuplicate(email, username)`를 다시 실행한 뒤에야 `IllegalStateException`을 던진다. 단위 테스트도 중복이 아닌 실패의 원인으로 `MemberNotUniqueException`을 기대한다.
- 제안: 두 유일 제약에 안정적인 이름을 주고, 영속성 어댑터에서 해당 제약 위반만 충돌 필드와 함께 번역한다. 서비스는 그 필드를 들어오는 포트의 중복 예외로 옮기고, 나머지 저장 실패는 서버 오류로 전달한다. 기존 DB 유일 제약과 동시 가입 테스트는 유지한다.

### ARCH-06. 이메일 검사가 빈 도메인 레이블과 Unicode 공백을 허용한다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/domain/01-member.md:51`, `src/main/java/trealworld/domain/Email.java:17`, `src/test/java/trealworld/domain/EmailTests.java:23`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:98`
- 내용: `a@example..com`은 빈 도메인 레이블이 있어도 통과한다. 전각 공백(U+3000)이 포함된 도메인도 통과한다. 가입과 로그인은 같은 값 객체를 쓰므로 둘 다 잘못된 이메일을 값 오류로 분류하지 못한다(R2, AC-1.3, AC-2.4).
- 근거: `[^@\\s]+@[^@\\s]+\\.[^@\\s]+`에서 도메인 문자 집합은 점도 허용한다. Unicode 옵션이 없는 Java 정규식의 `\s`에는 전각 공백이 없다. 기존 형식 테스트에는 ASCII 공백과 누락된 부분만 있다.
- 제안: 이 서비스가 받는 주소 형태를 R2에 명시한다. 도메인을 레이블 단위로 검사하고 빈 레이블과 공백을 거부한다. 가입·로그인 모두에서 `errors.email`을 확인한다.

### ARCH-07. 줄 바꿈 없는 공백만으로 사용자 이름을 만들 수 있다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/domain/01-member.md:52`, `src/main/java/trealworld/domain/Username.java:15`, `src/main/java/trealworld/domain/Username.java:18`, `src/test/java/trealworld/domain/UsernameTests.java:18`, `src/test/java/trealworld/domain/UsernameTests.java:25`
- 내용: U+00A0(NBSP) 하나가 빈 이름 검사를 통과한다. 이름 양끝의 NBSP도 남으므로 같은 이름의 양끝에 이 공백만 붙여 별도의 유일 키를 만들 수 있다. R3의 ‘앞뒤 공백’과 AC-1.2의 ‘비어 있음’이 사용자에게 보이는 이름과 다르게 적용된다.
- 근거: `isBlank()`와 `strip()`은 모두 `Character.isWhitespace`의 정의를 따른다. Java는 이 정의에서 NBSP를 제외한다. 테스트에는 일반 공백, 탭, 줄 바꿈만 있다. [Java 25의 공백 정의](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Character.html#isWhitespace(int))
- 제안: R3에 지울 공백의 범위를 정하고 양끝 제거와 빈 이름 검사에 같은 판정을 쓴다. NBSP만 있는 이름, 양끝에 붙은 이름, 허용한 내부 공백을 각각 테스트한다.

### ARCH-08. JSON 필드의 타입이 틀리면 400에서 필드 이름이 사라진다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/adr/0005-api-conventions.md:37`, `src/main/java/trealworld/adapter/web/RegisterMemberRequest.java:6`, `src/main/java/trealworld/adapter/web/ApiExceptionHandler.java:20`, `src/main/java/trealworld/adapter/web/ApiExceptionHandler.java:26`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:215`
- 내용: 올바른 JSON에서 `email` 자리에 객체가 오면 문자열로 읽을 수 없어 MVC가 유스케이스 호출 전에 거부한다. 이 경로의 400에는 ADR-0005가 필드 값 오류에 요구하는 `errors.email`이 없다. 문법이 깨져 어느 필드인지 알 수 없는 본문과 구별할 수 있는 경우다(AC-1.3, AC-2.4).
- 근거: `problem.setProperty("errors", ...)`는 애플리케이션 예외에만 적용된다. 상속한 `handleHttpMessageNotReadable`는 일반 `ProblemDetail`을 만든다. 기존 JSON 읽기 실패 테스트는 상태와 Content-Type만 검사한다. [Spring MVC의 기본 처리](https://github.com/spring-projects/spring-framework/blob/v7.0.10/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/ResponseEntityExceptionHandler.java#L505-L510)
- 제안: 웹 어댑터에서 이 처리를 재정의한다. Jackson이 식별한 요청 필드에는 고정된 타입 오류 사유를 넣는다. 문법 오류의 본문 규약은 따로 명시한다. 원래 입력값과 예외 메시지는 응답에서 제외한다.

### ARCH-09. 큰 JSON 본문은 도메인의 길이 검사보다 먼저 메모리를 사용한다

- 심각도: 중간
- 종류: 확인
- 위치: `src/main/java/trealworld/adapter/web/MemberController.java:34`, `src/main/java/trealworld/adapter/web/AuthController.java:23`, `src/main/java/trealworld/domain/Email.java:20`, `src/main/java/trealworld/domain/Password.java:16`, `src/main/resources/application.properties:1`, `docs/adr/0005-api-conventions.md:30`
- 내용: 두 경로 모두 Jackson이 DTO의 문자열을 만든 뒤에 R2·R3·R4를 검사한다. 이메일은 길이보다 형식을 먼저 검사하고, 비밀번호는 UTF-8 바이트 배열을 추가로 만든다. 프로젝트에는 JSON 본문 전체의 읽기 상한이 없다. 필드 제한(AC-1.4, AC-1.9)만으로는 이 선행 비용을 제한할 수 없다. 배포 환경의 외부 제한과 실제 메모리 사용량은 측정하지 않았다.
- 근거: 두 컨트롤러는 `@RequestBody`를 쓰고 비밀번호는 `getBytes(StandardCharsets.UTF_8).length`를 검사한다. Jackson 3.1.7의 기본 전체 문서 길이 제한은 `-1`이다. 개별 문자열과 중첩 깊이에는 별도 제한이 있다. [Jackson 읽기 제한](https://github.com/FasterXML/jackson-core/blob/jackson-core-3.1.7/src/main/java/tools/jackson/core/StreamReadConstraints.java)
- 제안: 정상 요청의 JSON 이스케이프와 인코딩을 수용하는 본문 상한을 정한다. 요청 스트림에서 실제로 읽는 바이트 수를 제한하고 초과 시 413 Problem Details를 반환한다. `Content-Length` 유무와 관계없이 같은 상한을 검증한다.

### ARCH-10. 요청 필드의 JSON 타입과 추가 필드 처리가 Jackson 기본값에 맡겨져 있다

- 심각도: 중간
- 종류: 확인
- 위치: `src/main/java/trealworld/adapter/web/RegisterMemberRequest.java:6`, `src/main/java/trealworld/adapter/web/LoginRequest.java:6`, `src/main/resources/application.properties:1`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:221`, `src/test/java/trealworld/acceptance/LoginAcceptanceTests.java:184`, `docs/adr/0005-api-conventions.md:30`
- 내용: Jackson 3.1.7은 숫자를 문자열 필드로 변환한다. 클라이언트가 비밀번호를 JSON 숫자로 보내면 서버는 그 숫자의 문자열 표현에 R4를 적용한다. 알 수 없는 필드도 기본적으로 무시한다. ADR-0005는 JSON을 요구하지만 두 동작의 계약은 정하지 않았다.
- 근거: 요청은 `String password`를 받고 수용 테스트 도우미는 `Map<String, String>`만 보낸다. Jackson의 문자열 역직렬화는 숫자에 `TryConvert`를 적용하며, `FAIL_ON_UNKNOWN_PROPERTIES`의 기본값은 `false`다.
- 제안: 자격 증명에는 JSON 문자열만 허용하도록 문자열 강제 변환을 제한한다. 추가 필드를 허용할지 거부할지는 ADR-0005에 명시한다. 숫자 필드와 추가 필드를 독립된 수용 사례로 둔다.

### ARCH-11. 공백으로 잘못 설정한 서명 키가 시작 오류로 드러나지 않는다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/adr/0003-authentication.md:24`, `docs/adr/0003-authentication.md:27`, `src/main/java/trealworld/adapter/security/JwtSigningKey.java:30`, `src/main/java/trealworld/adapter/security/JwtSigningKey.java:39`, `src/test/java/trealworld/adapter/security/JwtSigningKeyTests.java:22`, `src/test/java/trealworld/adapter/security/JwtSigningKeyTests.java:29`
- 내용: 한 칸짜리 공백을 설정하면 32바이트 미만이지만 서버는 임의 키 생성 분기를 택한다. 여러 인스턴스에 같은 잘못된 설정을 배포하면 각 인스턴스가 서로 다른 키를 만든다. ADR-0003이 구분한 ‘키 미설정’과 ‘짧은 키 설정’의 운영 결과가 뒤바뀐다.
- 근거: `secret.isBlank()`가 `bytes.length < MIN_SECRET_BYTES` 검사보다 먼저다. 짧은 일반 문자열과 미설정에 해당하는 빈 문자열·`null`은 테스트하지만 공백 문자열은 테스트하지 않는다.
- 제안: 설정 누락 시의 임의 키 생성은 유지한다. 제공된 공백 문자열은 잘못된 설정으로 보고 시작을 거부한다. 빈 문자열을 누락으로 볼지도 ADR에 명시하고 설정 바인딩 테스트로 구분한다.

### ARCH-12. 보안 필터의 401 본문이 별도 맵으로 구현돼 있다

- 심각도: 낮음
- 종류: 확인
- 위치: `docs/adr/0005-api-conventions.md:36`, `docs/adr/0005-api-conventions.md:43`, `src/main/java/trealworld/adapter/security/ProblemDetailsAuthenticationEntryPoint.java:35`, `src/main/java/trealworld/adapter/web/ApiExceptionHandler.java:25`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:101`
- 내용: ADR-0005는 필터 오류에도 Spring의 `ProblemDetail`을 쓰도록 정했다. 보안 어댑터는 상태, 제목, 설명, 경로를 직접 조립한다. 현재 테스트가 확인하는 응답 필드는 제공하지만 공통 표현이 바뀌면 이 경로만 예전 형식으로 남을 수 있다.
- 근거: MVC는 `ProblemDetail.forStatusAndDetail(...)`을 쓰고 필터는 `new LinkedHashMap<>()`과 네 번의 `put`을 쓴다. RFC 9457에서 생략 가능한 `type` 자체는 결함이 아니다.
- 제안: 보안 어댑터 안에서 `ProblemDetail`을 만들고 기존 `JsonMapper`로 쓴다. `instance`, `WWW-Authenticate: Bearer`, 실패 사유마다 동일한 본문을 확인하는 기존 계약을 유지한다. 어댑터 간 의존을 추가할 필요는 없다.

### ARCH-13. 도메인의 ArchUnit 규칙이 JPA 실행 API까지 허용한다

- 심각도: 낮음
- 종류: 확인
- 위치: `docs/adr/0002-hexagonal-architecture.md:16`, `docs/adr/0002-hexagonal-architecture.md:56`, `src/test/java/trealworld/ArchitectureTests.java:83`, `src/main/java/trealworld/domain/Member.java:30`
- 내용: ADR-0002가 허용한 것은 JPA 매핑 애너테이션이다. 검사 대상에 `jakarta.persistence..` 전체를 넣어, 도메인이 `EntityManager`에 직접 의존해도 이 테스트는 통과하도록 작성돼 있다. 현재 도메인 코드는 매핑만 사용한다.
- 근거: 규칙 이름은 `domain_depends_only_on_java_jpa_annotations_and_lombok`이지만 판정은 `resideInAnyPackage(..., "jakarta.persistence..", ...)`다.
- 제안: 허용 대상을 JPA 애너테이션과 매핑에 쓰는 열거형으로 좁힌다. 현재 쓰는 `GenerationType.UUID`는 허용하고 실행 API 타입을 참조하는 위반 사례는 규칙이 잡도록 한다.

### ARCH-14. 재시작 테스트가 옛 토큰을 거부한 이유를 구별하지 못한다

- 심각도: 낮음
- 종류: 확인
- 위치: `docs/adr/0003-authentication.md:65`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:37`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:46`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:51`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:59`
- 내용: AC-3.4·AC-3.5에서 검증하려는 조건은 ‘재시작 뒤에도 서명은 유효하고, 옛 회원은 사라졌다’다. 설정 키를 무시하고 시작할 때마다 임의 키를 만들어도 첫 서버의 토큰은 401이고 두 번째 서버의 새 토큰은 성공한다. 기존 단언은 이 회귀에서도 같은 결과를 기대한다.
- 근거: 테스트는 두 번 모두 설정 키를 넘기지만 옛 토큰에 대해서는 `response.statusCode()).isEqualTo(401)`과 새 회원 이름이 응답에 없다는 점만 검사한다. `sub`가 서로 다른지도 직접 확인하지 않는다.
- 제안: 두 번째 서버의 `JwtDecoder`가 옛 토큰을 정상 해석하는지 먼저 확인한다. 이어서 옛 토큰과 새 토큰의 `sub`가 다르고, 옛 토큰을 사용한 HTTP 조회만 401인지 검증한다.

### ARCH-15. 테스트에서 쓰지 않는 JPA 테스트 모듈과 Lombok 처리기를 선언했다

- 심각도: 낮음
- 종류: 확인
- 위치: `build.gradle.kts:21`, `build.gradle.kts:29`, `build.gradle.kts:30`, `build.gradle.kts:31`, `build.gradle.kts:33`, `build.gradle.kts:35`, `src/test/java/trealworld/adapter/persistence/MemberPersistenceAdapterTests.java:23`, `docs/adr/0001-tech-stack.md:26`
- 내용: 영속성 테스트는 전체 `@SpringBootTest`를 사용한다. `src/test`에는 JPA 테스트 슬라이스 API나 Lombok 애너테이션 사용처가 없다. JPA 실행 의존성과 공통 테스트 지원은 이미 다른 starter가 제공한다(ADR-0001).
- 근거: `spring-boot-starter-data-jpa-test`, `testCompileOnly("org.projectlombok:lombok")`, `testAnnotationProcessor("org.projectlombok:lombok")`가 선언돼 있다. Lombok의 실제 사용처는 `src/main/java/trealworld/domain/Member.java:21`이다.
- 제안: 현재 테스트 방식에 맞게 위 세 선언을 제거하고, 이후 빌드에서 테스트 컴파일과 실행을 확인한다. 운영 코드가 쓰는 JPA와 Lombok 선언은 필요하다.

### ARCH-16. 비밀번호 해시의 기본 문자열 표현에 해시 전체가 들어간다

- 심각도: 낮음
- 종류: 확인
- 위치: `src/main/java/trealworld/domain/PasswordHash.java:9`, `src/main/java/trealworld/domain/Password.java:23`, `src/main/java/trealworld/application/port/in/AccessToken.java:17`, `src/main/java/trealworld/adapter/web/MemberResponse.java:8`, `src/test/java/trealworld/domain/PasswordHashTests.java:17`
- 내용: `Password`와 `AccessToken`은 진단 출력에서 값을 숨긴다. `PasswordHash`는 record의 기본 문자열 표현을 사용하므로 객체를 로그나 단언 실패 메시지에 출력하면 저장된 해시가 포함된다. 현재 HTTP 응답 DTO에서는 해시를 제외하므로 AC-1.7을 어기는 응답은 발견하지 않았다(R5, ADR-0005).
- 근거: `record PasswordHash(String value)`에는 생성자만 있다. `Password.toString()`은 고정 문자열 `"Password[****]"`을 반환한다.
- 제안: `PasswordHash.toString()`도 고정된 마스킹 문자열을 반환하게 한다. 진단 문자열에 해시가 들어가지 않는지 값 객체 테스트로 확인한다.

### ARCH-17. 영속성 컨텍스트가 MVC 요청이 끝날 때까지 열려 있다

- 심각도: 낮음
- 종류: 확인
- 위치: `src/main/resources/application.properties:1`, `src/main/java/trealworld/application/port/in/GetMemberUseCase.java:16`, `src/main/java/trealworld/adapter/web/MemberController.java:48`, `src/main/java/trealworld/adapter/web/MemberResponse.java:11`, `src/main/java/trealworld/domain/Member.java:33`
- 내용: Spring Boot 4.1.2의 MVC·JPA 기본 설정은 요청에 `EntityManager`를 묶는다. 조회 포트가 반환한 엔티티를 웹에서 DTO로 바꾸므로, 향후 지연 로딩 필드를 읽으면 포트 호출이 끝난 뒤에도 SQL이 실행될 수 있다. 현재 응답 매핑은 임베디드 값만 읽는다. ADR-0002의 패키지 규칙이 탐지하지 않는 실행 경계다.
- 근거: 프로젝트 설정에는 `spring.application.name`만 있다. Boot의 `JpaBaseConfiguration`은 `spring.jpa.open-in-view`가 없을 때도 `OpenEntityManagerInViewInterceptor`를 등록한다. 이는 영속성 컨텍스트의 수명에 관한 설정이다. DB 트랜잭션을 요청 전체에서 유지한다는 뜻은 아니다.
- 제안: 현재 응답에 필요한 값은 이미 읽으므로 `spring.jpa.open-in-view=false`를 명시한다. 이 설정으로 가입 경쟁과 조회 수용 테스트를 확인해, 웹 매핑이 열린 영속성 컨텍스트에 기대는지 검증한다.

## 확인했지만 문제가 없는 것

- 운영 코드의 의존 방향, 포트 위치, `UseCaseConfig` 조립, 어댑터 간 직접 의존, 도메인의 JPA 매핑 애너테이션 사용 — 발견 없음(ADR-0002).
- 동시 가입의 유일성 보장 — 발견 없음. 이메일과 사용자 이름에 각각 DB 유일 제약이 있고, 동시 요청 테스트는 201 한 건과 나머지 409를 기대한다(R6·R7).
- 값 객체의 `equals`·`hashCode` 구현 — 발견 없음. record가 보관한 값을 비교한다. 이메일 비교값을 만드는 문제는 ARCH-03에 적었다.
- 정상 가입 경로의 BCrypt 해시 저장, 회원 응답 DTO의 필드 선택, `Principal`에서 조회 대상을 얻는 경로 — 발견 없음(R5·R10, AC-1.7·AC-3.4).
