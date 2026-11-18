# 강의 영상: Spring 백엔드 개발자가 기억할 교훈 7가지

- 길이: 약 3분. 장면 9개 (도입, 교훈 7개, 정리).
- 음성: `/Users/tobylee/workspace/ai/voice`의 `tts_pipeline.py`. 참조 음성은 `dongwooklee_eo`, 엔진은 `fish`다.
- 영상: Remotion. `video/`에 있다. 장면의 경계와 애니메이션의 시작은 `video/public/timeline.json`에서 읽는다.
- 결과: `out/spring-lessons.mp4`.

## 교훈과 근거

모든 교훈은 이 저장소의 일지(`docs/journal/2026-11-17.md`)에서 뽑았다. 숫자는 일지의 숫자다.

| # | 교훈 | 근거 (일지의 스텝) |
|---|---|---|
| 1 | 처음부터 초록인 테스트를 의심하라 | 스텝 25. ArchUnit 규칙 5개가 빈 패키지에서 모두 통과했다. 위반을 넣어서 red를 봤다 |
| 2 | 규칙은 타입 안에 넣어라 | 스텝 30, 35. 값 객체는 record다. 검사는 compact constructor에 있다. 도메인 테스트 30개가 0.05초에 돈다 |
| 3 | 유스케이스에서 프레임워크를 걷어내라 | 스텝 35. `application`에 Spring이 없다. `UseCaseConfig`가 빈으로 등록한다. 유스케이스 테스트는 0.01초, 가짜 포트는 30줄이다 |
| 4 | 확인하고 저장하는 코드는 경주다 | 스텝 37, 38. 같은 가입 12개 중 9개가 500이었다. 원인은 트랜잭션이 아니다. 고친 뒤: 201이 1개, 409가 11개 |
| 5 | 상태 코드만 보는 테스트를 조심하라 | 스텝 52. 오류로 넘기는 요청에 인증을 요구해서 400과 500이 401이 됐다. 테스트 203개가 놓쳤다 |
| 6 | 응답이 흘리는 정보를 통제하라 | 스텝 35, 40. 없는 이메일과 틀린 비밀번호의 응답이 바이트까지 같다. 짧은 비밀번호는 401이다. `toString`을 가렸다 |
| 7 | 결정에는 이유를 남겨라 | 스텝 35, 54, 66. 문서가 정하지 않은 결정이 작업 4에서 14개 나왔다. ADR-0003은 조건("DB가 인메모리다")을 적었다 |

## 대본

대본의 원문은 `scenes.json`이다. `display`는 자막이고, `tts`는 발음에 맞춘 글이다.

## 화면에 쓴 코드

실제 소스에서 가져왔다. 화면의 폭에 맞게 줄였다.

- `src/main/java/trealworld/domain/Email.java` (null 검사와 길이 검사는 뺐다)
- `src/main/java/trealworld/domain/Password.java`의 `toString`
- `src/main/java/trealworld/adapter/security/SecurityConfig.java`
- `src/main/java/trealworld/adapter/persistence/MemberPersistenceAdapter.java`
- `src/main/java/trealworld/UseCaseConfig.java`

## 다시 만드는 방법

```bash
cd /Users/tobylee/workspace/ai/voice
P=/Users/tobylee/workspace/sandbox/trealworld/learn/spring-lessons
.venv/bin/python script/tts_pipeline.py $P/scenes.json $P/video/public dongwooklee_eo --engine fish
cd $P/video && npm install
npx remotion render src/index.ts Main out/spring-lessons.mp4 --codec h264 --crf 18 --audio-codec aac
ffmpeg -y -i out/spring-lessons.mp4 -c:v copy -af "loudnorm=I=-16:TP=-1.5:LRA=11" -c:a aac -b:a 192k ../out/spring-lessons.mp4
```
