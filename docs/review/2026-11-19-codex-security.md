# 보안 리뷰

- 리뷰어: codex (gpt-6.2-astra), 날짜: 2026-11-19, 대상 커밋: `a088664`

## 요약

- 높음 0건, 중간 5건, 낮음 3건이다.
- JWT 발급기는 `exp`를 넣지만 검사기는 누락을 허용한다. 기존 테스트는 만료 시각과 현재 시각이 정확히 같을 때도 토큰을 받아들이도록 기대한다.
- 공개 가입·로그인에서 큰 JSON을 먼저 읽는다. 비밀번호는 잘못된 유니코드 입력을 UTF-8로 바꿀 때 서로 다른 값을 구별하지 못할 수 있다.
- 500 응답은 원인을 숨기지만 예외 메시지는 로그로 전달된다. 테스트는 응답 본문만 검사한다.
- 코드, 테스트, 설정 및 해당 버전의 라이브러리 파일을 정적으로 읽었다. Gradle과 서버는 실행하지 않았다. 아래 요청 예시는 수정 후 기대할 결과다.

## 발견

### SEC1. JWT 검사기가 필수 만료 시각을 요구하지 않는다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/adr/0003-authentication.md:16`, `docs/adr/0003-authentication.md:34`, `src/main/java/trealworld/adapter/security/JwtAccessTokenIssuer.java:44`, `src/main/java/trealworld/adapter/security/SecurityConfig.java:64`, `src/main/java/trealworld/adapter/security/MemberAuthenticationConverter.java:36`
- 내용: 같은 키로 올바르게 서명됐고 실제 회원의 `sub`가 있는 토큰에서 `exp`만 빠지면 검사기에 만료 기준이 없다. 현재 로그인 발급기는 항상 `exp`를 넣으므로 일반 회원이 정상 토큰의 내용을 바꿔 이 조건을 만들 수는 없다. 발급 오류가 생기면 그 토큰을 시간으로 제한할 마지막 검사가 빠져 있다. 관련 기준은 AC-3.3과 ADR-0003이다.
- 근거: 검사기는 `new JwtTimestampValidator(Duration.ZERO)`를 만들고 시계만 설정한다. Spring Security 7.1.2의 이 생성자는 `allowEmptyExpiryClaim`을 `true`로 초기화한다. `Duration.ZERO`는 시계 오차만 조절한다.
- 제안: `expiry.setAllowEmptyExpiryClaim(false)`를 설정한다. 서명 실패 테스트와 별도로 필수 클레임 누락을 검사한다.
- 요청 예시: 테스트 회원과 서명 검사를 통과하는 테스트용 토큰을 준비한다. `GET /api/members/me`에서 `exp`가 없으면 401, 미래의 `exp`가 있으면 200을 기대한다.

### SEC2. 공개 API가 필드 길이를 검사하기 전에 큰 JSON을 처리한다

- 심각도: 중간
- 종류: 확인
- 위치: `src/main/java/trealworld/adapter/web/AuthController.java:23`, `src/main/java/trealworld/adapter/web/MemberController.java:34`, `src/main/java/trealworld/domain/Email.java:17`, `src/main/java/trealworld/domain/Email.java:20`, `src/main/java/trealworld/domain/Password.java:15`, `src/main/resources/application.properties:1`
- 내용: 두 공개 경로 모두 JSON을 문자열 필드로 읽은 뒤에 도메인 상한을 적용한다. 이메일은 254자를 검사하기 전에 정규식을 실행하고, 비밀번호는 72바이트를 검사하려고 전체 UTF-8 배열을 만든다. 짧은 회원 정보만 받는 API에서 거부할 입력에도 CPU와 메모리를 쓴다. 실제 지연이나 메모리 고갈은 측정하지 않았다. 관련 기준은 AC-1.4, AC-1.9, R2, R4다.
- 근거: 두 컨트롤러는 `@RequestBody`를 쓴다. `FORMAT.matcher(value).matches()`가 길이 검사보다 앞선다. 비밀번호 검사는 `value.getBytes(StandardCharsets.UTF_8).length`를 읽는다. 프로젝트 설정에는 JSON 본문의 총 바이트 상한이 없다.
- 제안: 두 경로에 JSON 본문 바이트 상한을 정하고, 읽은 바이트가 상한을 넘는 시점에 413을 반환한다. `Content-Length`가 없는 요청에도 같은 상한을 적용한다. 이메일은 긴 입력을 먼저 거부하고, 비밀번호도 72바이트 안에 들어갈 수 없는 길이는 배열을 만들기 전에 거부한다.
- 요청 예시: 테스트에서 작은 본문 상한을 설정한다. `POST /api/members`와 `POST /api/auth/login`에 상한보다 1바이트 큰 JSON을 보냈을 때 413과 유스케이스 호출 0회를 기대한다. 상한 안에 있는 잘못된 필드는 기존 400 또는 401로 처리한다.

