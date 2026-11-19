# 테스트 리뷰

- 리뷰어: codex (gpt-6.2-astra), 날짜: 2026-11-19, 대상 커밋: `a088664`

## 요약

- 수용 테스트 이름에서 AC 19개를 모두 찾았다. 전체 테스트 이름에서 R 10개를 모두 찾았다. 일부 단언은 해당 요구를 어긴 구현도 통과시킨다.
- 먼저 고칠 곳은 재시작 뒤 401의 원인, JWT 만료 경계, 유니코드 길이의 저장 경계다.
- 동시 가입은 두 유일성 제약을 한꺼번에 건드린다. 필터 오류는 같은 예외를 컨트롤러에서도 일으킬 수 있다.
- 파일만 읽었다. ‘확인’은 입력·단언·구현에서 직접 읽은 사실을 뜻한다. 실행 시간과 간헐 실패는 측정하지 않았다.

## 발견

### TEST1. 재시작 테스트는 옛 토큰의 서명이 여전히 유효한지 확인하지 않는다

- 심각도: 높음
- 종류: 확인
- 위치: `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:35`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:46`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:51`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:59`, `docs/adr/0003-authentication.md:65`
- 내용: AC-3.4·AC-3.5에서 확인하려는 조건은 서명이 맞아도 옛 회원에게 접근할 수 없다는 것이다. 두 서버가 설정한 키를 무시하고 각자 임의의 키를 써도, 옛 토큰은 401이고 새 토큰은 성공한다. 두 번째 서버에서 새 회원으로 조회한 결과만으로는 이 회귀를 구분할 수 없다.
- 근거: 두 번 모두 `start()`를 호출한다. 두 번째 서버에서 옛 토큰에 대한 단언은 `doesNotContain("finn")`과 `isEqualTo(401)`이다.
- 제안: 기존 두 번의 기동을 유지한다. 두 번째 서버의 `JwtDecoder`로 옛 토큰을 검증해 첫 회원의 `sub`가 나오는지 확인한다. 이어서 같은 토큰의 HTTP 조회가 401인지 검사한다. 두 회원의 `sub`도 비교하면 식별자 재사용까지 구분할 수 있다.

### TEST2. 만료 시각과 같은 순간을 성공으로 고정하고, 만료 시각의 누락은 검사하지 않는다

- 심각도: 높음
- 종류: 확인
- 위치: `src/test/java/trealworld/adapter/security/AccessTokenDecoderTests.java:47`, `src/test/java/trealworld/adapter/security/AccessTokenDecoderTests.java:53`, `src/test/java/trealworld/adapter/security/JwtAccessTokenIssuerTests.java:52`, `src/test/java/trealworld/acceptance/TestTokens.java:26`, `src/main/java/trealworld/adapter/security/SecurityConfig.java:64`, `docs/adr/0003-authentication.md:16`, `docs/adr/0003-authentication.md:34`
- 내용: AC-3.3과 ADR-0003은 JWT의 만료를 검사한다. JWT의 `exp`는 토큰을 받아들일 수 없는 첫 시각이다. 현재 단위 테스트는 정확히 그 순간의 수락을 요구한다. 또 발급기의 필수 claim은 검사하지만, 검사기가 `exp` 없는 토큰을 거부하는지는 확인하지 않는다.
- 근거: 정확히 한 시간 뒤에는 `decode(token)).isNotNull()`을 기대한다. 거부를 기대하는 첫 시각은 `plusSeconds(1)`이다. 수용 테스트의 서명 도우미는 항상 `expirationTime(Date.from(expiry))`를 넣는다.
- 제안: 고정한 `Clock`으로 만료 직전의 수락, 만료 시각과 직후의 거부를 각각 검사한다. `exp` 누락은 검사기 단위 테스트에서 별도로 확인한다. 현재 검사기의 누락 처리 결과는 실행으로 확인할 사항이다.

### TEST3. 코드 포인트 길이와 저장 길이가 갈리는 유니코드 입력이 빠졌다

- 심각도: 높음
- 종류: 확인
- 위치: `src/test/java/trealworld/domain/UsernameTests.java:56`, `src/test/java/trealworld/domain/EmailTests.java:40`, `src/test/java/trealworld/adapter/persistence/MemberPersistenceAdapterTests.java:104`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:272`, `src/main/java/trealworld/domain/Email.java:23`, `src/main/java/trealworld/domain/Member.java:34`, `src/main/java/trealworld/domain/Member.java:38`, `docs/domain/01-member.md:63`
- 내용: R2·R3과 AC-1.1·AC-1.9의 길이 단위는 코드 포인트다. 사용자 이름 단위 테스트에는 이모지가 있지만, DB 경계 테스트에는 한글과 ASCII만 있다. 이메일은 소문자 변환 전에만 길이를 잰다. 254코드 포인트의 주소에 `İ`가 하나 있으면 변환 뒤 `i`와 결합점으로 늘어 255코드 포인트가 된다. 현재 테스트는 이 값 객체와 저장 경계를 모두 놓친다.
- 근거: 이메일 생성자는 `codePointCount(...)`를 검사한 다음 `toLowerCase(Locale.ROOT)`를 호출한다. DB 테스트의 사용자 이름은 `"가".repeat(...)`, 이메일은 `"a".repeat(...)`다.
- 제안: 보조 평면 문자가 들어간 사용자 이름 50자와 이메일 254자를 각각 저장하고 다시 읽어 원문과 비교한다. 소문자 변환으로 상한을 넘는 이메일은 R2에서 길이를 재는 시점을 명확히 하고, 그에 맞는 도메인 결과와 HTTP 응답을 검사한다. 실제 DB가 어떤 응답을 유발하는지는 미확인이다.

