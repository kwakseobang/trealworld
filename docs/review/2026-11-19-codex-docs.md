# 문서 리뷰

- 리뷰어: codex (gpt-6.2-astra), 날짜: 2026-11-19, 대상 커밋: `a088664`

## 요약

- Plan 01은 종료 조건을 충족했다는 기록과 사용자의 종료 확인이 남았다는 사실을 표시하지 않는다. 작업 7의 실제 검증 순서도 ADR-0004의 필수 절차와 다르다.
- 회원 식별자의 UUID 전환은 ADR과 구현에 반영됐다. 도메인 문서의 조회 포트 설명과 ADR의 `sub` 입력 형식에는 빠진 내용이 있다.
- 같은 입력에 성공과 거부를 함께 요구하는 AC가 있다. 이메일 형식, 길이의 단위, 공백, 토큰 만료 시각에서는 문서만으로 기대 결과를 정하기 어렵다.
- 소스에서 AC 19개와 R 10개의 테스트 이름을 확인했다. 테스트 통과 수치는 일지에 기록된 당시 결과다. 이번 리뷰는 Gradle과 서버를 실행하지 않았다.

## 발견

### DOC1. Plan 01에서 구현 완료와 종료 확인 대기를 구별할 수 없다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/plan/01-member-slice.md:3`, `docs/plan/01-member-slice.md:22`, `docs/plan/01-member-slice.md:35`, `src/test/java/trealworld/acceptance/MemberFlowAcceptanceTests.java:42`, `docs/journal/2026-11-18.md:153`, `docs/journal/2026-11-18.md:221`, `docs/journal/2026-11-18.md:269`
- 내용: 계획의 상태 줄은 구현 시작을 기다리는 문장으로 끝난다. 작업 7의 테스트는 가입 응답, 로그인 토큰, 그 토큰으로 조회한 회원을 차례로 검사한다. 일지는 당시 AC 19개, R 10개, 아키텍처 테스트와 작업별 기록까지 네 종료 조건을 모두 충족했다고 보고한다. UUID 변경 후에도 전체 테스트 214개 통과를 기록했다.
- 근거: 계획에는 “구현은 사용자의 지시가 있을 때 시작한다”가 남아 있다. 일지에는 “상태의 줄은 고치지 않았다. 사용자가 닫는다”가 있다.
- 제안: 계획 승인 이력을 보존하고 “구현 및 검증 완료(2026-11-18), 사용자 종료 확인 대기”를 덧붙인다. 종료 조건 옆에 스텝 72와 UUID 변경 후 검증인 스텝 75를 연결한다.

### DOC2. ADR-0004의 필수 TDD 순서를 작업 7에 적용할 수 없다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/adr/0004-verification-tdd.md:12`, `docs/adr/0004-verification-tdd.md:20`, `docs/plan/01-member-slice.md:8`, `docs/plan/01-member-slice.md:22`, `docs/journal/2026-11-18.md:143`, `docs/journal/2026-11-18.md:209`
- 내용: ADR-0004는 수용 테스트 실패와 도메인 단위 테스트 실패를 구현 전에 요구한다. Plan 01은 이 순서를 모든 작업에 적용하면서, 작업 7에서는 이미 구현한 AC-1.1, AC-2.1, AC-3.1의 연결을 검증하라고 한다. 일지에 따르면 새 흐름 테스트는 처음부터 통과했고 이후에 결함을 넣어 검출력을 확인했다.
- 근거: ADR은 “순서는 **반드시** 아래와 같**아야 한다**”고 한다. 일지는 “빨강은 결함을 넣어서 봤다”고 기록했다.
- 제안: 새 동작을 만드는 작업과 기존 동작을 검증하는 작업의 적용 조건을 ADR에 적는다. 작업 7처럼 구현 변경이 없는 경우에 어떤 증거로 테스트의 검출력을 인정할지 정하고 Plan 01에서 그 결정을 참조한다.