### SEC3. 잘못된 유니코드를 포함한 비밀번호가 다른 입력과 같은 바이트로 바뀐다

- 심각도: 중간
- 종류: 확인
- 위치: `src/main/java/trealworld/domain/Password.java:15`, `src/main/java/trealworld/adapter/security/BCryptPasswordHasher.java:21`, `src/main/java/trealworld/adapter/security/BCryptPasswordHasher.java:26`, `src/main/java/trealworld/application/service/LoginService.java:73`, `src/test/java/trealworld/domain/PasswordTests.java:49`
- 내용: `Password`는 짝이 없는 UTF-16 서로게이트도 한 자리로 센다. Java의 UTF-8 변환은 이 값을 대체 바이트로 바꾼다. BCrypt도 같은 변환을 사용하므로, 해당 위치에서 서로 다른 입력이 같은 비밀번호로 비교될 수 있다. 정상적인 이모지의 서로게이트 쌍은 유효하다. 관련 기준은 AC-1.4, AC-2.2, R4, R8이다.
- 근거: Jackson 3.1.7의 문자열 값 파서는 유니코드 이스케이프 하나를 Java의 `char` 하나로 읽는다. `Password`의 `codePointCount`와 `getBytes(StandardCharsets.UTF_8)` 사이에는 인코딩 오류 검사가 없다. Spring Security 7.1.2의 `BCrypt.hashpw`와 `checkpw`도 같은 문자셋으로 변환한다.
- 제안: `Password`에서 UTF-8 인코딩 오류를 거부하고, 검증된 바이트 수로 기존 72바이트 상한을 적용한다. 가입과 로그인 모두 같은 값 객체를 거치므로 두 경로의 비교 규칙을 함께 고칠 수 있다.
- 요청 예시: 테스트용 비밀번호에 짝이 없는 서로게이트가 있는 경우 `POST /api/members`는 400과 `errors.password`, `POST /api/auth/login`은 401을 기대한다. 같은 테스트에 정상적인 이모지 비밀번호의 가입과 로그인 성공을 대조 사례로 둔다.

### SEC4. 500 응답에서 가린 비밀번호 해시가 예외 로그에 남을 수 있다

- 심각도: 중간
- 종류: 확인
- 위치: `src/main/java/trealworld/adapter/web/ApiExceptionHandler.java:48`, `src/test/java/trealworld/adapter/web/ApiExceptionHandlerTests.java:48`, `src/main/java/trealworld/domain/PasswordHash.java:9`, `docs/adr/0005-api-conventions.md:39`
- 내용: 예외와 원인 체인이 그대로 로거에 전달된다. 메시지에 SQL 값이나 비밀번호 해시가 들어간 경우 로그 열람자에게도 그 값이 보인다. 기존 테스트에 바로 이 조건이 있다. 실제 회원의 해시가 로그에 기록된 사례는 확인하지 않았다. AC-1.7은 응답에 관한 기준이고, 로그의 진단 정보는 ADR-0005가 다룬다.
- 근거: 처리기는 `logger.error("예상하지 못한 오류", exception)`을 호출한다. 테스트는 모의 예외 메시지에 `stored-hash`를 넣고 `body`만 검사한다. 또 `PasswordHash`는 기본 record의 `toString()`이 해시를 출력한다. 현재 이 객체를 직접 기록하는 애플리케이션 로그 호출은 찾지 못했다.
- 제안: 오류 식별자, 예외 타입, 필요한 스택 위치는 기록하되 원문 값이 든 예외 메시지는 제거한다. `PasswordHash.toString()`도 마스킹한다. 객체 마스킹과 별도로 이미 문자열이 된 예외 메시지를 검사한다.
- 요청 예시: 기존 `POST /api/members` 모의 예외 테스트에 로그 캡처를 추가한다. 500 응답과 로그 양쪽에 테스트용 해시 표식이 없고 로그에는 오류 식별자가 남는지 확인한다.

### SEC5. 유니코드 가입 정보의 길이를 도메인과 H2가 다르게 센다

