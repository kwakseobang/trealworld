import React from "react";
import { AbsoluteFill, Easing, interpolate, random, spring } from "remotion";
import { C, FONT, MONO } from "./theme";
import { useT } from "./components";

/** `cues[i]` = local frame at which sentence i of the scene starts (from timeline.json). */
export type SceneProps = { durationInFrames: number; cues: number[] };

const CL = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

/** 0 → 1 between frames a and b (ease-out). */
export const ramp = (f: number, a: number, b: number) =>
  interpolate(f, [a, b], [0, 1], { ...CL, easing: Easing.out(Easing.cubic) });

export const sp = (f: number, fps: number, delay = 0, damping = 14, stiffness = 140) =>
  spring({ frame: f - delay, fps, config: { damping, stiffness, mass: 0.8 } });

export const shake = (f: number, start: number, dur = 16, amp = 12) =>
  f < start || f > start + dur ? 0 : Math.sin((f - start) * 2.4) * amp * (1 - (f - start) / dur);

/** Cue i, or a spaced fallback when the timeline has fewer sentences. */
export const q = (cues: number[], i: number) =>
  cues[i] ?? (cues[cues.length - 1] ?? 0) + 60 * (i - cues.length + 1);

export const gradText = (a: string = C.accent, b: string = C.accent2): React.CSSProperties => ({
  background: `linear-gradient(100deg, ${a}, ${b})`,
  WebkitBackgroundClip: "text",
  backgroundClip: "text",
  color: "transparent",
});

/** Content area between the header and the subtitle: 1728 x 685. */
export const Stage: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <AbsoluteFill style={{ top: 180, bottom: 215, left: 96, right: 96, width: "auto", height: "auto" }}>{children}</AbsoluteFill>
);

/** Shows children from frame `from` until `to`, cross-fading at both ends. */
export const Phase: React.FC<{ from: number; to?: number; fade?: number; children: React.ReactNode }> = ({ from, to, fade = 10, children }) => {
  const { frame } = useT();
  if (frame < from - 1 || (to !== undefined && frame > to + fade)) return null;
  const a = ramp(frame, from, from + fade);
  const b = to === undefined ? 1 : 1 - ramp(frame, to, to + fade);
  return (
    <AbsoluteFill style={{ opacity: Math.min(a, b), transform: `scale(${0.96 + 0.04 * a + (1 - b) * 0.05})` }}>{children}</AbsoluteFill>
  );
};

