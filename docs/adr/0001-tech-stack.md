# ADR-0001. 기술 스택

- 상태: 승인 (사용자가 결정, 2026-11-16). 수정 (2026-11-17): 프로젝트를 만들 때 확인한 버전을 적었다.

## 맥락

백엔드만 만든다. 원본과의 호환은 필요 없다. 예제보다 process가 중요하다.

## 결정

| 항목 | 선택 |
|---|---|
| 프레임워크 | Spring Boot 4.1.2 (2026-11-17의 최신 안정 버전) |
| 언어 | Java 25 |
| 빌드 | Gradle 9.9.1 (wrapper), Kotlin DSL |
| 영속성 | JPA |
| DB | H2 (인메모리) |
| 보조 도구 | Lombok |
| 테스트 | JUnit, ArchUnit |

## 결과

- 대안을 비교하지 않았다. 사용자가 익숙한 스택을 골랐다.
- Spring Boot의 버전은 Spring Initializr(start.spring.io)의 기본값에서 확인했다. SNAPSHOT과 RC는 뺐다.
- 기본 패키지는 `trealworld`다.
- 의존성은 쓰는 작업에서 더한다. ArchUnit 1.5.1은 작업 2에서 더했다. Spring Security와 검증은 아직 없다.
- H2는 인메모리다. 서버를 다시 시작하면 데이터가 사라진다. 시연에는 충분하다.