### TEST4. 동시 가입 테스트는 이메일 충돌과 사용자 이름 충돌을 구분할 수 없다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:142`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:160`, `src/test/java/trealworld/application/service/RegisterMemberServiceTests.java:123`, `src/test/java/trealworld/application/service/RegisterMemberServiceTests.java:135`, `src/test/java/trealworld/adapter/persistence/MemberPersistenceAdapterTests.java:32`, `src/test/java/trealworld/adapter/persistence/MemberPersistenceAdapterTests.java:40`, `src/main/java/trealworld/application/service/RegisterMemberService.java:59`
- 내용: AC-1.5·AC-1.6, R6·R7의 HTTP 경합 테스트는 12개 요청에 두 값 모두 같은 입력을 준다. 한 제약만 작동해도 상태 코드의 분포가 같다. 특히 이메일 검사가 먼저라서 사용자 이름 충돌을 잘못된 필드로 보고해도 검출할 수 없다. 테스트 이름에 있는 ‘회원 한 명’도 응답 수만으로 판단한다.
- 근거: 모든 작업이 `register(username, email, PASSWORD)`를 호출한다. 단언은 201 한 개와 409 열한 개다. 서비스의 fake와 영속성 테스트는 두 충돌을 따로 다룬다.
- 제안: 이메일만 같은 요청 묶음과 사용자 이름만 같은 요청 묶음을 따로 보낸다. 각각 409의 `errors` 키와 해당 값으로 저장된 행 한 개를 검사한다. 확인과 저장 사이의 정확한 순서는 기존 서비스 fake 테스트에서 계속 검증한다.

### TEST5. 필터 오류 테스트는 컨트롤러에서 난 500도 통과시킨다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/acceptance/FilterErrorAcceptanceTests.java:32`, `src/test/java/trealworld/acceptance/FilterErrorAcceptanceTests.java:37`, `src/test/java/trealworld/acceptance/FilterErrorAcceptanceTests.java:53`, `src/test/java/trealworld/acceptance/FilterErrorAcceptanceTests.java:64`, `src/main/java/trealworld/adapter/security/MemberAuthenticationConverter.java:38`, `src/main/java/trealworld/adapter/web/MemberController.java:48`, `src/main/java/trealworld/adapter/web/ApiExceptionHandler.java:49`
- 내용: ADR-0005와 AC-1.7에서 확인하려는 것은 컨트롤러에 닿기 전에 난 오류의 처리다. 필터와 컨트롤러는 같은 `GetMemberUseCase`를 부른다. 필터의 회원 확인이 사라져도 컨트롤러가 같은 mock을 불러 예외를 내고, MVC 예외 처리기가 원인을 숨긴 500을 만들 수 있다.
- 근거: `given(getMember.getMember(any())).willThrow(...)` 뒤에 실제 회원 조회 경로를 호출한다. 두 테스트 모두 응답만 검사한다.
- 제안: 회원 조회 컨트롤러에 매핑되지 않은 보호 경로로 요청한다. 필터에서 mock을 호출했는지, 그 경로를 `instance`로 가진 500이 왔는지 검사한다. 이렇게 하면 MVC의 예외 처리만으로 단언을 만족시킬 수 없다.