### DOC3. AC-3.1과 AC-3.5가 주인이 사라진 같은 토큰에 서로 다른 결과를 요구한다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:60`, `docs/spec/01-member-signup-login.md:64`, `docs/adr/0003-authentication.md:38`, `src/test/java/trealworld/acceptance/GetMemberAcceptanceTests.java:194`
- 내용: AC-3.1은 유효한 토큰이면 회원 정보를 돌려주라고 한다. AC-3.5는 주인이 사라진 토큰도 “유효”하다고 부르면서 조회 거부를 요구한다. 테스트는 같은 토큰으로 먼저 200을 기대하고 회원을 삭제한 뒤 401을 기대한다. 응답을 가르는 조건은 회원의 존재 여부다.
- 근거: “액세스 토큰이 유효하면”과 “토큰이 유효해도 토큰의 주인인 회원이 없으면”의 적용 범위가 겹친다.
- 제안: AC-3.1의 성공 조건에 “토큰 검사를 통과하고 그 식별자의 회원이 존재하면”을 쓴다. AC-3.5에는 같은 검사를 통과했지만 회원이 없는 경우를 남긴다. 구체적인 토큰 검사 방법은 ADR-0003에서 정의한다.

### DOC4. AC-2.5의 로그인 성공 조건에 비밀번호 일치가 빠졌다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:48`, `docs/spec/01-member-signup-login.md:49`, `docs/spec/01-member-signup-login.md:52`, `docs/domain/01-member.md:57`, `src/main/java/trealworld/application/service/LoginService.java:39`, `src/test/java/trealworld/acceptance/LoginAcceptanceTests.java:162`
- 내용: 이메일의 대소문자를 바꾸고 틀린 비밀번호를 내면 AC-2.2는 거부를 요구한다. AC-2.5의 성공 문장에는 틀린 비밀번호를 제외하는 조건이 없다. R8과 구현은 해시 일치를 확인하고, AC-2.5의 테스트는 가입 때 설정한 비밀번호를 낸다.
- 근거: AC-2.5는 “시스템은 **반드시** 로그인을 받아들여**야 한다**”고 한다. 테스트 호출은 `login(email.toUpperCase(), PASSWORD)`다.
- 제안: AC-2.5에 “가입한 이메일과 대소문자만 다르고 비밀번호가 일치하면”을 쓴다. 또는 이메일 비교 규칙만 이 ID에 두고 성공·거부는 AC-2.1과 AC-2.2를 참조한다.

### DOC5. R2의 ‘올바른 형식’에는 이메일을 판정할 규칙이 없다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:34`, `docs/spec/01-member-signup-login.md:51`, `docs/domain/01-member.md:51`, `src/main/java/trealworld/domain/Email.java:17`, `src/test/java/trealworld/domain/EmailTests.java:23`, `docs/journal/2026-11-17.md:314`
- 내용: AC-1.3과 AC-2.4는 틀린 이메일 형식을 거부하도록 하지만 R2도 “올바른 형식”만 요구한다. 실제 정규식은 도메인의 연속된 점을 허용한다. 예를 들어 `jake@example..com`을 받을지 여부를 문서로 판정할 수 없다. 구현에서 정한 형식이 곧 승인된 요구인지는 별도 결정이다.
- 근거: R2는 “이메일은 **반드시** 올바른 형식이**어야 한다**”로 시작한다. 테스트는 `jake@example`과 `jake@@example.com`을 거부하는 예를 든다.
- 제안: 이 슬라이스가 허용하는 주소 형식을 R2 한 곳에 정한다. 연속된 점, 앞뒤 공백, 유니코드 공백의 허용 여부에 각각 예를 붙인다. 가입과 로그인 AC는 R2를 참조한다.

