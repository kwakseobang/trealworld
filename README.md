# trealworld

[![CI](https://github.com/tobyilee/trealworld/actions/workflows/ci.yml/badge.svg)](https://github.com/tobyilee/trealworld/actions/workflows/ci.yml)

AI와 함께 "문서 → 테스트 → 코드"의 순서로 백엔드를 만드는 방법을 실험한 저장소다.

- 만든 것은 작다: [RealWorld](https://github.com/gothinkster/realworld)의 회원 기능 3개(가입, 로그인, 내 정보 조회).
- 보려는 것은 **과정**이다. 예제보다 과정이 중요하다.
- 과정은 스텝마다 [일지](docs/journal/)에 남겼다. 틀린 것과 고친 것도 남겼다.

## 흐름

이 실험이 검증하는 흐름이다. 앞의 문서가 뒤의 문서의 입력이다.

| 단계 | 하는 일 | 결과 |
|---|---|---|
| 1. 의도 추출 | 원본의 API와 테스트에서 "무엇을 하려는가"를 읽는다 | [킥오프](docs/brainstorming/00-kickoff.md) |
| 2. Spec | 의도를 수용 기준(AC)으로 쓴다. 기술을 말하지 않는다 | [Spec 01](docs/spec/01-member-signup-login.md) — AC 19개 |
| 3. Domain | 모델과 규칙(R)을 쓴다 | [Domain 01](docs/domain/01-member.md) — R 10개 |
| 4. ADR | 기술의 결정과 이유, 버린 대안을 쓴다 | [ADR](docs/adr/) 5개 |
| 5. Plan | 작업을 나누고 순서를 정한다 | [Plan 01](docs/plan/01-member-slice.md) — 작업 7개 |
| 6. 개발 | 실패하는 테스트 → 구현 → 통과 → 정리 | `src/` |
| 7. 리뷰 | 다른 리뷰어가 읽는다. 주장을 테스트로 옮기고 고친다 | [리뷰와 그 처리](docs/review/) |

## 만든 것

| 목표 | 요청 | 성공 |
|---|---|---|
| 가입 | `POST /api/members` | 201 |
| 로그인 | `POST /api/auth/login` | 200 |
| 내 정보 조회 | `GET /api/members/me` | 200 |

- 인증은 JWT(HS256)다. 토큰은 `Authorization: Bearer <토큰>` 헤더로 낸다.
- 오류의 본문은 RFC 9457 Problem Details다.
- 자세한 약속은 [ADR-0005](docs/adr/0005-api-conventions.md)에 있다.

## 실행

Java 25가 필요하다. DB는 인메모리(H2)다. 서버를 끄면 회원이 사라진다.

```bash
./gradlew bootRun
```

다른 터미널에서 요청한다.

```bash
# 가입 → 201 {"username":"jake","email":"jake@example.com"}
curl -i -X POST localhost:8080/api/members \
  -H 'Content-Type: application/json' \
  -d '{"username": "jake", "email": "jake@example.com", "password": "correct-horse-battery"}'

# 로그인 → 200 {"accessToken":"..."}
curl -i -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email": "jake@example.com", "password": "correct-horse-battery"}'

# 내 정보 조회 → 200 {"username":"jake","email":"jake@example.com"}
curl -i localhost:8080/api/members/me -H 'Authorization: Bearer <위에서 받은 토큰>'
```

설정은 1개다.

| 설정 | 뜻 | 없으면 |
|---|---|---|
| `trealworld.jwt.secret` | JWT의 서명 키. UTF-8로 32바이트 이상 | 서버가 시작할 때 임의의 키를 만든다. 다시 시작하면 발급한 토큰은 모두 무효다 |

## 구조

헥사고날 아키텍처다 ([ADR-0002](docs/adr/0002-hexagonal-architecture.md)). 의존은 안쪽으로만 향한다.

```text
src/main/java/trealworld
├── domain                  회원, 이메일, 사용자 이름, 비밀번호. Spring을 모른다
├── application
│   ├── port/in             유스케이스의 인터페이스 (가입, 로그인, 조회)
│   ├── port/out            밖에 기대는 것 (저장, 해시, 토큰의 발급)
│   └── service             유스케이스의 구현. Spring을 쓰지 않는다
└── adapter
    ├── web                 컨트롤러, 오류의 응답
    ├── persistence         JPA
    └── security            Spring Security, JWT, BCrypt
```

## 검증

규칙을 글로만 두지 않는다. 검사로 만들 수 있는 것은 검사로 만들었다.

| 무엇 | 도구 | 어기면 |
|---|---|---|
| 수용 기준과 도메인 규칙 | 테스트 247개. 이름이 ID로 시작한다 (`ac1_1_`, `r4_`, `adr0003_`) | 빌드 실패 |
| 의존의 방향 | ArchUnit ([ArchitectureTests](src/test/java/trealworld/ArchitectureTests.java)) | 빌드 실패 |
| 테스트가 결함을 잡는가 | mutation testing (PIT). `domain`과 `application`의 결함 61개를 모두 잡아야 한다 | 빌드 실패 |
| ID마다 테스트가 있는가 | `tstack-check.py trace` — 29 / 29 | CI 실패 |
| RFC 2119의 키워드 | `tstack-check.py keywords` | CI 실패 |
| 위의 전부 | [GitHub Actions](.github/workflows/ci.yml). push마다 돈다 | CI 실패 |
| 커밋하지 않은 변경을 지우는 git 명령 | Claude Code의 [hook](.claude/hooks/guard-git-discard.py) | 명령이 실행되지 않는다 |

```bash
./gradlew build
```

```bash
python3 tstack/skills/t-mode/scripts/tstack-check.py trace
```

```bash
python3 tstack/skills/t-mode/scripts/tstack-check.py keywords
```

보고서는 `build/reports/tests/test/index.html`과 `build/reports/pitest/index.html`에 생긴다.

## 문서의 지도

| 알고 싶은 것 | 읽을 곳 |
|---|---|
| 시스템이 무엇을 해야 하는가 | [docs/spec](docs/spec/) |
| 모델과 규칙 | [docs/domain](docs/domain/) |
| 왜 그렇게 정했는가. 무엇을 버렸는가 | [docs/adr](docs/adr/) |
| 어떤 순서로 만들었는가 | [docs/plan](docs/plan/) |
| 실제로 무슨 일이 있었는가 | [docs/journal](docs/journal/) — 스텝마다의 요청, 한 일, 결정, 버린 대안, 실수 |
| 다른 리뷰어가 무엇을 찾았는가 | [docs/review](docs/review/) — codex의 리뷰 4개와 [처리 문서](docs/review/2026-11-19-review-handling.md) |
| 일하는 규칙 | [CLAUDE.md](CLAUDE.md) |

문서는 결론만 쓴다. 과정은 일지에 쓴다. 문서는 한국어이고, 요구는 RFC 2119의 키워드(**반드시**, **절대**, **권장한다**, **할 수 있다**)로 쓴다.

## 과정을 읽는 다른 방법

- **설명 문서 11개** (`explain-diff-*.html`). 커밋 하나를 배경부터 설명하고 퀴즈로 끝난다. 브라우저에서 연다. 이 문서를 만드는 스킬이 저장소에 있다: [explain-diff](.claude/skills/explain-diff/SKILL.md). Claude Code에서 `/explain-diff <커밋>`으로 쓴다. 순서: [킥오프](explain-diff-kickoff-brainstorming.html) → [문서](explain-diff-member-slice-docs.html) → [뼈대](explain-diff-project-skeleton.html) → [아키텍처 테스트](explain-diff-architecture-tests.html) → [도메인](explain-diff-domain-model.html) → [가입](explain-diff-member-signup.html) → [로그인](explain-diff-member-login.html) → [조회](explain-diff-member-profile.html) → [오류의 응답](explain-diff-error-dispatch.html) → [문체](explain-diff-rfc2119-ste100-docs.html) → [전체 흐름](explain-diff-member-flow.html)
- **강의 영상** (3분). Spring 백엔드 개발자가 기억할 교훈 7가지: [spring-lessons.mp4](learn/spring-lessons/out/spring-lessons.mp4)
- **학습 게임 2개**. JWT를 게임으로 배운다: [토큰 마을](game-token-village.html), [토큰 문지기](game-token-gatekeeper.html)
- **AI와 일한 방식의 평가**: [ai-collab-scorecard.html](ai-collab-scorecard.html) ([PDF](ai-collab-scorecard.pdf))

## tstack

이 과정에서 쓴 방식을 Claude Code의 스킬 세트로 묶은 것이다: 문서를 먼저, 테스트를 먼저, 스텝마다 일지.

```text
/plugin install tstack --marketplace tobyilee/trealworld
```

- 설명: [tstack/README.md](tstack/README.md), [tstack-guide.md](tstack-guide.md) ([그림](tstack-guide.html))
- 설계의 배경: [like-pstack.md](like-pstack.md)

## 하지 않은 것

결함이 아니다. 범위의 밖이거나 받아들인 한계다 ([Spec 01](docs/spec/01-member-signup-login.md), [ADR-0003](docs/adr/0003-authentication.md)).

- 로그아웃, 토큰의 갱신과 취소가 없다.
- 로그인의 시도 횟수와 요청의 본문의 크기에 제한이 없다.
- 로그인의 응답 시간이 경우마다 다르다.
- DB는 인메모리다. 서버가 여러 대이면 서명 키를 설정해야 한다.