### TEST6. 토큰 거부 사례 두 개에 서로 다른 실패 원인이 섞여 있다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/acceptance/ErrorResponseAcceptanceTests.java:41`, `src/test/java/trealworld/acceptance/ErrorResponseAcceptanceTests.java:101`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:123`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:166`, `src/main/java/trealworld/adapter/security/MemberAuthenticationConverter.java:47`
- 내용: 첫 AC-3.3 사례는 방금 가입한 회원과 무관한 고정 UUID를 사용한다. 만료 검사를 생략해도 AC-3.5의 회원 부재로 401이 가능하다. 두 번째 사례는 본문의 변경과 함께 주인 식별자를 UUID 형식이 아닌 값으로 바꾼다. 여기서도 401만으로는 서명 검사가 작동했는지 알 수 없다.
- 근거: 첫 사례는 고정 `"11111111-1111-4111-8111-111111111111"`과 과거 시각을 함께 쓴다. 두 번째 사례는 `sub`를 `"1"`로 바꾸고 `exp`도 변경한다. 반면 같은 파일의 166번째 줄 사례는 같은 회원으로 만료·유효 응답을 비교한다.
- 제안: 만료 사례에는 현재 회원을 사용하고 시각만 바꾼다. 본문 변경 사례에는 서명 이외의 검사를 통과하는 대조군을 둔다. 이미 같은 회귀를 잡는 사례가 있으면 원인이 섞인 사례를 제거해도 된다.

### TEST7. 다른 회원의 정보를 요청 본문으로 지정하는 경우와 오류 응답의 정보 노출을 놓친다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:181`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:185`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:245`, `docs/adr/0005-api-conventions.md:48`, `docs/adr/0003-authentication.md:33`
- 내용: AC-3.4·R10과 ADR-0005는 경로·쿼리·본문에서 조회 대상을 읽는 것을 금지한다. 테스트는 쿼리 응답의 사용자 이름과 대체 경로의 상태만 검사한다. 대체 경로에서 500을 돌려주거나 오류 본문에 다른 회원의 이메일을 넣어도 단언을 만족한다. ADR-0003의 토큰 전달 위치 검사도 쿼리만 다룬다.
- 근거: 대체 경로 세 곳의 단언이 모두 `isNotEqualTo(200)`이다. 본문에 조회 대상이나 토큰을 넣는 사례는 없다.
- 제안: 두 회원을 준비하고, 조회 대상을 본문에 넣어도 다른 회원의 정보가 나오지 않는지 검사한다. 대체 경로는 의도한 거부 상태와 응답 본문을 함께 확인한다. 토큰 전달 위치는 별도 사례에서 본문에만 토큰이 있을 때 401인지 검사한다.

### TEST8. 중복 가입을 거부한 뒤 기존 회원의 데이터가 보존되는지 확인하지 않는다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:24`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:130`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:165`, `src/test/java/trealworld/application/service/RegisterMemberServiceTests.java:66`, `src/test/java/trealworld/application/service/RegisterMemberServiceTests.java:85`
- 내용: AC-1.5·AC-1.6에서 ‘거부’는 오류를 알리고 요청한 일을 수행하지 않는다는 뜻이다. 기존 회원을 일부 갱신한 뒤 올바른 409를 보내는 경우를 수용 테스트가 구분하지 못한다. 서비스 테스트에서 저장 목록의 크기가 1인 것도 기존 값의 보존을 보장하지 않는다.
- 근거: 기존 가입과 중복 가입 모두 같은 `PASSWORD`를 낸다. 마지막 단언은 `$.errors.email` 또는 `$.errors.username`의 존재다.
- 제안: 중복 요청에 다른 비밀번호를 쓴다. 거부 후 원래 자격 증명으로 로그인해 처음 회원의 이름·이메일을 확인하고, 중복 요청의 자격 증명으로는 로그인할 수 없는지 검사한다.

### TEST9. 만료 수용 테스트가 실제 경과 시간과 공유 시계에 의존한다