### DOC6. R2가 이메일 254자의 출처와 길이를 세는 시점을 혼동하게 한다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:40`, `docs/spec/01-member-signup-login.md:91`, `docs/domain/01-member.md:51`, `docs/domain/01-member.md:63`, `docs/domain/01-member.md:65`, `src/main/java/trealworld/domain/Email.java:23`, `src/test/java/trealworld/domain/EmailTests.java:40`, `docs/journal/2026-11-17.md:902`
- 내용: AC-1.9와 R2의 254자는 프로젝트에서 정한 코드 포인트 수다. Domain 01은 이를 SMTP 표준의 최대 길이라고 설명하지만, 일지에도 기록했듯 SMTP의 단위는 옥텟이다. 또 R2에는 소문자 변환 전후 어느 값을 세는지 없다. 구현은 입력 길이부터 검사한다. `İ`처럼 소문자 변환으로 코드 포인트가 늘어나는 문자가 있어 두 기준의 결과가 달라진다.
- 근거: 문서는 “한 자는 유니코드의 코드 포인트 하나다”와 “SMTP 표준(RFC 5321)에서 쓰는 주소의 최대 길이다”를 함께 쓴다. `Email`은 `codePointCount` 검사 뒤에 `toLowerCase(Locale.ROOT)`를 호출한다. 254자 경계 테스트는 ASCII로만 주소를 만든다.
- 제안: 254 코드 포인트를 선택한 프로젝트 결정과 SMTP의 옥텟 단위를 구별해 적는다. 입력값과 저장할 값 중 어디에 상한을 적용할지 정하고, 소문자 변환으로 길이가 변하는 경계값을 R2의 예로 둔다.

### DOC7. ‘비어 있다’와 ‘이메일이 없다’로는 로그인 400과 401을 가를 수 없다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:33`, `docs/spec/01-member-signup-login.md:50`, `docs/spec/01-member-signup-login.md:51`, `docs/domain/01-member.md:51`, `docs/domain/01-member.md:53`, `docs/adr/0005-api-conventions.md:53`, `src/test/java/trealworld/acceptance/LoginAcceptanceTests.java:122`, `src/test/java/trealworld/acceptance/LoginAcceptanceTests.java:157`, `src/test/java/trealworld/acceptance/LoginAcceptanceTests.java:205`, `src/test/java/trealworld/domain/PasswordTests.java:55`
- 내용: AC-2.3의 “이메일이 없으면”은 필드 누락(AC-2.4, 400)과 미가입 이메일(AC-2.3, 401) 모두로 읽힌다. AC-1.2와 AC-2.4는 여러 필드에 같은 “비어 있다”를 쓴다. 테스트는 공백 세 개인 이메일에 400을, 같은 비밀번호에 401을 기대한다. 공백 여덟 개인 비밀번호는 R4를 만족한다. 이메일이 254자를 넘을 때도 테스트는 400을 기대하지만 ADR-0005의 로그인 값 오류 목록은 이 길이 조건을 명시하지 않는다.
- 근거: ADR-0005는 비밀번호의 값 오류를 “없거나 빈 글자(`""`)”로 한정한다. `PasswordTests`는 공백 여덟 개로 값 객체를 만드는 예를 허용한다.
- 제안: AC-2.3에는 “R2를 만족하는 이메일로 가입한 회원이 없으면”을 쓴다. 필드별로 누락, `null`, 빈 문자열, 공백만 있는 문자열의 의미를 정의한다. 로그인 오류 표에 이메일 길이 초과(400)와 비어 있지 않은 비밀번호의 길이 위반(401)을 함께 적는다.