- 심각도: 낮음
- 종류: 확인
- 위치: `docs/domain/01-member.md:63`, `src/main/java/trealworld/domain/Email.java:23`, `src/main/java/trealworld/domain/Username.java:19`, `src/main/java/trealworld/domain/Member.java:34`, `src/main/java/trealworld/domain/Member.java:38`, `src/main/java/trealworld/application/service/RegisterMemberService.java:52`, `src/test/java/trealworld/domain/UsernameTests.java:57`, `src/test/java/trealworld/adapter/persistence/MemberPersistenceAdapterTests.java:104`
- 내용: 도메인은 코드 포인트를 세지만 H2 2.4.240은 `VARCHAR`에 넣을 때 UTF-16 코드 단위 수를 열 길이와 비교한다. 따라서 유효한 사용자 이름도 저장 단계에서 거부될 수 있다. 이메일은 소문자로 바꾸기 전에만 길이를 검사하므로 변환 뒤 길이가 늘어나는 경우도 놓친다. 공개 가입 요청의 입력값이 서버 오류 경로에 도달한다. 관련 기준은 AC-1.1, AC-1.9, R2, R3, ADR-0001이다.
- 근거: 열 길이가 `Username.MAX_CHARACTERS`, `Email.MAX_CHARACTERS`와 같다. H2의 `Value.convertToVarchar`는 `String.length()`를 쓴다. 도메인 테스트는 이모지 50자를 허용하지만 저장 테스트는 한글과 ASCII만 쓴다. 이메일의 `toLowerCase(Locale.ROOT)`는 길이 검사 다음에 있다.
- 제안: 도메인의 코드 포인트 상한을 유지하고 열은 허용한 값의 UTF-16 길이를 수용하도록 정한다. 이메일은 소문자 변환 전후 중 어디에 254자를 적용할지 R2에 명시하고, DB에 보내기 전에 그 규칙을 검사한다.
- 요청 예시: `POST /api/members`에서 도메인 테스트가 이미 허용한 이모지 50자 이름으로 201과 저장된 이름의 일치를 기대한다. 소문자 변환으로 길이가 늘어나는 경계 이메일에는 정한 규칙에 따라 201 또는 필드별 400을 기대한다.

### SEC6. 만료 시각과 현재 시각이 같을 때 JWT를 받아들인다

