import React from "react";
import { interpolate } from "remotion";
import { C, FONT, MONO } from "./theme";
import { Counter, Panel, Typed, useT } from "./components";
import { Badge, BigNumber, Card, Code, CodeLine, DotGrid, Label, Phase, SceneProps, Stage, Stamp, Strike, Wire, Words, gradText, q, ramp, shake, sp } from "./fx";

const abs = (left: number, top: number, extra: React.CSSProperties = {}): React.CSSProperties => ({ position: "absolute", left, top, ...extra });

// ------------------------------------------------------------------ S1 intro
export const S1Intro: React.FC<SceneProps> = ({ cues }) => {
  const { frame, fps, t } = useT();
  const [c0, c1, c2, c3] = [0, 1, 2, 3].map((i) => q(cues, i));
  const apis = [
    { m: "POST", p: "/api/members", k: "회원 가입", c: C.accent },
    { m: "POST", p: "/api/auth/login", k: "로그인", c: C.accent },
    { m: "GET", p: "/api/members/me", k: "내 정보 조회", c: C.accent2 },
  ];
  const step = Math.max(10, Math.min(26, (c1 - c0) / 3.6));
  const three = sp(frame, fps, c1 + 4, 9, 160);
  return (
    <Stage>
      <Phase from={0} to={c2}>
        <div style={abs(40, 110, { display: "flex", flexDirection: "column", gap: 40 })}>
          {apis.map((a, i) => {
            const s = sp(frame, fps, c0 + 2 + i * step, 14);
            return (
              <div key={a.p} style={{ display: "flex", alignItems: "center", gap: 30, opacity: Math.min(1, s * 1.4), transform: `translateX(${(1 - s) * -220}px)` }}>
                <Badge color={a.c} size={40} solid style={{ width: 150 }}>
                  {a.m}
                </Badge>
                <span style={{ fontFamily: MONO, fontSize: 50, fontWeight: 700, color: C.text }}>{a.p}</span>
                <span style={{ fontFamily: FONT, fontSize: 40, fontWeight: 600, color: C.muted }}>{a.k}</span>
              </div>
            );
          })}
        </div>
        <div style={abs(1250, 20, { width: 440, height: 560, display: "flex", alignItems: "center", justifyContent: "center" })}>
          {[0, 1, 2].map((i) => {
            const k = (t * 0.7 + i / 3) % 1;
            return (
              <div
                key={i}
                style={{
                  position: "absolute",
                  width: 300 + k * 260,
                  height: 300 + k * 260,
                  borderRadius: "50%",
                  border: `3px solid ${C.accent}`,
                  opacity: (1 - k) * 0.6 * Math.min(1, three),
                }}
              />
            );
          })}
          <div style={{ fontFamily: MONO, fontSize: 460, fontWeight: 800, lineHeight: 1, ...gradText(), opacity: Math.min(1, three * 1.5), transform: `scale(${0.3 + 0.7 * three}) rotate(${(1 - three) * -30}deg)` }}>3</div>
          <Label size={44} color={C.text} weight={800} style={abs(300, 400, { opacity: ramp(frame, c1 + 14, c1 + 26), letterSpacing: 6 })}>
            APIs
          </Label>
        </div>
      </Phase>

      <Phase from={c2} to={c3}>
        <div style={abs(0, 70, { width: 620, textAlign: "center" })}>
          <div style={{ transform: `scale(${1 + Math.sin(ramp(frame, c2, c2 + 44) * Math.PI) * 0.08})` }}>
            <Counter to={212} start={c2 + 2} dur={42} size={300} />
          </div>
          <Label size={54} color={C.text} weight={800} style={{ marginTop: -10 }}>
            tests
          </Label>
          <div style={{ marginTop: 26, opacity: ramp(frame, c2 + 44, c2 + 56) }}>
            <Badge color={C.green} size={36}>
              ✓ BUILD SUCCESSFUL
            </Badge>
          </div>
        </div>
        <div style={abs(700, 110)}>
          <DotGrid n={212} cols={27} start={c2 + 2} per={0.2} size={26} gap={11} color={(i) => (i % 9 === 0 ? C.accent2 : C.green)} />
        </div>
      </Phase>

      <Phase from={c3}>
        <div style={abs(0, 40, { width: 1728, display: "flex", flexDirection: "column", alignItems: "center" })}>
          <Words text="백엔드 개발자가 꼭 기억해야 할" start={c3 + 2} size={62} color={C.muted} weight={700} style={{ justifyContent: "center" }} />
          <Words text="일곱 가지 교훈" start={c3 + 16} size={190} stagger={6} style={{ justifyContent: "center", marginTop: 6, letterSpacing: -6 }} wordStyle={gradText(C.accent, C.accent2)} />
          <div style={{ display: "flex", gap: 26, marginTop: 44 }}>
            {[1, 2, 3, 4, 5, 6, 7].map((n, i) => {
              const s = sp(frame, fps, c3 + 40 + i * 5, 10, 180);
              return (
                <div
                  key={n}
                  style={{
                    width: 92,
                    height: 92,
                    borderRadius: 46,
                    border: `3px solid ${i % 2 ? C.accent2 : C.accent}`,
                    background: `${i % 2 ? C.accent2 : C.accent}22`,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontFamily: MONO,
                    fontSize: 46,
                    fontWeight: 800,
                    color: C.text,
                    opacity: Math.min(1, s * 1.5),
                    transform: `scale(${s}) translateY(${Math.sin(t * 2 + i) * 6}px)`,
                  }}
                >
                  {n}
                </div>
              );
            })}
          </div>
          <div style={{ display: "flex", gap: 18, marginTop: 44, opacity: ramp(frame, c3 + 80, c3 + 94) }}>
            {["Java 25", "Spring Boot 4", "Hexagonal", "TDD", "ArchUnit"].map((x) => (
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

// ------------------------------------------------------------------ S2 a test that is green from the start
export const S2Green: React.FC<SceneProps> = ({ cues }) => {
  const { frame, fps, t } = useT();
  const [, c1, c2, c3, c4] = [0, 1, 2, 3, 4].map((i) => q(cues, i));
  const rules = ["domain은 application을 모른다", "application은 adapter를 모른다", "domain에 Spring이 없다", "adapter끼리 의존하지 않는다", "web은 port.in만 쓴다"];
  const failAt = c4 + 26;
  const failed = frame >= failAt;
  const pkgs = ["trealworld.domain", "trealworld.application", "trealworld.adapter"];
  const pulse = frame >= c2 && frame < c3 ? 0.5 + 0.5 * Math.sin((frame - c2) * 0.35) : 0;
  const drop = sp(frame, fps, c4 + 2, 11, 160);
  return (
    <>
      <BigNumber n="1" />
      <Stage>
        <div style={abs(0, 6, { width: 900, transform: `translateX(${shake(frame, failAt)}px)` })}>
          <Panel title="ArchitectureTests.java" delay={4}>
            <div style={{ padding: "20px 28px 24px", display: "flex", flexDirection: "column", gap: 13 }}>
              {rules.map((r, i) => {
                const p = sp(frame, fps, c1 + 6 + i * 8, 12, 200);
                const bad = failed && i === 0;
                const col = bad ? C.red : C.green;
                return (
                  <div key={r} style={{ display: "flex", alignItems: "center", gap: 18, opacity: 0.25 + 0.75 * Math.min(1, p) }}>
                    <div
                      style={{
                        width: 46,
                        height: 46,
                        borderRadius: 23,
                        background: p > 0.05 ? col : C.line,
                        color: C.bg,
                        fontSize: 30,
                        fontWeight: 900,
                        fontFamily: FONT,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        transform: `scale(${0.4 + 0.6 * p})`,
                        boxShadow: p > 0.5 ? `0 0 22px ${col}AA` : "none",
                      }}
                    >
                      {bad ? "✕" : "✓"}
                    </div>
                    <span style={{ fontFamily: MONO, fontSize: 24, color: C.muted }}>rule{i + 1}</span>
                    <span style={{ fontFamily: FONT, fontSize: 32, fontWeight: 700, color: bad ? C.red : C.text }}>{r}</span>
                  </div>
                );
              })}
              <div style={{ marginTop: 8, height: 54, borderRadius: 12, background: failed ? `${C.red}33` : `${C.green}26`, border: `2px solid ${failed ? C.red : C.green}`, display: "flex", alignItems: "center", justifyContent: "center", opacity: ramp(frame, c1 + 50, c1 + 60) }}>
                <span style={{ fontFamily: MONO, fontSize: 28, fontWeight: 800, color: failed ? C.red : C.green }}>{failed ? "4 passed · 1 FAILED" : "5 passed · 0 failed"}</span>
              </div>
            </div>
          </Panel>
        </div>

        {pkgs.map((p, i) => {
          const s = sp(frame, fps, c2 - 14 + i * 5, 14);
          return (
            <div
              key={p}
              style={abs(1010, 10 + i * 150, {
                width: 700,
                height: 126,
                borderRadius: 20,
                border: `3px dashed ${pulse > 0 ? C.yellow : C.line}`,
                background: `${C.panel}CC`,
                opacity: Math.min(1, s * 1.3),
                transform: `translateX(${(1 - s) * 90}px) scale(${1 + pulse * 0.025})`,
                boxShadow: pulse > 0 ? `0 0 ${30 * pulse}px ${C.yellow}66` : "none",
              })}
            >
              <div style={{ position: "absolute", left: 26, top: 18, fontFamily: MONO, fontSize: 30, fontWeight: 700, color: C.text }}>{p}</div>
              <div style={{ position: "absolute", left: 26, top: 70, fontFamily: FONT, fontSize: 28, color: pulse > 0 ? C.yellow : C.muted, fontWeight: 700 }}>검사할 클래스 0개</div>
            </div>
          );
        })}

        <div style={abs(150, 250, { opacity: 1 - ramp(frame, c4 - 4, c4 + 8), transform: `scale(${1 + ramp(frame, c4 - 4, c4 + 8) * 0.4})` })}>
          <Stamp text="증명한 것: 없음" start={c3 + 4} size={78} />
        </div>

        {/* a class that breaks rule 1 drops into the domain package */}
        {frame >= c4 ? (
          <>
            <div style={abs(1440, 28, { opacity: Math.min(1, drop * 1.5), transform: `translateY(${(1 - drop) * -220}px) rotate(${(1 - drop) * 20}deg)` })}>
              <Badge color={C.red} size={34} solid>
                V1.java
              </Badge>
            </div>
            <Wire x1={1560} y1={84} x2={1560} y2={158} start={c4 + 14} color={C.red} head width={6} />
            <Label mono size={22} color={C.red} style={abs(1585, 108, { opacity: ramp(frame, c4 + 20, c4 + 30) })}>
              import
            </Label>
          </>
        ) : null}
        <div style={abs(1010, 478, { width: 700, opacity: ramp(frame, failAt + 4, failAt + 14) })}>
          <Words text="위반을 넣고 RED를 먼저 본다" start={failAt + 4} size={50} style={{ justifyContent: "center" }} wordStyle={gradText(C.red, C.yellow)} />
          <Label size={30} style={{ textAlign: "center", marginTop: 14, opacity: 0.6 + 0.4 * Math.sin(t * 3) }}>
            초록 → <span style={{ color: C.red }}>빨강</span> → 초록
          </Label>
        </div>
      </Stage>
    </>
  );
};

// ------------------------------------------------------------------ S3 rules live inside the type
export const S3Types: React.FC<SceneProps> = ({ cues }) => {
  const { frame, fps } = useT();
  const [, c1, c2, c3, c4] = [0, 1, 2, 3, 4].map((i) => q(cues, i));
  const code = [
    "public record Email(String value) {",
    "  public Email {",
    "    if (!FORMAT.matcher(value).matches())",
    "      throw new IllegalArgumentException();",
    "    value = value.toLowerCase(Locale.ROOT);",
    "  }",
    "}",
  ];
  const vals = [
    { v: '"jake@"', ok: false },
    { v: '"a b@c.d"', ok: false },
    { v: '"Jake@Example.com"', ok: true, out: "jake@example.com" },
  ];
  const gateX = 1330;
  return (
    <>
      <BigNumber n="2" color={C.accent2} />
      <Stage>
        <div style={abs(0, 0, { width: 850 })}>
          <Panel title="domain/Email.java" delay={6}>
            <Code lines={code} start={c1 - 4} stagger={7} size={27} hl={{ 2: C.yellow, 3: C.yellow }} hlStart={c1 + 54} />
          </Panel>
        </div>

        {/* the constructor is a gate: a value that breaks the rule never becomes an object */}
        <div style={abs(gateX - 3, 0, { width: 6, height: 300, borderRadius: 3, background: `linear-gradient(${C.accent}, ${C.accent2})`, boxShadow: `0 0 26px ${C.accent}`, opacity: ramp(frame, c2 - 6, c2 + 6) })} />
        <Label mono size={24} color={C.accent} style={abs(gateX - 130, 306, { width: 260, textAlign: "center", opacity: ramp(frame, c2 - 6, c2 + 6) })}>
          new Email(value)
        </Label>
        {vals.map((x, i) => {
          const st = c2 + 4 + i * 22;
          const go = ramp(frame, st, st + 14);
          const after = ramp(frame, st + 14, st + 30);
          const hit = frame >= st + 14;
          const x0 = 900 + go * 150 + (x.ok ? after * 400 : -after * 70);
          const col = !hit ? C.text : x.ok ? C.green : C.red;
          return (
            <div key={x.v} style={abs(x0, 22 + i * 92, { opacity: go, transform: `translateX(${x.ok ? 0 : shake(frame, st + 14, 12, 14)}px)` })}>
              <Badge color={col} size={28} solid={hit && x.ok}>
                {hit ? (x.ok ? `✓ ${x.out}` : `✕ ${x.v}`) : x.v}
              </Badge>
            </div>
          );
        })}

        <div style={abs(900, 360, { width: 828, display: "flex", alignItems: "center", gap: 22, opacity: ramp(frame, c3, c3 + 10) })}>
          <Strike start={c3 + 22}>
            <Badge color={C.muted} size={28}>
              요청 객체 @Valid
            </Badge>
          </Strike>
          <span style={{ fontFamily: FONT, fontSize: 34, color: C.muted, fontWeight: 800 }}>+</span>
          <Badge color={C.accent} size={28} solid>
            도메인 record
          </Badge>
          <Label size={30} color={C.text} weight={800} style={{ opacity: ramp(frame, c3 + 30, c3 + 42) }}>
            규칙은 한 곳에만
          </Label>
        </div>

        <Card start={c4} color={C.accent} glow style={{ left: 0, top: 468, width: 1728, height: 210 }}>
          <div style={{ position: "absolute", left: 40, top: 22, display: "flex", alignItems: "baseline", gap: 18 }}>
            <Counter to={30} start={c4 + 4} dur={26} size={120} />
            <Label size={40} color={C.text} weight={800}>
              도메인 테스트
            </Label>
          </div>
          <div style={{ position: "absolute", left: 620, top: 34, width: 1060 }}>
            {[
              { k: "domain 테스트 30개", v: "0.05s", w: 0.035, c: C.green },
              { k: "contextLoads 1개 (Spring)", v: "1.6s", w: 1, c: C.muted },
            ].map((b, i) => (
              <div key={b.k} style={{ display: "flex", alignItems: "center", gap: 18, marginBottom: 22 }}>
                <Label size={28} color={C.text} style={{ width: 360 }}>
                  {b.k}
                </Label>
                <div style={{ width: 540, height: 34, borderRadius: 17, background: C.line, overflow: "hidden" }}>
                  <div style={{ width: `${b.w * 100 * ramp(frame, c4 + 14 + i * 10, c4 + 44 + i * 10)}%`, minWidth: 18, height: "100%", borderRadius: 17, background: b.c, boxShadow: `0 0 18px ${b.c}` }} />
                </div>
                <span style={{ fontFamily: MONO, fontSize: 34, fontWeight: 800, color: b.c }}>{b.v}</span>
              </div>
            ))}
          </div>
        </Card>
      </Stage>
    </>
  );
};

// ------------------------------------------------------------------ S4 ports and adapters
const hexPoints = (cx: number, cy: number, r: number) =>
  [0, 60, 120, 180, 240, 300].map((a) => `${cx + r * Math.cos((a * Math.PI) / 180)},${cy + r * Math.sin((a * Math.PI) / 180)}`).join(" ");

export const S4Ports: React.FC<SceneProps> = ({ cues }) => {
  const { frame, fps, t } = useT();
  const [c0, c1, c2, c3, c4] = [0, 1, 2, 3, 4].map((i) => q(cues, i));
  const cx = 864;
  const cy = 300;
  const R = 212;
  const draw = ramp(frame, c0 + 4, c0 + 40);
  const fake = ramp(frame, c4 + 4, c4 + 18);
  const svc = sp(frame, fps, c1 - 4, 14);
  const svcGone = ramp(frame, c1 + 30, c1 + 50);
  const adapters = [
    { name: "adapter.web", sub: "Controller", fakeSub: "Controller", x: 60, y: cy - 60, px: cx - R, py: cy, right: false },
    { name: "adapter.persistence", sub: "JPA · H2", fakeSub: "FakeRepository", x: 1290, y: 70, px: cx + R / 2, py: cy - R * 0.866, right: true },
    { name: "adapter.security", sub: "JWT · BCrypt", fakeSub: "FakeHasher", x: 1290, y: 376, px: cx + R / 2, py: cy + R * 0.866, right: true },
  ];
  return (
    <>
      <BigNumber n="3" />
      <Stage>
        <svg style={{ position: "absolute", left: 0, top: 0, overflow: "visible" }} width={1728} height={685}>
          <polygon points={hexPoints(cx, cy, R)} fill={`${C.accent}14`} stroke={C.accent} strokeWidth={6} strokeLinejoin="round" strokeDasharray={6 * R} strokeDashoffset={(1 - draw) * 6 * R} style={{ filter: `drop-shadow(0 0 18px ${C.accent})` }} />
          <polygon points={hexPoints(cx, cy + 34, 96)} fill={`${C.accent2}22`} stroke={C.accent2} strokeWidth={4} opacity={ramp(frame, c0 + 30, c0 + 44)} transform={`rotate(${Math.sin(t * 0.6) * 4} ${cx} ${cy + 34})`} />
        </svg>
        <Label size={34} color={C.text} weight={800} style={abs(cx - 150, cy - 150, { width: 300, textAlign: "center", opacity: ramp(frame, c0 + 26, c0 + 38) })}>
          application
        </Label>
        <Label size={28} color={C.accent2} weight={800} style={abs(cx - 100, cy + 16, { width: 200, textAlign: "center", opacity: ramp(frame, c0 + 34, c0 + 46) })}>
          domain
        </Label>

        {/* @Service leaves the hexagon; a config class outside wires the beans */}
        <div style={abs(cx - 96, cy - 96, { opacity: Math.min(1, svc * 1.4) * (1 - svcGone), transform: `translateY(${svcGone * -190}px) rotate(${svcGone * 24}deg) scale(${0.5 + 0.5 * svc})` })}>
          <Strike start={c1 + 16}>
            <Badge color={C.yellow} size={36}>
              @Service
            </Badge>
          </Strike>
        </div>
        <Card start={c1 + 30} color={C.yellow} from="down" style={{ left: cx - 200, top: 0, width: 400, height: 66, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <CodeLine text="UseCaseConfig  @Bean" size={27} />
        </Card>
        <Wire x1={cx} y1={70} x2={cx} y2={cy - R * 0.866 - 6} start={c1 + 42} color={C.yellow} dashed head />

        {/* ports and the adapters behind them */}
        {adapters.map((a, i) => {
          const st = c2 + 2 + i * 10;
          const edgeX = a.right ? a.x : a.x + 380;
          const col = a.right && fake > 0 ? C.yellow : C.accent2;
          return (
            <React.Fragment key={a.name}>
              <Wire x1={a.px} y1={a.py} x2={edgeX} y2={a.y + 60} start={st + 8} color={col} />
              <div style={abs(a.px - 16, a.py - 16, { width: 32, height: 32, borderRadius: 16, background: C.bg, border: `5px solid ${C.text}`, transform: `scale(${sp(frame, fps, st, 9, 200)})` })} />
              <Card start={st + 4} color={col} glow={a.right && fake > 0} from={a.right ? "right" : "left"} style={{ left: a.x, top: a.y, width: 380, height: 120 }}>
                <div style={{ position: "absolute", left: 24, top: 16, fontFamily: MONO, fontSize: 28, fontWeight: 800, color: C.text }}>{a.name}</div>
                <div style={{ position: "absolute", left: 24, top: 64, fontFamily: FONT, fontSize: 28, fontWeight: 700, color: col, opacity: a.right ? 1 - fake : 1 }}>{a.sub}</div>
                {a.right ? <div style={{ position: "absolute", left: 24, top: 64, fontFamily: MONO, fontSize: 28, fontWeight: 800, color: C.yellow, opacity: fake, transform: `translateY(${(1 - fake) * 20}px)` }}>{a.fakeSub}</div> : null}
              </Card>
            </React.Fragment>
          );
        })}
        <Label mono size={22} color={C.muted} style={abs(cx - R - 150, cy + 26, { opacity: ramp(frame, c2 + 20, c2 + 30) })}>
          port.in
        </Label>
        <Label mono size={22} color={C.muted} style={abs(cx + R + 26, cy - 16, { opacity: ramp(frame, c2 + 30, c2 + 40) })}>
          port.out
        </Label>

        {/* no application context needed */}
        <Card start={c3} color={C.line} from="left" style={{ left: 60, top: 524, width: 560, height: 150 }}>
          <div style={{ position: "absolute", left: 26, top: 18 }}>
            <Strike start={c3 + 22}>
              <CodeLine text="@SpringBootTest" size={34} />
            </Strike>
          </div>
          <div style={{ position: "absolute", left: 26, top: 84, right: 26, height: 22, borderRadius: 11, background: C.line, overflow: "hidden" }}>
            <div style={{ width: `${interpolate(frame, [c3 + 4, c3 + 22], [0, 62], { extrapolateLeft: "clamp", extrapolateRight: "clamp" })}%`, height: "100%", background: frame > c3 + 22 ? C.red : C.muted }} />
          </div>
          <Label size={22} style={{ position: "absolute", left: 26, top: 112 }}>
            컨텍스트를 띄우지 않는다
          </Label>
        </Card>

        <Card start={c4 + 12} color={C.green} glow style={{ left: 1060, top: 530, width: 610, height: 146 }}>
          <div style={{ position: "absolute", left: 30, top: 14, display: "flex", alignItems: "baseline", gap: 16 }}>
            <span style={{ fontFamily: MONO, fontSize: 96, fontWeight: 800, color: C.green, letterSpacing: -4 }}>0.01s</span>
            <Label size={30} color={C.text} weight={800}>
              유스케이스 테스트
            </Label>
          </div>
          <Label size={24} style={{ position: "absolute", left: 34, top: 112 }}>
            가짜 포트 30줄 · Spring 없음
          </Label>
        </Card>
      </Stage>
    </>
  );
};

// ------------------------------------------------------------------ S5 check-then-save is a race
export const S5Race: React.FC<SceneProps> = ({ cues }) => {
  const { frame } = useT();
  const [c0, c1, c2, c3, c4] = [0, 1, 2, 3, 4].map((i) => q(cues, i));
  const fix = ramp(frame, c4 + 46, c4 + 80);
  const arrived = ramp(frame, c1 + 40, c1 + 54);
  const n500 = 9 - Math.round(9 * fix);
  const n409 = 2 + Math.round(9 * fix);
  const tally = [
    { code: "201", n: 1, c: C.green },
    { code: "409", n: n409, c: C.yellow },
    { code: "500", n: n500, c: C.red },
  ];
  return (
    <>
      <BigNumber n="4" color={C.red} />
      <Stage>
        <div style={abs(0, 0, { opacity: ramp(frame, c0 + 4, c0 + 12) })}>
          <Typed text="if (!members.existsByEmail(email))  members.save(member);" start={c0 + 8} cps={34} size={30} />
        </div>
        <Label size={26} color={C.yellow} weight={800} style={abs(1090, 4, { opacity: ramp(frame, c0 + 60, c0 + 72) })}>
          ← 확인과 저장 사이의 틈
        </Label>

        {/* 12 identical sign-up requests at the same time */}
        {Array.from({ length: 12 }, (_, i) => {
          const st = c1 + 4 + (i % 4) * 2;
          const p = ramp(frame, st, st + 34);
          const y = 78 + i * 33;
          const base = i === 0 ? { code: "201", c: C.green } : i < 3 ? { code: "409", c: C.yellow } : { code: "500", c: C.red };
          const flipAt = c4 + 46 + (i - 3) * 3.5;
          const res = i >= 3 && frame >= flipAt ? { code: "409", c: C.yellow } : base;
          const done = p >= 1;
          const pop = ramp(frame, st + 34, st + 42);
          return (
            <React.Fragment key={i}>
              <div style={abs(30, y + 9, { width: p * 930, height: 4, borderRadius: 2, background: `linear-gradient(90deg, transparent, ${done ? res.c : C.text})`, opacity: 0.55 })} />
              <div style={abs(30 + p * 930 - 11, y, { width: 22, height: 22, borderRadius: 11, background: done ? res.c : C.text, boxShadow: `0 0 18px ${done ? res.c : C.text}`, opacity: p > 0 ? 1 : 0.25 })} />
              <div style={abs(1196, y - 5, { opacity: pop, transform: `scale(${0.5 + 0.5 * pop + (i >= 3 ? Math.sin(ramp(frame, flipAt, flipAt + 10) * Math.PI) * 0.35 : 0)})` })}>
                <Badge color={res.c} size={20} solid>
                  {res.code}
                </Badge>
              </div>
            </React.Fragment>
          );
        })}
        {/* the database with its unique constraint */}
        <div style={abs(1000, 66, { width: 170, height: 410, borderRadius: "85px / 34px", border: `4px solid ${C.accent2}`, background: `linear-gradient(${C.accent2}33, ${C.accent2}11)`, opacity: ramp(frame, c1 - 10, c1 + 2), boxShadow: `0 0 34px ${C.accent2}55` })}>
          <div style={{ position: "absolute", left: 0, right: 0, top: 150, textAlign: "center", fontFamily: MONO, fontSize: 40, fontWeight: 800, color: C.text }}>DB</div>
          <div style={{ position: "absolute", left: 0, right: 0, top: 210, textAlign: "center", fontFamily: MONO, fontSize: 20, fontWeight: 700, color: C.yellow, opacity: ramp(frame, c3, c3 + 10) }}>
            UNIQUE
            <br />
            (email)
          </div>
        </div>

        {/* tally */}
        <div style={abs(1330, 80, { display: "flex", flexDirection: "column", gap: 26, opacity: arrived })}>
          {tally.map((x) => (
            <div key={x.code} style={{ display: "flex", alignItems: "center", gap: 20, opacity: x.n === 0 ? 0.3 : 1 }}>
              <Badge color={x.c} size={46} solid style={{ width: 150 }}>
                {x.code}
              </Badge>
              <span style={{ fontFamily: MONO, fontSize: 84, fontWeight: 800, color: x.c, letterSpacing: -3 }}>× {Math.round(x.n * arrived)}</span>
            </div>
          ))}
        </div>

        {/* suspect, cause, fix */}
        <Card start={c2} style={{ left: 0, top: 506, width: 500, height: 172 }}>
          <Label size={24} style={{ position: "absolute", left: 26, top: 16 }}>
            용의자
          </Label>
          <div style={{ position: "absolute", left: 26, top: 60 }}>
            <CodeLine text="@Transactional" size={40} />
          </div>
          <div style={{ position: "absolute", left: 250, top: 96 }}>
            <Stamp text="범인 아님" start={c2 + 34} size={40} rotate={-8} />
          </div>
        </Card>
        <Card start={c3 + 6} color={C.red} style={{ left: 540, top: 506, width: 560, height: 172 }}>
          <Label size={24} color={C.red} style={{ position: "absolute", left: 26, top: 16 }}>
            진짜 원인 · 번역하는 코드가 없다
          </Label>
          <div style={{ position: "absolute", left: 26, top: 62 }}>
            <CodeLine text="DataIntegrityViolation" size={28} />
          </div>
          <div style={{ position: "absolute", left: 26, top: 110, display: "flex", alignItems: "center", gap: 14 }}>
            <CodeLine text="Exception  →  ???  →" size={28} color={C.muted} />
            <Badge color={C.red} size={26} solid>
              500
            </Badge>
          </div>
        </Card>
        <Card start={c4 + 4} color={C.green} glow style={{ left: 1140, top: 506, width: 588, height: 172 }}>
          <Label size={24} color={C.green} style={{ position: "absolute", left: 26, top: 16 }}>
            영속성 어댑터가 번역한다
          </Label>
          <div style={{ position: "absolute", left: 26, top: 60 }}>
            <CodeLine text="catch (DataIntegrity… e)" size={25} />
          </div>
          <div style={{ position: "absolute", left: 26, top: 98 }}>
            <CodeLine text="  throw new MemberNotUnique…(e);" size={25} />
          </div>
          <div style={{ position: "absolute", right: 22, top: 12 }}>
            <Badge color={C.yellow} size={26} solid>
              409
            </Badge>
          </div>
        </Card>
      </Stage>
    </>
  );
};