### DOC8. Domain 01의 조회 포트와 ADR-0003의 UUID 표기가 구현보다 덜 구체적이다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/domain/01-member.md:44`, `docs/domain/01-member.md:73`, `docs/adr/0003-authentication.md:17`, `docs/adr/0003-authentication.md:66`, `src/main/java/trealworld/domain/Member.java:26`, `src/main/java/trealworld/application/port/out/MemberRepository.java:21`, `src/main/java/trealworld/adapter/security/MemberAuthenticationConverter.java:46`, `src/test/java/trealworld/adapter/security/MemberAuthenticationConverterTests.java:62`
- 내용: Domain 01은 “이메일이나 사용자 이름으로 회원을 찾는다”고 설명한다. 포트는 식별자와 이메일로 회원을 반환하고, 사용자 이름으로는 존재 여부만 확인한다. ADR-0003은 UUID와 36글자를 명시하지만, 같은 UUID의 대문자 표기까지 거부하는 입력 규칙은 코드와 테스트에만 있다. AC-3.4·AC-3.5와 R10을 따르는 구현자가 두 문서만으로 같은 조회 계약을 만들기 어렵다.
- 근거: 포트에는 `findById(UUID id)`, `findByEmail(Email email)`, `existsByUsername(Username username)`이 있다. 변환기 주석은 “소문자, 36글자”라고 한다.
- 제안: Domain 01에 저장한 회원을 식별자로 조회한다는 책임과 중복 확인의 역할을 적는다. ADR-0003에는 `sub`를 하이픈을 포함한 소문자 UUID 문자열로 발급하고 검사한다는 규칙을 둔다. Spec 01에는 회원 소유권만 남겨도 된다.

### DOC9. 만료 시각과 현재 시각이 같을 때의 판정은 테스트에만 있다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:62`, `docs/adr/0003-authentication.md:18`, `docs/adr/0003-authentication.md:34`, `src/test/java/trealworld/adapter/security/AccessTokenDecoderTests.java:47`, `src/test/java/trealworld/adapter/security/AccessTokenDecoderTests.java:53`
- 내용: AC-3.3은 만료된 토큰을 거부하도록 한다. ADR-0003은 유효 시간 1시간과 여유 시간 0을 정한다. 어느 문장도 `now == exp`를 명시하지 않는다. 단위 테스트는 발급 후 정확히 1시간에는 허용하고 1초 뒤에는 거부하도록 정했다.
- 근거: “만료 시각의 뒤에 여유 시간을 **절대** 두**어서는 안 된다**.” 테스트는 `decoderAt(ISSUED.plus(Duration.ofHours(1)))`에서 디코딩 성공을 기대한다.
- 제안: `now < exp`, `now == exp`, `now > exp`의 기대 결과를 ADR에 적는다. JWT의 만료 시각에 맞춰 `now >= exp`를 거부하는 계약을 채택하면 등호 시점의 테스트 기대값도 함께 바꾼다.

### DOC10. ADR-0003의 회원 조회 횟수는 요청과 처리 단계의 단위를 섞는다

- 심각도: 낮음
- 종류: 확인
- 위치: `docs/adr/0003-authentication.md:38`, `docs/adr/0003-authentication.md:63`, `docs/adr/0003-authentication.md:74`, `src/main/java/trealworld/adapter/security/MemberAuthenticationConverter.java:38`, `src/main/java/trealworld/adapter/web/MemberController.java:48`, `src/main/java/trealworld/application/service/GetMemberService.java:27`
- 내용: 같은 ADR에 요청당 조회가 한 번과 두 번으로 적혀 있다. 성공한 내 정보 조회는 인증 단계에서 한 번, 응답을 만들 때 한 번 `findById`를 호출한다. 주인이 없으면 첫 단계에서 끝난다(AC-3.1, AC-3.5). 버린 대안인 서버 저장 토큰의 비용도 “요청마다 DB를 조회한다”라고만 적어 현재 방식의 회원 조회와 비교하기 어렵다.
- 근거: 결정에는 “요청마다 DB를 한 번 조회한다”, 결과에는 “인증이 필요한 요청마다 회원을 DB에서 두 번 읽는다”가 있다.
- 제안: “인증 시 회원 존재 확인 1회, 성공한 내 정보 조회에서는 회원 조회 호출 총 2회”로 조건과 단위를 적는다. 대안 비교에서는 토큰 저장소 조회와 회원 저장소 조회를 구별한다.