- 심각도: 중간
- 종류: 추정
- 위치: `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:69`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:156`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:302`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:313`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:325`, `build.gradle.kts:38`
- 내용: AC-3.3 테스트는 발급 뒤 시각을 59분 50초 앞당긴다. 로그인 응답부터 조회까지 약 10초 이상 지연되면 성공을 기대한 첫 조회가 만료될 수 있다. 메서드를 병렬로 실행하면 다른 사례의 `reset()`도 같은 빈을 바꾼다. 현재 빌드 파일에는 병렬 실행을 켜는 설정이 없고, 실제 간헐 실패도 확인하지 않았다.
- 근거: `MovableClock.instant()`는 `Instant.now().plus(offset)`를 반환한다. `offset`은 테스트 메서드들이 공유한다.
- 제안: 서버의 테스트 `Clock`을 한 순간에 고정하고, 만료 시각만 다른 토큰으로 경계를 검사한다. 로그인으로 발급한 토큰의 정확한 1시간 수명은 이미 고정 시각을 쓰는 발급기 단위 테스트에서 검사한다.

### TEST10. 키 미설정 테스트가 같은 설정으로 다시 시작했을 때의 키 교체를 입증하지 않는다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/adapter/security/JwtSigningKeyTests.java:16`, `src/test/java/trealworld/adapter/security/JwtSigningKeyTests.java:22`, `src/test/java/trealworld/adapter/security/JwtSigningKeyTests.java:29`, `src/test/java/trealworld/acceptance/MemberFlowAcceptanceTests.java:23`, `src/test/java/trealworld/acceptance/MemberFlowAcceptanceTests.java:53`, `src/main/java/trealworld/adapter/security/JwtSigningKey.java:30`, `docs/adr/0003-authentication.md:25`, `docs/adr/0003-authentication.md:27`
- 내용: ADR-0003의 임의 키 단위 테스트는 빈 문자열로 만든 키와 `null`로 만든 키를 비교한다. 각 입력에 서로 다른 상수를 반환하는 구현도 통과한다. 실제 설정의 기본값은 빈 문자열이다. 수용 테스트는 한 서버에서 발급·조회만 하므로 외부에서 고정 키를 주어도 같은 결과가 나온다. 경고 출력과 UTF-8의 32바이트 경계도 각각 관찰할 단언·다중 바이트 입력이 없다.
- 근거: `new JwtSigningKey("")`와 `new JwtSigningKey(null)`을 한 번씩 만든다. 길이 사례의 입력은 ASCII다.
- 제안: 같은 빈 문자열로 두 인스턴스를 만들어 키 교체와 경고를 검사한다. UTF-8로 정확히 31·32바이트인 다중 바이트 입력도 넣는다. 수용 테스트의 미설정 경로에는 빈 설정을 명시하면 외부 설정의 영향도 없앨 수 있다. 별도 서버 두 대가 더 필요하지 않다.

### TEST11. 도메인 의존 규칙이 JPA 패키지 전체를 허용한다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/ArchitectureTests.java:83`, `src/test/java/trealworld/ArchitectureTests.java:86`, `src/main/java/trealworld/domain/Member.java:30`, `docs/adr/0002-hexagonal-architecture.md:16`, `docs/adr/0002-hexagonal-architecture.md:56`
- 내용: ADR-0002는 도메인에 JPA 매핑 애너테이션을 허용한다. ArchUnit은 패키지만 검사해서 도메인에서 `EntityManager`로 저장·조회 동작을 직접 호출해도 이 규칙을 통과시킨다.
- 근거: `onlyDependOnClassesThat().resideInAnyPackage(..., "jakarta.persistence..", ...)`에는 타입이나 사용 방식에 대한 제한이 없다.
- 제안: 매핑 애너테이션과 그 속성에 필요한 타입만 허용한다. 현재 `@GeneratedValue`가 쓰는 `GenerationType`도 포함한다. 도메인이 JPA 작업용 타입에 의존하는 반례를 규칙 검증에 사용한다.

