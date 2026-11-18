# CLAUDE.md

## 프로젝트의 목적

- 이 프로젝트는 방법론 실험이다. "API spec → 의도 추출 → spec → domain → ADR → plan → 개발" 흐름을 검증한다.
- 과정을 세미나에서 사례로 발표한다. 예제(RealWorld)보다 **process**가 중요하다.
- 작업은 베이비 스텝으로 한다. 한 번에 한 단계만 하고, 사용자와 확인한 뒤에 다음으로 간다.

## 작업 일지

- 스텝이 끝날 때마다 `docs/journal/YYYY-MM-DD.md`에 **반드시 기록해야 한다.** (MUST)
- 스텝마다 쓰는 항목: 요청 또는 질문, 답, 한 일, 결정, 버린 대안, 관찰. 해당 없는 항목은 뺀다.
- 프로젝트 문서에는 결론만 쓴다. 과정(왜, 어떤 순서로, 무엇을 버렸는가)은 일지에 쓴다.

## 문서 언어

- 이 프로젝트의 모든 문서는 **반드시 한국어로 작성해야 한다.** (MUST)
- 대상: `docs/` 아래의 모든 문서(brainstorming, journal, spec, domain, adr, plan)와 이 파일.
- 코드 식별자, 파일 경로, API 필드 이름, 기술 용어는 원문(영어) 그대로 쓸 수 있다. (MAY)

## 문서의 문체

- 대상: 요구, 규칙, 결정을 적는 문서 (`docs/spec`, `docs/domain`, `docs/adr`, `docs/plan`)와 이 파일.
- 요구의 수준은 **반드시** RFC 2119의 키워드로 써**야 한다.** (MUST) 키워드는 굵게 쓴다.

  | 말 | RFC 2119 |
  |---|---|
  | **반드시 ~해야 한다** | MUST |
  | **절대 ~해서는 안 된다** | MUST NOT |
  | **~하는 것을 권장한다** | SHOULD |
  | **~할 수 있다** | MAY |

- 문장은 **반드시** ASD-STE100의 원칙으로 써**야 한다.** (MUST) 능동으로 쓴다. 문장은 짧게 쓴다. 한 문장에 요구는 하나다. 한 개념에 한 용어를 쓴다. 범위가 흐린 말을 쓰지 않는다.
- 자세한 규칙은 `tstack/skills/t-mode/references/doc-style.md`에 있다.
- 문서를 고친 뒤에 `python3 tstack/skills/t-mode/scripts/tstack-check.py keywords`를 실행하는 것을 **권장한다.** (SHOULD) 이 검사는 RFC 2119의 키워드만 본다. ASD-STE100의 검사는 없다.