### DOC11. ADR-0005에 가입과 로그인 요청의 JSON 필드 계약이 없다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:7`, `docs/adr/0005-api-conventions.md:14`, `docs/adr/0005-api-conventions.md:30`, `docs/adr/0005-api-conventions.md:47`, `docs/adr/0005-api-conventions.md:52`, `src/main/java/trealworld/adapter/web/RegisterMemberRequest.java:6`, `src/main/java/trealworld/adapter/web/LoginRequest.java:6`, `docs/journal/2026-11-17.md:468`
- 내용: Spec 01은 API 모양을 ADR에 맡긴다. ADR-0005는 경로와 성공 응답을 정의하지만 가입·로그인 요청의 필드 이름과 JSON 타입을 한 곳에 정의하지 않는다. 숫자 입력이나 모르는 필드를 허용할지도 정하지 않았다. 일지는 과거 실행에서 숫자 사용자 이름이 문자열로 바뀐 사례를 기록한다. AC-1.1·AC-1.2·AC-2.4의 입력 계약을 DTO나 과거 실행 결과에서 추론해야 한다.
- 근거: 문서는 본문이 “JSON”이라고 정한다. DTO는 `RegisterMemberRequest(String username, String email, String password)`와 `LoginRequest(String email, String password)`다.
- 제안: 두 요청의 필수 필드와 JSON 타입을 ADR-0005에 적고 짧은 예를 하나씩 둔다. 잘못된 타입과 추가 필드의 처리에 대해 약속할 범위를 정한다.

### DOC12. ADR-0002의 예외 배치 규칙과 가입 실패 테스트가 다르다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/adr/0002-hexagonal-architecture.md:52`, `docs/adr/0005-api-conventions.md:39`, `src/main/java/trealworld/application/service/RegisterMemberService.java:48`, `src/test/java/trealworld/application/service/RegisterMemberServiceTests.java:150`
- 내용: ADR-0002는 유스케이스가 던지는 모든 예외를 들어오는 포트 패키지에 두도록 한다. 가입은 저장 실패 후 이메일·사용자 이름의 중복(R6·R7)이 확인되지 않으면 `java.lang.IllegalStateException`을 직접 던진다. 테스트도 그 타입을 요구한다. 업무 오류와 예상하지 못한 500의 경계를 규칙에 명시할 필요가 있다.
- 근거: “유스케이스가 던지는 예외는 **반드시** 들어오는 포트의 패키지에 두**어야 한다**.” 테스트는 `assertThatExceptionOfType(IllegalStateException.class)`를 쓴다.
- 제안: 웹 어댑터가 구분해서 처리하는 업무 예외에만 패키지 규칙을 적용할지 결정한다. 모든 명시적 실패에 적용하려는 의도라면 이 저장 실패도 들어오는 포트의 예외로 표현하고 500 매핑을 유지한다.

### DOC13. R6·R7의 검증 방법이 ADR-0004의 테스트 분류에서 빠진다

- 심각도: 낮음
- 종류: 확인
- 위치: `docs/adr/0004-verification-tdd.md:12`, `docs/adr/0004-verification-tdd.md:16`, `docs/domain/01-member.md:61`, `src/test/java/trealworld/adapter/persistence/MemberPersistenceAdapterTests.java:23`, `src/test/java/trealworld/application/service/RegisterMemberServiceTests.java:123`
- 내용: ADR-0004는 규칙 R을 Spring 없는 도메인 단위 테스트에 대응시킨다. Domain 01은 회원 하나로 R6·R7을 보장할 수 없다고 설명한다. 실제로 가입 서비스 테스트는 저장 사이의 중복을, Spring을 띄운 영속성 어댑터 테스트는 DB의 유일성 제약을 확인한다. 현재 분류로는 두 검사의 목적과 위치를 설명할 수 없다.
- 근거: ADR은 “테스트는 세 종류다”라고 한다. 영속성 테스트는 `@SpringBootTest`에서 `members.save(...)`를 직접 부른다.
- 제안: 세 종류가 필수 검증 목록인지 전체 분류인지 밝힌다. 여러 회원에 걸친 규칙은 유스케이스 테스트와 영속성 어댑터 테스트가 각각 무엇을 확인하는지 R6·R7을 예로 들어 적는다.