### TEST12. 어댑터 경계 검사는 지정한 패키지 밖에 둔 구현을 놓친다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/ArchitectureTests.java:32`, `src/test/java/trealworld/ArchitectureTests.java:68`, `src/test/java/trealworld/ArchitectureTests.java:76`, `docs/adr/0002-hexagonal-architecture.md:27`, `docs/adr/0002-hexagonal-architecture.md:38`, `docs/adr/0002-hexagonal-architecture.md:45`
- 내용: ADR-0002 규칙 4와 5는 패키지 배치를 전제로 한다. 예를 들어 컨트롤러를 `trealworld.adapter` 바로 아래에 두면 어댑터 간 의존을 비교하는 그룹에도, 웹에서 들어오는 포트만 쓰는지 검사하는 대상에도 포함되지 않는다. 기존 `adapter.web`에 다른 클래스가 남아 있으면 빈 대상 검사도 이를 알리지 않는다.
- 근거: 그룹 패턴은 `trealworld.adapter.(*)..`이고, 규칙 5의 출발점은 `resideInAPackage(WEB_ADAPTER)`다.
- 제안: 먼저 컨트롤러가 `adapter.web`에 있는지 검사한다. `adapter` 바로 아래에는 현재의 `package-info`처럼 의도한 항목만 허용한다. 기존 규칙 4·5는 각 패키지의 의존 방향을 계속 검사하면 된다.

### TEST13. 실제로 검사하는 문장과 다른 ID도 이름 규칙을 통과한다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/ArchitectureTests.java:145`, `src/test/java/trealworld/application/service/RegisterMemberServiceTests.java:114`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:218`, `docs/spec/01-member-signup-login.md:33`, `docs/spec/01-member-signup-login.md:35`, `docs/adr/0003-authentication.md:40`, `docs/adr/0004-verification-tdd.md:28`, `docs/adr/0005-api-conventions.md:43`
- 내용: `ac1_2_invalid_values_are_reported_before_duplicates`는 세 값을 모두 채우고 짧은 비밀번호를 낸다. 입력이 어기는 문장은 AC-1.4다. 또 `adr0005_every_rejection_has_the_same_body`가 비교하는 ‘인증 실패 이유별 응답 동일’은 ADR-0003의 결정이다. ArchUnit은 접두어 모양만 확인한다. 존재하지 않는 ID나 마지막 수용 테스트가 삭제된 AC도 발견하지 못한다.
- 근거: 가입 입력은 `"jake", "jake@example.com", "short"`다. 이름 규칙은 `haveNameMatching(...)`으로 끝난다.
- 제안: 두 이름을 실제 검사 문장에 맞춘다. 값 오류의 우선순위도 요구로 추적하려면 그 근거를 별도로 명시한다. 기계 검사는 문서에 있는 ID 집합과 수용 테스트에 있는 ID 집합을 대조하고, 의미 일치는 리뷰에서 입력과 단언으로 확인한다.

### TEST14. 해시 생성과 비교가 함께 바뀌면 BCrypt 교체를 알아내지 못한다

- 심각도: 중간
- 종류: 확인
- 위치: `src/test/java/trealworld/adapter/security/BCryptPasswordHasherTests.java:23`, `src/test/java/trealworld/adapter/security/BCryptPasswordHasherTests.java:39`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:207`, `src/main/java/trealworld/adapter/security/BCryptPasswordHasher.java:17`, `docs/adr/0003-authentication.md:46`
- 내용: R5·R8 테스트는 평문과 다르고 salt가 있으며 같은 어댑터로 비교된다는 성질을 검사한다. 다른 salted 해시가 이 조건을 모두 만족해도 ADR-0003의 BCrypt 결정과 기존 BCrypt 해시 호환성이 깨진다. 현재 구현은 `BCryptPasswordEncoder`를 쓴다.
- 근거: `hasher.hash(...)`로 만든 값을 `hasher.matches(...)`에 다시 넣는다. DB에서 읽은 값에 대한 단언은 `isNotBlank().doesNotContain(PASSWORD)`다.
- 제안: 미리 정한 테스트 비밀번호와 독립적으로 생성한 BCrypt 해시를 한 쌍 고정해 기존 해시의 검증을 확인한다. 새로 생성한 해시는 테스트에서 BCrypt 기준 구현으로 확인한다.

### TEST15. 오류 형식 단언은 틀린 요청 경로와 빈 Problem Details 본문을 놓친다

- 심각도: 낮음
- 종류: 확인
- 위치: `src/test/java/trealworld/acceptance/ErrorResponseAcceptanceTests.java:60`, `src/test/java/trealworld/acceptance/ErrorResponseAcceptanceTests.java:70`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:215`, `docs/adr/0005-api-conventions.md:36`, `docs/adr/0005-api-conventions.md:41`
- 내용: ADR-0005는 `instance`가 오류가 난 요청의 경로이도록 정한다. 거부 경로 테스트는 모든 응답이 같은 엉뚱한 경로를 담아도 통과한다. JSON 구문 오류 테스트는 상태와 미디어 타입만 검사해서 본문이 비어 있어도 통과한다.
- 근거: 단언은 `isNotEqualTo("/error")`다. 구문 오류 사례는 `contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)`에서 끝난다.
- 제안: 각 입력 경로의 기대 `instance`를 명시해 비교한다. 구문 오류 응답은 JSON으로 읽어 `status`와 요청 경로를 확인한다.