- 심각도: 낮음
- 종류: 확인
- 위치: `src/main/java/trealworld/adapter/security/SecurityConfig.java:64`, `src/test/java/trealworld/adapter/security/AccessTokenDecoderTests.java:49`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:156`, `docs/adr/0003-authentication.md:34`
- 내용: AC-3.3과 ADR-0003은 만료된 토큰을 거부하고 시계 오차를 두지 않는다. 현재 검사는 `now > exp`일 때 거부하므로 정확히 같은 시각이 허용된다. 영향은 두 시각이 일치하는 경계에 한정된다. JWT의 `exp`는 해당 시각부터 유효하지 않다는 뜻이다. [RFC 7519 §4.1.4](https://www.rfc-editor.org/rfc/rfc7519.html#section-4.1.4)
- 근거: 만료 정각의 디코딩 결과를 `isNotNull()`로 단언한다. 수용 테스트는 만료 10초 전과 10초 후를 검사한다.
- 제안: 주입한 `Clock`을 사용해 `now < exp`일 때만 허용한다. 기존 정각 테스트의 기대값을 거부로 바꾼다.
- 요청 예시: 같은 테스트 회원의 정상 토큰으로 `GET /api/members/me`를 검사한다. 고정 시계가 만료 직전이면 200, 만료 정각과 직후이면 각각 401을 기대한다.

### SEC7. 직접 설정하는 HS256 키에는 생성 기준이 없다

- 심각도: 낮음
- 종류: 확인
- 위치: `docs/adr/0003-authentication.md:24`, `src/main/java/trealworld/adapter/security/JwtSigningKey.java:35`, `src/main/java/trealworld/adapter/security/JwtSigningKey.java:38`, `src/main/resources/application.properties:1`
- 내용: 자동 생성 경로는 `SecureRandom`을 쓴다. 직접 설정하는 경로는 문자열의 UTF-8 길이만 검사한다. 사람이 고른 예측 가능한 문자열도 길이를 채우면 서명 키가 된다. 저장소의 기본 설정은 실제 키를 지정하지 않는다. 위험은 운영자가 약한 값을 명시한 경우에 생긴다. 관련 기준은 ADR-0003과 AC-3.3이다.
- 근거: 직접 지정한 값에는 `bytes.length < MIN_SECRET_BYTES`만 검사한다. 32바이트라는 길이로 256비트의 무작위성을 보장할 수 없다. [RFC 8725 §3.5](https://www.rfc-editor.org/rfc/rfc8725.html#section-3.5)
- 제안: 설정 키도 암호학적 난수로 생성하는 방법을 ADR에 적는다. 배포 예시에는 실제 키를 싣지 않는다. 외부 키 관리 서비스 도입은 **범위 밖**이다.
- 요청 예시: 서버 소유자가 생성한 테스트 키로 로그인한 뒤 `GET /api/members/me`에서 200을 기대한다. 다른 테스트 키로 교체한 검사기에서는 기존 토큰에 401을 기대한다. 키 생성 품질은 HTTP 응답으로 판별할 수 없으므로 설정을 준비하는 단계에서 확인한다.

### SEC8. 공개 로그인에 비밀번호 시도 횟수 제한이 없다

- 심각도: 중간
- 종류: 확인
- 위치: `src/main/java/trealworld/adapter/security/SecurityConfig.java:43`, `src/main/java/trealworld/adapter/web/AuthController.java:23`, `src/main/java/trealworld/application/service/LoginService.java:39`, `src/main/java/trealworld/adapter/security/BCryptPasswordHasher.java:26`
- 내용: 유효한 형식의 로그인 시도는 이전 실패 횟수와 관계없이 처리된다. 등록된 이메일에는 매번 BCrypt 비교 비용이 들며, 비밀번호 후보를 계속 시도하는 데 애플리케이션 수준의 제한이 없다. AC-2.2, R8, R9, ADR-0003은 실패 응답과 자격 증명 검사를 규정한다. 시도 횟수 정책은 정하지 않았다.
- 근거: `permitAll()`인 로그인 경로가 `login.login(...)`으로 바로 이어진다. 서비스에는 `passwordHasher.matches(...)` 전에 시도 횟수를 확인하는 분기가 없다.
- 제안: **범위 밖** — 외부에 공개할 때 출발지와 로그인 식별자를 함께 고려한 제한을 BCrypt 앞에 둔다. 제한 응답은 가입 여부와 관계없이 같게 한다.
- 요청 예시: 제한을 도입할 때 테스트에서 허용 횟수를 1회로 설정한다. 테스트 회원의 `POST /api/auth/login` 실패 뒤 다음 시도에는 429를 기대한다. 미등록 테스트 이메일에도 같은 정책과 응답을 기대한다.

## 확인했지만 문제가 없는 것

- **JWT 서명·소유권 — 발견 없음.** 발급기와 검사기가 모두 HS256을 지정한다. `sub`는 소문자 UUID 형식과 회원 존재 여부를 확인한다. 조회 대상은 `Principal`에서만 읽는다. 다른 키, 무서명, 위조, 없는 회원, 재시작 후 옛 토큰에 대한 거부 테스트가 있다.
- **임의 키와 BCrypt의 정상 입력 — 발견 없음.** 키 미설정 시 32바이트 난수를 만들고 경고에는 키 값을 쓰지 않는다. 가입과 로그인은 같은 `Password`의 72바이트 상한을 거친다. BCrypt 테스트는 같은 원문의 해시 두 개가 달라도 둘 다 검증되는지 확인한다.
- **로그인 실패 본문 — 발견 없음.** AC-2.3 테스트는 없는 이메일과 틀린 비밀번호의 401 본문과 미디어 타입을 비교한다. 시간 차이는 남는다. `LoginService.java:39`에서 없는 이메일은 BCrypt 전에 끝난다. 원격에서 구별되는 시간 차이는 측정하지 않았다. 가입 중복 응답과 로그인 시간 차이는 명세가 수용한 한계다(`docs/spec/01-member-signup-login.md:97`). **범위 밖**에서 시간 차이까지 줄인다면 미등록 이메일도 사전 계산한 더미 해시와 한 번 비교한다.
- **응답 필드·스택 트레이스 — 발견 없음.** 가입·조회는 `MemberResponse(username, email)`, 로그인은 `LoginResponse(accessToken)`만 반환한다. MVC와 필터의 500 응답에 SQL·해시·예외 원인을 싣지 않는 테스트가 있다. AC-1.7의 응답 보호와 SEC4의 로그 경로는 별도로 확인했다.
- **필터 체인·브라우저·관리 경로 — 발견 없음.** 공개 경로는 두 POST로 제한된다. 내부 `ERROR` 디스패치는 허용하고 직접 요청한 `/error`는 인증한다. Bearer 헤더와 `STATELESS` 정책에서 CSRF를 끈다. 자격 증명을 허용하는 교차 출처 설정, 기본 보안 헤더를 끄는 설정, H2 콘솔 활성화 설정, Actuator 의존성은 대상 파일에 없다.
- **SQL 주입·대량 할당 — 발견 없음.** 회원 조회는 Spring Data 파생 쿼리를 사용한다. 컨트롤러는 요청 DTO에서 허용한 필드만 명령에 복사한다. 요청 값으로 회원 식별자나 저장할 비밀번호 해시를 지정하는 경로는 찾지 못했다.