### DOC14. AC-3.4와 Plan 01에서 요구 수준을 일관되게 읽을 수 없다

- 심각도: 낮음
- 종류: 확인
- 위치: `docs/spec/01-member-signup-login.md:21`, `docs/spec/01-member-signup-login.md:63`, `docs/plan/01-member-slice.md:8`, `docs/plan/01-member-slice.md:35`, `tstack/skills/t-mode/references/doc-style.md:14`, `tstack/skills/t-mode/references/doc-style.md:17`, `tstack/skills/t-mode/references/doc-style.md:20`
- 내용: AC-3.4 한 행의 수준은 MUST NOT인데 두 번째 문장은 MUST다. 서로 같은 의도로 읽혀도 표의 수준과 문장이 일치하지 않는다. Plan 01은 종료 조건과 작업 순서를 평서문으로만 쓰고 읽는 법도 생략했다. 특히 테스트 실행과 사용자 확인이 필수인지 문서 표기만으로 식별하기 어렵다.
- 근거: AC-3.4는 “**절대** 돌려줘**서는 안 된다**”에 “조회의 대상은 **반드시** 토큰의 주인이**어야 한다**”를 잇는다. Plan 01은 “작업 하나가 끝나면 전체 테스트를 돌리고, 일지를 쓰고, 커밋한다”라고 쓴다.
- 제안: AC-3.4의 ID를 유지하고 금지 대상과 기준을 한 문장에 넣는다. 예를 들어 “시스템은 토큰의 주인 외에 다른 회원의 정보를 **절대** 돌려줘**서는 안 된다**”로 쓴다. Plan 01의 머리에서 공통 키워드 표를 가리키고 필수 절차와 종료 조건에는 적용 주체와 요구 수준을 표시한다.

### DOC15. 짧은 서명 키가 공백으로만 구성된 경우의 시작 조건이 다르다

- 심각도: 중간
- 종류: 확인
- 위치: `docs/adr/0003-authentication.md:24`, `docs/adr/0003-authentication.md:25`, `docs/adr/0003-authentication.md:27`, `src/main/java/trealworld/adapter/security/JwtSigningKey.java:30`, `src/test/java/trealworld/adapter/security/JwtSigningKeyTests.java:22`, `src/test/java/trealworld/adapter/security/JwtSigningKeyTests.java:29`
- 내용: ADR-0003은 32바이트 미만이면 시작을 거부하고, 설정이 없으면 임의 키를 만든다고 정했다. 코드에서는 명시적으로 설정한 공백 한 칸도 `isBlank()` 분기로 들어가 임의 키를 만든다. 이 분기에서는 길이를 검사하지 않는다. 테스트는 짧은 일반 문자열, 빈 문자열, `null`을 다룬다.
- 근거: ADR의 “더 짧으면 서버는 시작을 **반드시** 거부**해야 한다**”에 해당하는 값이 코드의 `secret == null || secret.isBlank()`에도 해당한다.
- 제안: 미설정, 빈 문자열, 공백만 있는 문자열을 표에서 구별한다. 명시한 공백을 설정 오류로 볼지 미설정과 같게 볼지 결정한 뒤, 시작 조건과 테스트 기대값을 맞춘다.

## 확인했지만 문제가 없는 것

- AC 19개와 R 10개에 대응하는 테스트 이름, Plan 01 작업 1~7의 일지 기록, ADR-0002 규칙 1~5의 ArchUnit 테스트 존재 여부 — 발견 없음.
- UUID 전환의 이유, 고정 서명 키와 인메모리 DB라는 재시작 조건, 버린 대안 세 가지, 옛 토큰으로 새 회원을 조회하지 못하도록 검사하는 수용 테스트 — 발견 없음.
- ADR-0001이 정한 Spring Boot·Java 버전과 Gradle 파일의 선언, H2와 보안 의존성 — 발견 없음. 기술 스택의 선택 이유와 대안을 비교하지 않았다는 사실도 ADR에 명시했다.