### TEST16. 회원 준비 도우미가 중복되어 익명 요청 테스트도 가입과 로그인을 수행한다

- 심각도: 낮음
- 종류: 확인
- 위치: `src/test/java/trealworld/acceptance/ErrorResponseAcceptanceTests.java:40`, `src/test/java/trealworld/acceptance/ErrorResponseAcceptanceTests.java:74`, `src/test/java/trealworld/acceptance/ErrorResponseAcceptanceTests.java:82`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:69`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:93`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:265`, `src/test/java/trealworld/acceptance/RegisterMemberAcceptanceTests.java:49`
- 내용: ADR-0004 수용 테스트에서 회원 준비를 `@BeforeEach`로 실행한다. 토큰 없는 조회나 익명 `/error` 요청도 가입·로그인을 거치고, 오류 응답 클래스에서는 정상 조회까지 한다. 이 구조는 공유 DB에 불필요한 행을 남기고, 가입 회귀가 익명 경로 테스트의 본문 실행까지 막는다. 데이터는 UUID 앞 8자리로 구분한다.
- 근거: 여러 클래스가 가입 본문을 만들고 로그인 응답에서 토큰을 꺼내는 코드를 따로 둔다. `a_member_is_logged_in()`의 마지막 요청은 `get("/api/members/me", token)`이다.
- 제안: 회원이 필요한 사례에서만 호출하는 작은 HTTP 도우미를 공유한다. 상태·본문 단언은 각 사례에 남긴다. 공유 DB용 식별자는 전체 UUID를 쓰고, 길이 경계 사례는 그 길이를 포함해 입력을 계산한다.

### TEST17. 재시작 테스트가 빈 인메모리 DB를 전제로 고정 회원을 쓴다

- 심각도: 중간
- 종류: 추정
- 위치: `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:36`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:44`, `src/test/java/trealworld/acceptance/MemberRestartAcceptanceTests.java:57`, `src/main/resources/application.properties:1`, `docs/adr/0001-tech-stack.md:45`
- 내용: AC-3.4·AC-3.5 재시작 사례는 기동할 때마다 빈 DB를 전제하고 고정 이름으로 가입한다. 실행 환경이 별도 데이터소스를 지정하면 테스트가 그 DB에 회원을 쓰거나 두 번째 기동이 이전 회원을 볼 수 있다. 반복 실행 때 첫 가입부터 409가 날 수도 있다. 실제 기본 설정에서 이런 충돌을 관찰한 것은 아니다.
- 근거: `start()`가 지정하는 값은 `server.port`와 `trealworld.jwt.secret`뿐이다. 각 서버의 첫 가입 이름은 `"jake"`와 `"finn"`이다.
- 제안: 재시작 사례의 데이터소스를 테스트 전용 인메모리 H2로 고정한다. 각 서버에서 첫 가입 전에 회원 수가 0인지 확인하면 DB가 남은 실패와 옛 토큰의 조회 실패를 구분할 수 있다.

## 확인했지만 문제가 없는 것

- AC-1.1 → AC-2.1 → AC-3.1의 연결은 발견 없음. `MemberFlowAcceptanceTests`가 가입 전 로그인 실패부터 발급한 토큰으로 본인 정보를 읽는 단계까지 HTTP로 확인한다.
- R1의 필수 값, R4의 코드 포인트 하한·UTF-8 바이트 상한, R8·R9의 회원별 비밀번호 대조와 로그인 실패 응답 비교는 발견 없음.
- 현재 수용 테스트 간 명시적인 실행 순서 의존은 발견 없음. 재시작 사례는 두 서버 컨텍스트를 각각 닫는다. 도메인·서비스 단위 테스트의 Spring 격리와 Gradle의 JUnit Platform 설정도 발견 없음.