export const Particles: React.FC<{ count?: number }> = ({ count = 80 }) => {
  const { t } = useT();
  return (
    <AbsoluteFill>
      {Array.from({ length: count }, (_, i) => {
        const x = random(`x${i}`) * 1920;
        const size = 2 + random(`s${i}`) * 5;
        const speed = 14 + random(`v${i}`) * 44;
        const y = (((random(`y${i}`) * 1080 - t * speed) % 1080) + 1080) % 1080;
        const col = i % 5 === 0 ? C.accent2 : i % 7 === 0 ? C.yellow : C.accent;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: x + Math.sin(t * 0.6 + i) * 16,
              top: y,
              width: size,
              height: size,
              borderRadius: size,
              background: col,
              opacity: 0.2 + 0.35 * (0.5 + 0.5 * Math.sin(t * 1.5 + i)),
              boxShadow: `0 0 ${size * 3}px ${col}`,
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};

/** Giant outlined lesson number behind the content. */
export const BigNumber: React.FC<{ n: string; color?: string }> = ({ n, color = C.accent }) => {
  const { frame, fps, t } = useT();
  const s = sp(frame, fps, 0, 18, 80);
  return (
    <div
      style={{
        position: "absolute",
        right: 30,
        top: 150,
        fontFamily: MONO,
        fontWeight: 800,
        fontSize: 640,
        lineHeight: 1,
        color: "transparent",
        WebkitTextStroke: `3px ${color}`,
        opacity: 0.13 * s,
        transform: `translateX(${(1 - s) * 260}px) translateY(${Math.sin(t * 0.7) * 10}px) rotate(${(1 - s) * 10 - 4}deg)`,
      }}
    >
      {n}
    </div>
  );
};

/** Kinetic text: each word springs up. */
export const Words: React.FC<{
  text: string;
  start: number;
  size?: number;
  color?: string;
  weight?: number;
  stagger?: number;
  style?: React.CSSProperties;
  wordStyle?: React.CSSProperties;
}> = ({ text, start, size = 64, color = C.text, weight = 800, stagger = 4, style, wordStyle }) => {
  const { frame, fps } = useT();
  return (
    <div style={{ display: "flex", flexWrap: "wrap", gap: `0 ${size * 0.28}px`, fontFamily: FONT, fontSize: size, fontWeight: weight, color, letterSpacing: -1, lineHeight: 1.18, ...style }}>
      {text.split(" ").map((w, i) => {
        const s = sp(frame, fps, start + i * stagger, 13);
        return (
          <span key={i} style={{ display: "inline-block", opacity: Math.min(1, s * 1.5), transform: `translateY(${(1 - s) * size * 0.7}px) rotate(${(1 - s) * 6}deg)`, ...wordStyle }}>
            {w}
          </span>
        );
      })}
    </div>
  );
};

const TOKEN = /(\/\/.*$|"[^"]*"|@\w+|\b(?:public|private|static|final|record|class|if|throw|new|return|catch|try|void)\b)/g;
const tokenColor = (tok: string) =>
  tok.startsWith("//") ? C.muted : tok.startsWith('"') ? C.green : tok.startsWith("@") ? C.yellow : C.purple;

export const CodeLine: React.FC<{ text: string; size?: number; color?: string }> = ({ text, size = 26, color = C.text }) => (
  <span style={{ fontFamily: MONO, fontSize: size, color, whiteSpace: "pre" }}>
    {text.split(TOKEN).map((part, i) => (i % 2 === 1 ? <span key={i} style={{ color: tokenColor(part) }}>{part}</span> : <span key={i}>{part}</span>))}
  </span>
);

/** Code block revealed line by line. `hl[i]` tints line i. */
export const Code: React.FC<{ lines: string[]; start: number; stagger?: number; size?: number; hl?: Record<number, string>; hlStart?: number }> = ({
  lines,
  start,
  stagger = 6,
  size = 26,
  hl = {},
  hlStart,
}) => {
  const { frame } = useT();
  return (
    <div style={{ display: "flex", flexDirection: "column", padding: "22px 26px" }}>
      {lines.map((l, i) => {
        const p = ramp(frame, start + i * stagger, start + i * stagger + 10);
        const h = hl[i] ? ramp(frame, hlStart ?? start + lines.length * stagger, (hlStart ?? start + lines.length * stagger) + 12) : 0;
        return (
          <div
            key={i}
            style={{
              opacity: p,
              transform: `translateX(${(1 - p) * -24}px)`,
              lineHeight: 1.55,
              borderRadius: 8,
              margin: "0 -10px",
              padding: "0 10px",
              background: hl[i] ? `${hl[i]}${Math.round(h * 0x30).toString(16).padStart(2, "0")}` : "transparent",
              boxShadow: hl[i] ? `inset 4px 0 0 ${hl[i]}${Math.round(h * 255).toString(16).padStart(2, "0")}` : "none",
            }}
          >
            <CodeLine text={l || " "} size={size} />
          </div>
        );
      })}
    </div>
  );
};

/** A rubber stamp that slams in. */
export const Stamp: React.FC<{ text: string; start: number; color?: string; size?: number; rotate?: number }> = ({ text, start, color = C.yellow, size = 64, rotate = -8 }) => {
  const { frame, fps } = useT();
  if (frame < start) return null;
  const s = sp(frame, fps, start, 9, 220);
  return (
    <div
      style={{
        display: "inline-block",
        fontFamily: FONT,
        fontWeight: 900,
        fontSize: size,
        color,
        border: `6px solid ${color}`,
        borderRadius: 16,
        padding: "6px 28px",
        transform: `rotate(${rotate}deg) scale(${2.6 - 1.6 * s})`,
        opacity: Math.min(1, s * 2),
        background: `${C.bg}DD`,
        boxShadow: `0 0 50px ${color}77`,
        whiteSpace: "nowrap",
        letterSpacing: 2,
      }}
    >
      {text}
    </div>
  );
};

/** Glass card with a spring entrance. */
export const Card: React.FC<{
  children: React.ReactNode;
  start?: number;
  color?: string;
  glow?: boolean;
  from?: "up" | "left" | "right" | "down";
  style?: React.CSSProperties;
}> = ({ children, start = 0, color = C.line, glow, from = "up", style }) => {
  const { frame, fps } = useT();
  const s = sp(frame, fps, start, 15);
  const d = (1 - s) * 70;
  const tr = from === "up" ? `translateY(${d}px)` : from === "down" ? `translateY(${-d}px)` : from === "left" ? `translateX(${-d}px)` : `translateX(${d}px)`;
  return (
    <div
      style={{
        position: "absolute",
        background: `linear-gradient(160deg, ${C.panel}F2, ${C.bg2}E6)`,
        border: `2px solid ${color}`,
        borderRadius: 22,
        boxShadow: glow ? `0 0 46px ${color}55, 0 24px 60px rgba(0,0,0,0.5)` : "0 24px 60px rgba(0,0,0,0.5)",
        opacity: Math.min(1, s * 1.4),
        transform: `${tr} scale(${0.94 + 0.06 * s})`,
        ...style,
      }}
    >
      {children}
    </div>
  );
};

export const Badge: React.FC<{ children: React.ReactNode; color: string; size?: number; solid?: boolean; style?: React.CSSProperties }> = ({ children, color, size = 30, solid, style }) => (
  <span
    style={{
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      fontFamily: MONO,
      fontWeight: 800,
      fontSize: size,
      color: solid ? C.bg : color,
      background: solid ? color : `${color}22`,
      border: `2px solid ${color}`,
      borderRadius: size * 0.4,
      padding: `${size * 0.14}px ${size * 0.5}px`,
      whiteSpace: "nowrap",
      ...style,
    }}
  >
    {children}
  </span>
);

/** Grid of dots that light up one after another. */
export const DotGrid: React.FC<{ n: number; cols: number; start: number; per?: number; size?: number; gap?: number; color?: (i: number) => string }> = ({
  n,
  cols,
  start,
  per = 0.2,
  size = 18,
  gap = 8,
  color = () => C.green,
}) => {
  const { frame } = useT();
  return (
    <div style={{ display: "grid", gridTemplateColumns: `repeat(${cols}, ${size}px)`, gap }}>
      {Array.from({ length: n }, (_, i) => {
        const p = ramp(frame, start + i * per, start + i * per + 8);
        const c = color(i);
        return (
          <div
            key={i}
            style={{
              width: size,
              height: size,
              borderRadius: size * 0.3,
              background: p > 0 ? c : C.line,
              opacity: 0.35 + 0.65 * p,
              transform: `scale(${0.6 + 0.4 * p + Math.sin(p * Math.PI) * 0.5})`,
              boxShadow: p > 0.5 ? `0 0 ${size * 0.7}px ${c}AA` : "none",
            }}
          />
        );
      })}
    </div>
  );
};

/** Draws a line through its children. */
export const Strike: React.FC<{ children: React.ReactNode; start: number; color?: string }> = ({ children, start, color = C.red }) => {
  const { frame } = useT();
  const p = ramp(frame, start, start + 10);
  return (
    <span style={{ position: "relative", display: "inline-block" }}>
      <span style={{ opacity: 1 - 0.5 * p }}>{children}</span>
      <span style={{ position: "absolute", left: -6, top: "50%", height: 5, width: `calc(${p * 100}% + ${p * 12}px)`, background: color, borderRadius: 3, boxShadow: `0 0 12px ${color}` }} />
    </span>
  );
};

/** SVG line that draws itself from (x1,y1) to (x2,y2). */
export const Wire: React.FC<{ x1: number; y1: number; x2: number; y2: number; start: number; color?: string; dur?: number; dashed?: boolean; head?: boolean; width?: number }> = ({
  x1,
  y1,
  x2,
  y2,
  start,
  color = C.accent,
  dur = 14,
  dashed,
  head,
  width = 4,
}) => {
  const { frame, t } = useT();
  const p = ramp(frame, start, start + dur);
  if (p <= 0) return null;
  const ex = x1 + (x2 - x1) * p;
  const ey = y1 + (y2 - y1) * p;
  const ang = (Math.atan2(y2 - y1, x2 - x1) * 180) / Math.PI;
  const k = (t * 0.8) % 1;
  return (
    <svg style={{ position: "absolute", left: 0, top: 0, overflow: "visible", pointerEvents: "none" }} width={1} height={1}>
      <line x1={x1} y1={y1} x2={ex} y2={ey} stroke={color} strokeWidth={width} strokeLinecap="round" strokeDasharray={dashed ? "10 12" : undefined} opacity={0.9} />
      {head && p > 0.95 ? <polygon points="-18,-11 0,0 -18,11" fill={color} transform={`translate(${x2},${y2}) rotate(${ang})`} /> : null}
      {p >= 1 ? <circle cx={x1 + (x2 - x1) * k} cy={y1 + (y2 - y1) * k} r={width * 1.4} fill={C.text} opacity={0.9} /> : null}
    </svg>
  );
};

export const Label: React.FC<{ children: React.ReactNode; size?: number; color?: string; mono?: boolean; weight?: number; style?: React.CSSProperties }> = ({
  children,
  size = 28,
  color = C.muted,
  mono,
  weight = 600,
  style,
}) => <div style={{ fontFamily: mono ? MONO : FONT, fontSize: size, fontWeight: weight, color, wordBreak: "keep-all", ...style }}>{children}</div>;
