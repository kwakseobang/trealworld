import React from "react";
import { C, FONT, MONO } from "./theme";
import { Arrow, Counter, Panel, Typed, useT } from "./components";
import { Badge, BigNumber, Card, CodeLine, DotGrid, Label, Phase, SceneProps, Stage, Stamp, Strike, Words, gradText, q, ramp, shake, sp } from "./fx";

const abs = (left: number, top: number, extra: React.CSSProperties = {}): React.CSSProperties => ({ position: "absolute", left, top, ...extra });

// ------------------------------------------------------------------ S6 a test that only looks at the status code
export const S6Status: React.FC<SceneProps> = ({ cues, durationInFrames }) => {
  const { frame, fps } = useT();
  const [c0, c1, c2, c3, c4] = [0, 1, 2, 3, 4].map((i) => q(cues, i));
  const boxes = [
    { t: "요청 + 유효한 토큰", s: c0 + 6, c: C.green },
    { t: "서버 안에서 오류", s: c0 + 20, c: C.red },
    { t: "forward → /error", s: c1 + 2, c: C.muted },
    { t: "authenticated() ?", s: c1 + 16, c: C.yellow },
  ];
  const flipAt = c1 + 50;
  const flip = ramp(frame, flipAt, flipAt + 14);
  const shown = flip < 0.5 ? { code: "500", c: C.red } : { code: "401", c: C.yellow };
  const ins = ramp(frame, c3 + 24, c3 + 38);
  const look = Math.round(c4 + (durationInFrames - c4) * 0.46);
  const ring = sp(frame, fps, look + 6, 10, 160);
  const lines = [
    { t: ".authorizeHttpRequests(requests -> requests", add: false, bad: false },
    { t: "  .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()", add: true, bad: false },
    { t: '  .requestMatchers(POST, "/api/members", …).permitAll()', add: false, bad: false },
    { t: "  .anyRequest().authenticated())", add: false, bad: true },
  ];
  return (
    <>
      <BigNumber n="5" color={C.yellow} />
      <Stage>
        {/* how a server error turns into 401 */}
        {boxes.map((b, i) => (
          <React.Fragment key={b.t}>
            <Card start={b.s} color={b.c} from="left" style={{ left: i * 395, top: 0, width: 340, height: 104, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <span style={{ fontFamily: i >= 2 ? MONO : FONT, fontSize: i >= 2 ? 25 : 30, fontWeight: 800, color: C.text }}>{b.t}</span>
            </Card>
            {i < 3 ? (
              <div style={abs(i * 395 + 343, 32)}>
                <Arrow delay={b.s + 8} width={50} color={C.muted} />
              </div>
            ) : null}
          </React.Fragment>
        ))}
        <div style={abs(1590, 10, { opacity: ramp(frame, c1 + 30, c1 + 40), transform: `perspective(600px) rotateX(${flip * 360}deg) translateX(${shake(frame, flipAt + 14)}px)` })}>
          <Badge color={shown.c} size={54} solid>
            {shown.code}
          </Badge>
        </div>

        {/* a user with a valid token is told the token is wrong */}
        <Card start={c2} color={C.red} from="left" style={{ left: 0, top: 150, width: 800, height: 250 }}>
          <div style={{ position: "absolute", left: 30, top: 28, width: 96, height: 96, borderRadius: 48, background: `${C.accent2}33`, border: `3px solid ${C.accent2}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 54 }}>🙂</div>
          <div style={{ position: "absolute", left: 152, top: 44 }}>
            <Badge color={C.green} size={30}>
              Bearer eyJhbGci… ✓ 유효한 토큰
            </Badge>
          </div>
          <div style={{ position: "absolute", left: 30, top: 146, right: 30, opacity: ramp(frame, c2 + 30, c2 + 42), transform: `translateY(${(1 - ramp(frame, c2 + 30, c2 + 42)) * 20}px)` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 18, background: `${C.red}22`, border: `2px solid ${C.red}`, borderRadius: 16, padding: "14px 20px" }}>
              <Badge color={C.red} size={30} solid>
                401
              </Badge>
              <span style={{ fontFamily: FONT, fontSize: 30, fontWeight: 700, color: C.text }}>“액세스 토큰이 없거나 유효하지 않다”</span>
            </div>
          </div>
        </Card>

        {/* the fix: let the server's own error dispatch through */}
        <div style={abs(840, 150, { width: 888 })}>
          <Panel title="adapter/security/SecurityConfig.java" delay={c3 - 2}>
            <div style={{ padding: "12px 22px 14px" }}>
              {lines.map((l) => (
                <div
                  key={l.t}
                  style={{
                    height: l.add ? 36 * ins : 36,
                    opacity: l.add ? ins : 1,
                    overflow: "hidden",
                    display: "flex",
                    alignItems: "center",
                    borderRadius: 8,
                    margin: "0 -8px",
                    padding: "0 8px",
                    background: l.add ? `${C.green}2E` : l.bad && ins < 1 && frame > c3 + 8 ? `${C.red}2A` : "transparent",
                    boxShadow: l.add ? `inset 4px 0 0 ${C.green}` : "none",
                  }}
                >
                  <CodeLine text={l.t} size={21.5} />
                </div>
              ))}
              <Label size={22} color={C.green} weight={800} style={{ marginTop: 8, opacity: ramp(frame, c3 + 40, c3 + 52) }}>
                + 오류로 넘기는 요청만 허용한다 (경로 /error가 아니다)
              </Label>
            </div>
          </Panel>
        </div>

        {/* 203 tests were green; the body told the truth */}
        <div style={abs(0, 452, { opacity: ramp(frame, c4, c4 + 10) })}>
          <DotGrid n={203} cols={41} start={c4 + 2} per={0.16} size={15} gap={5.5} />
          <div style={{ display: "flex", alignItems: "center", gap: 22, marginTop: 20 }}>
            <Badge color={C.green} size={34}>
              ✓ 203 passed
            </Badge>
            <Label size={30} color={C.red} weight={800} style={{ opacity: ramp(frame, c4 + 44, c4 + 56) }}>
              …결함은 그대로 통과했다
            </Label>
          </div>
        </div>
        <Card start={look - 8} color={C.accent2} glow from="right" style={{ left: 900, top: 446, width: 828, height: 236 }}>
          <div style={{ position: "absolute", left: 30, top: 12, display: "flex", flexDirection: "column", gap: 2 }}>
            <CodeLine text="HTTP/1.1 401" size={27} color={C.muted} />
            <CodeLine text='{ "status": 401,' size={27} />
            <CodeLine text='  "detail": "액세스 토큰이 없거나…",' size={27} />
            <CodeLine text='  "instance": "/error" }' size={27} />
          </div>
          <div style={{ position: "absolute", left: 44, top: 118, width: 400, height: 50, borderRadius: 26, border: `5px solid ${C.red}`, boxShadow: `0 0 26px ${C.red}`, opacity: Math.min(1, ring * 1.4), transform: `scale(${2 - ring})` }} />
          <Label size={30} color={C.text} weight={800} style={{ position: "absolute", left: 30, top: 180, opacity: ramp(frame, look + 16, look + 28) }}>
            본문이 <span style={{ color: C.red }}>진짜 이유</span>를 말한다
          </Label>
        </Card>
      </Stage>
    </>
  );
};

// ------------------------------------------------------------------ S7 what a response leaks
export const S7Leak: React.FC<SceneProps> = ({ cues }) => {
  const { frame } = useT();
  const [c0, c1, c2, c3, c4] = [0, 1, 2, 3, 4].map((i) => q(cues, i));
  const rows = [
    { k: "없는 이메일", body: '{ "email": "nobody@x.com" }' },
    { k: "틀린 비밀번호", body: '{ "password": "wrong-pw" }' },
  ];
  const scan = ramp(frame, c1 + 38, c1 + 74);
  const same = ramp(frame, c1 + 70, c1 + 82);
  const sw = ramp(frame, c3 + 30, c3 + 42);
  return (
    <>
      <BigNumber n="6" color={C.purple} />
      <Stage>
        {rows.map((r, i) => (
          <React.Fragment key={r.k}>
            <Card start={c0 + 6 + i * 14} from="left" style={{ left: 0, top: i * 158, width: 590, height: 136 }}>
              <Label size={26} color={C.yellow} weight={800} style={{ position: "absolute", left: 24, top: 14 }}>
                POST /api/auth/login · {r.k}
              </Label>
              <div style={{ position: "absolute", left: 24, top: 68 }}>
                <CodeLine text={r.body} size={30} />
              </div>
            </Card>
            <div style={abs(606, i * 158 + 48)}>
              <Arrow delay={c1 + i * 10} width={110} color={C.muted} />
            </div>
            <Card start={c1 + 8 + i * 10} color={same > 0 ? C.green : C.line} glow={same > 0} from="right" style={{ left: 736, top: i * 158, width: 680, height: 136 }}>
              <div style={{ position: "absolute", left: 24, top: 16 }}>
                <Badge color={C.yellow} size={28} solid>
                  401
                </Badge>
              </div>
              <div style={{ position: "absolute", left: 24, top: 74 }}>
                <CodeLine text='"이메일 또는 비밀번호가 틀리다"' size={30} />
              </div>
            </Card>
          </React.Fragment>
        ))}
        {/* a scanner compares the two responses byte by byte */}
        <div style={abs(736 + scan * 680, -10, { width: 5, height: 314, background: C.green, boxShadow: `0 0 30px 6px ${C.green}`, opacity: scan > 0 && scan < 1 ? 1 : 0 })} />
        <div style={abs(1450, 60, { width: 278, textAlign: "center", opacity: same, transform: `scale(${0.7 + 0.3 * same})` })}>
          <div style={{ fontFamily: MONO, fontSize: 130, fontWeight: 800, lineHeight: 1, ...gradText(C.green, C.accent) }}>0</div>
          <Label size={30} color={C.text} weight={800}>
            bytes differ
          </Label>
        </div>

        <Card start={c2} color={C.purple} style={{ left: 0, top: 346, width: 548, height: 330 }}>
          <Label size={30} color={C.purple} weight={800} style={{ position: "absolute", left: 28, top: 22 }}>
            toString까지 가린다
          </Label>
          <div style={{ position: "absolute", left: 28, top: 86 }}>
            <CodeLine text="record Password(String value)" size={25} />
          </div>
          <div style={{ position: "absolute", left: 28, top: 136 }}>
            <CodeLine text='log.info("{}", password);' size={25} color={C.muted} />
          </div>
          <div style={{ position: "absolute", left: 28, top: 206, right: 28, padding: "14px 18px", borderRadius: 14, background: C.bg, border: `1px solid ${C.line}` }}>
            <Typed text="Password[****]" start={c2 + 40} cps={22} size={36} color={C.green} />
          </div>
        </Card>

        <Card start={c3} color={C.yellow} style={{ left: 590, top: 346, width: 548, height: 330 }}>
          <Label size={30} color={C.yellow} weight={800} style={{ position: "absolute", left: 28, top: 22 }}>
            너무 짧은 비밀번호
          </Label>
          <div style={{ position: "absolute", left: 28, top: 86 }}>
            <CodeLine text='{ "password": "12345" }' size={30} />
          </div>
          <div style={{ position: "absolute", left: 28, top: 176, display: "flex", alignItems: "center", gap: 26 }}>
            <Strike start={c3 + 30}>
              <Badge color={C.muted} size={56}>
                400
              </Badge>
            </Strike>
            <span style={{ fontFamily: FONT, fontSize: 50, color: C.muted, opacity: sw }}>→</span>
            <div style={{ opacity: sw, transform: `scale(${0.5 + 0.5 * sw + Math.sin(sw * Math.PI) * 0.25})` }}>
              <Badge color={C.yellow} size={56} solid>
                401
              </Badge>
            </div>
          </div>
        </Card>

        <Card start={c4} color={C.red} style={{ left: 1180, top: 346, width: 548, height: 330 }}>
          <div style={{ position: "absolute", left: 28, top: 26, fontSize: 84, transform: `translateX(${shake(frame, c4 + 50, 20, 8)}px)` }}>🕵️</div>
          <div style={{ position: "absolute", left: 150, top: 34, right: 24, padding: "14px 18px", borderRadius: "18px 18px 18px 4px", background: `${C.red}22`, border: `2px solid ${C.red}`, opacity: ramp(frame, c4 + 12, c4 + 22) }}>
            <Label size={28} color={C.text} weight={700}>
              400이면… 최소 길이가 있구나?
            </Label>
          </div>
          <div style={{ position: "absolute", left: 70, top: 190 }}>
            <Stamp text="규칙을 알려 주지 않는다" start={c4 + 50} size={34} color={C.green} rotate={-5} />
          </div>
        </Card>
      </Stage>
    </>
  );
};

// ------------------------------------------------------------------ S8 write down why
export const S8Adr: React.FC<SceneProps> = ({ cues, durationInFrames }) => {
  const { frame, fps, t } = useT();
  const [c0, c1, c2, c3, c4] = [0, 1, 2, 3, 4].map((i) => q(cues, i));
  const sections = [
    { k: "맥락", v: "목표 2, 3에 액세스 토큰이 필요하다" },
    { k: "결정", v: "JWT · HS256 · BCrypt" },
    { k: "결과", v: "서명 키가 없으면 임의의 키를 쓴다" },
    { k: "버린 대안", v: "HTTP 세션 · 서버 저장 토큰" },
  ];
  const crack = Math.round(c4 + (durationInFrames - c4) * 0.5);
  const broken = frame >= crack;
  return (
    <>
      <BigNumber n="7" color={C.accent2} />
      <Stage>
        <Phase from={0} to={c3}>
          <div style={abs(20, 40, { display: "flex", gap: 6 })}>
            {"WHY".split("").map((ch, i) => {
              const s = sp(frame, fps, c0 + 4 + i * 6, 9, 170);
              return (
                <span key={ch} style={{ fontFamily: FONT, fontSize: 300, fontWeight: 900, lineHeight: 1, letterSpacing: -10, ...gradText(C.accent, C.accent2), display: "inline-block", opacity: Math.min(1, s * 1.5), transform: `translateY(${(1 - s) * 160 + Math.sin(t * 1.6 + i) * 8}px) rotate(${(1 - s) * (i - 1) * 24}deg)` }}>
                  {ch}
                </span>
              );
            })}
          </div>
          <Words text="결정에는 이유를 남긴다" start={c0 + 26} size={54} style={abs(30, 370, { width: 700 })} />

          {/* 14 decisions the documents did not make */}
          <div style={abs(820, 0, { width: 908, height: 330 })}>
            {Array.from({ length: 14 }, (_, i) => {
              const s = sp(frame, fps, c1 + 4 + i * 3, 11, 170);
              const col = i % 7;
              const row = Math.floor(i / 7);
              return (
                <div
                  key={i}
                  style={abs(col * 92, 20 + row * 124, {
                    width: 80,
                    height: 108,
                    borderRadius: 12,
                    background: `linear-gradient(160deg, ${C.panel}, ${C.bg2})`,
                    border: `2px solid ${i % 3 === 0 ? C.yellow : C.accent2}`,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontFamily: MONO,
                    fontSize: 34,
                    fontWeight: 800,
                    color: C.text,
                    opacity: Math.min(1, s * 1.5),
                    transform: `translateY(${(1 - s) * -140}px) rotate(${(1 - s) * 40 + Math.sin(t + i) * 3}deg)`,
                  })}
                >
                  ?
                </div>
              );
            })}
            <div style={abs(672, 30, { textAlign: "center", width: 236 })}>
              <Counter to={14} start={c1 + 6} dur={40} size={170} />
              <Label size={26} color={C.text} weight={800} style={{ marginTop: -6 }}>
                문서에 없던 결정
              </Label>
            </div>
          </div>

          {/* decisions pile up faster than they are confirmed */}
          <div style={abs(820, 350, { width: 908, opacity: ramp(frame, c2, c2 + 10) })}>
            {[
              { k: "AI와 결정이 쌓이는 속도", w: 1, c: C.red },
              { k: "사람이 확인하는 속도", w: 0.24, c: C.accent2 },
            ].map((b, i) => (
              <div key={b.k} style={{ marginBottom: 26 }}>
                <Label size={30} color={C.text} weight={800} style={{ marginBottom: 10 }}>
                  {b.k}
                </Label>
                <div style={{ height: 44, borderRadius: 22, background: C.line, overflow: "hidden" }}>
                  <div style={{ width: `${b.w * 100 * ramp(frame, c2 + 8 + i * 10, c2 + 60 + i * 10)}%`, height: "100%", borderRadius: 22, background: `linear-gradient(90deg, ${b.c}88, ${b.c})`, boxShadow: `0 0 20px ${b.c}` }} />
                </div>
              </div>
            ))}
          </div>
        </Phase>

        <Phase from={c3}>
          <Card start={c3 + 2} from="left" style={{ left: 0, top: 30, width: 720, height: 420 }}>
            <Label size={30} weight={800} style={{ position: "absolute", left: 30, top: 24 }}>
              코드가 보여 주는 것
            </Label>
            <div style={{ position: "absolute", left: 30, top: 84, fontFamily: FONT, fontSize: 130, fontWeight: 900, lineHeight: 1, color: C.muted, letterSpacing: -4 }}>WHAT</div>
            <div style={{ position: "absolute", left: 30, top: 250, right: 30, padding: "16px 18px", borderRadius: 14, background: C.bg, border: `1px solid ${C.line}` }}>
              <CodeLine text='implementation("…:spring-security' size={24} />
              <br />
              <CodeLine text='               -oauth2-jose")' size={24} />
            </div>
            <Label size={24} style={{ position: "absolute", left: 30, top: 366 }}>
              무엇을 골랐는가 — 여기까지만
            </Label>
          </Card>

          <Card start={c4 - 6} color={C.accent} glow from="right" style={{ left: 780, top: 0, width: 948, height: 650 }}>
            <div style={{ position: "absolute", left: 34, top: 24, display: "flex", alignItems: "center", gap: 18 }}>
              <Badge color={C.accent} size={32} solid>
                ADR-0003
              </Badge>
              <Label size={38} color={C.text} weight={800}>
                인증
              </Label>
              <Label size={38} weight={900} style={gradText(C.accent, C.accent2)}>
                = WHY
              </Label>
            </div>
            {sections.map((s, i) => {
              const p = sp(frame, fps, c4 + 4 + i * 9, 14);
              const last = i === 3;
              return (
                <div key={s.k} style={{ position: "absolute", left: 34, right: 34, top: 104 + i * 98, height: 82, borderRadius: 14, border: `2px solid ${last ? C.yellow : C.line}`, background: last ? `${C.yellow}1C` : `${C.bg}AA`, display: "flex", alignItems: "center", gap: 22, padding: "0 22px", opacity: Math.min(1, p * 1.4), transform: `translateX(${(1 - p) * 60}px)` }}>
                  <span style={{ fontFamily: FONT, fontSize: 30, fontWeight: 800, color: last ? C.yellow : C.accent, width: 150 }}>{s.k}</span>
                  <span style={{ fontFamily: FONT, fontSize: 28, fontWeight: 600, color: C.text }}>{s.v}</span>
                </div>
              );
            })}
            <div style={{ position: "absolute", left: 34, right: 34, top: 510, height: 136, opacity: ramp(frame, c4 + 46, c4 + 58) }}>
              <div style={{ display: "inline-block", transform: `translateX(${shake(frame, crack, 18, 14)}px) rotate(${broken ? -3 : 0}deg)` }}>
                <Badge color={broken ? C.red : C.accent2} size={30} solid={broken}>
                  조건: DB가 인메모리인 동안
                </Badge>
              </div>
              <Label size={34} color={C.text} weight={800} style={{ marginTop: 20, opacity: ramp(frame, crack + 8, crack + 20) }}>
                이유가 더는 맞지 않는 날 → <span style={{ color: C.red }}>결정을 다시 본다</span>
              </Label>
            </div>
          </Card>
        </Phase>
      </Stage>
    </>
  );
};

// ------------------------------------------------------------------ S9 outro
export const S9Outro: React.FC<SceneProps> = ({ cues }) => {
  const { frame, fps, t } = useT();
  const [, c1, c2, c3] = [0, 1, 2, 3].map((i) => q(cues, i));
  const g1 = (c2 - c1) / 3.3;
  const g2 = (c3 - c2) / 4.3;
  const items = [
    { k: "초록을 의심하라", at: c1 + 2 },
    { k: "규칙은 타입 안에", at: c1 + 2 + g1 },
    { k: "기술은 포트 뒤에", at: c1 + 2 + g1 * 2 },
    { k: "동시에 보내 보라", at: c2 + 2 },
    { k: "본문까지 확인하라", at: c2 + 2 + g2 },
    { k: "새는 정보를 막아라", at: c2 + 2 + g2 * 2 },
    { k: "이유를 기록하라", at: c2 + 2 + g2 * 3 },
  ];
  const cols = [C.accent, C.accent2, C.accent, C.red, C.yellow, C.purple, C.accent2];
  return (
    <Stage>
      <Phase from={0} to={c3}>
        <div style={abs(0, 0, { width: 1728, display: "grid", gridTemplateColumns: "1fr 1fr", gap: "22px 40px" })}>
          {items.map((it, i) => {
            const s = sp(frame, fps, Math.round(it.at), 12, 170);
            return (
              <div
                key={it.k}
                style={{
                  gridColumn: i === 6 ? "1 / span 2" : undefined,
                  justifySelf: i === 6 ? "center" : undefined,
                  width: i === 6 ? 844 : undefined,
                  height: 136,
                  borderRadius: 22,
                  border: `3px solid ${cols[i]}`,
                  background: `linear-gradient(120deg, ${cols[i]}30, ${C.panel}E6)`,
                  boxShadow: `0 0 ${30 * s}px ${cols[i]}55`,
                  display: "flex",
                  alignItems: "center",
                  gap: 30,
                  padding: "0 34px",
                  opacity: Math.min(1, s * 1.5),
                  transform: `translateX(${(1 - s) * (i % 2 ? 160 : -160)}px) scale(${0.85 + 0.15 * s})`,
                }}
              >
                <span style={{ fontFamily: MONO, fontSize: 84, fontWeight: 800, color: cols[i], letterSpacing: -4 }}>{i + 1}</span>
                <span style={{ fontFamily: FONT, fontSize: 56, fontWeight: 800, color: C.text, letterSpacing: -1 }}>{it.k}</span>
              </div>
            );
          })}
        </div>
      </Phase>

      <Phase from={c3} fade={14}>
        <div style={abs(0, 20, { width: 1728, display: "flex", flexDirection: "column", alignItems: "center" })}>
          <Words text="테스트가 통과했다는 사실보다" start={c3 + 6} size={66} color={C.muted} weight={700} style={{ justifyContent: "center" }} />
          <Words text="그 초록은" start={c3 + 46} size={170} stagger={8} style={{ justifyContent: "center", marginTop: 30, letterSpacing: -5 }} wordStyle={gradText(C.green, C.accent)} />
          <Words text="무엇을 증명하는가?" start={c3 + 64} size={170} stagger={8} style={{ justifyContent: "center", letterSpacing: -5 }} />
          <div style={{ display: "flex", gap: 16, marginTop: 44, opacity: ramp(frame, c3 + 110, c3 + 126), transform: `translateY(${Math.sin(t * 1.4) * 4}px)` }}>
            {["trealworld", "Java 25", "Spring Boot 4", "212 tests"].map((x) => (
              <Badge key={x} color={C.muted} size={28}>
                {x}
              </Badge>
            ))}
          </div>
        </div>
      </Phase>
    </Stage>
  );
};
