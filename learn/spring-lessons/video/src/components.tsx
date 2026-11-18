import React from "react";
import {
  AbsoluteFill,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
  Easing,
} from "remotion";
import { C, FONT, MONO } from "./theme";

// ---------- helpers ----------
/** Maps the real frame to the frame the scene animations were authored for (see Video.tsx). Identity by default. */
export const TimeWarp = React.createContext<(frame: number) => number>((f) => f);

export const useT = () => {
  const warp = React.useContext(TimeWarp);
  const frame = warp(useCurrentFrame());
  const { fps } = useVideoConfig();
  return { frame, fps, t: frame / fps };
};

export const pop = (frame: number, fps: number, delay = 0, damping = 14) =>
  spring({ frame: frame - delay, fps, config: { damping, stiffness: 140, mass: 0.8 } });

export const fadeIn = (frame: number, start: number, len = 12) =>
  interpolate(frame, [start, start + len], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.out(Easing.cubic),
  });

export const slideUp = (frame: number, fps: number, delay = 0, dist = 40) => {
  const s = pop(frame, fps, delay);
  return { opacity: Math.min(1, s * 1.4), transform: `translateY(${(1 - s) * dist}px)` };
};

// ---------- background ----------
export const Background: React.FC<{ hue?: number }> = ({ hue = 0 }) => {
  const { t } = useT();
  const a = Math.sin(t * 0.35) * 120;
  const b = Math.cos(t * 0.27) * 100;
  return (
    <AbsoluteFill style={{ background: `radial-gradient(1200px 800px at 70% 20%, ${C.bg2}, ${C.bg})` }}>
      <div
        style={{
          position: "absolute",
          width: 900,
          height: 900,
          borderRadius: "50%",
          left: 1200 + a,
          top: -350 + b,
          background: `radial-gradient(circle, ${C.accent}33, transparent 60%)`,
          filter: "blur(40px)",
        }}
      />
      <div
        style={{
          position: "absolute",
          width: 800,
          height: 800,
          borderRadius: "50%",
          left: -250 - b,
          top: 500 + a * 0.6,
          background: `radial-gradient(circle, ${C.accent2}2A, transparent 60%)`,
          filter: "blur(40px)",
        }}
      />
      {/* grid */}
      <AbsoluteFill
        style={{
          backgroundImage: `linear-gradient(${C.line}55 1px, transparent 1px), linear-gradient(90deg, ${C.line}55 1px, transparent 1px)`,
          backgroundSize: "80px 80px",
          maskImage: "radial-gradient(ellipse at center, black 30%, transparent 85%)",
          opacity: 0.5,
        }}
      />
    </AbsoluteFill>
  );
};

// ---------- scene chrome ----------
export const SceneHeader: React.FC<{ index: number; total: number; title: string }> = ({
  index,
  total,
  title,
}) => {
  const { frame, fps } = useT();
  const s = pop(frame, fps, 2);
  return (
    <div style={{ position: "absolute", top: 56, left: 96, right: 96, display: "flex", alignItems: "center", gap: 24 }}>
      <div
        style={{
          fontFamily: MONO,
          fontSize: 26,
          color: C.accent,
          border: `2px solid ${C.accent}`,
          borderRadius: 10,
          padding: "6px 14px",
          opacity: s,
          transform: `scale(${0.8 + s * 0.2})`,
        }}
      >
        {String(index).padStart(2, "0")} / {String(total).padStart(2, "0")}
      </div>
      <div
        style={{
          fontFamily: FONT,
          fontSize: 40,
          fontWeight: 700,
          color: C.text,
          letterSpacing: -0.5,
          opacity: s,
          transform: `translateX(${(1 - s) * -30}px)`,
        }}
      >
        {title}
      </div>
    </div>
  );
};

export const ProgressBar: React.FC<{ progress: number; segments: number[] }> = ({ progress, segments }) => (
  <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 6, background: C.line }}>
    <div style={{ width: `${progress * 100}%`, height: "100%", background: C.accent, transition: "none" }} />
    {segments.map((p, i) => (
      <div key={i} style={{ position: "absolute", left: `${p * 100}%`, top: 0, width: 2, height: "100%", background: C.bg }} />
    ))}
  </div>
);

// ---------- subtitle ----------
export type SubtitleSpan = { start: number; end: number }; // seconds from the scene's first word

export const Subtitle: React.FC<{ text: string; sceneDur: number; sentenceSpans?: SubtitleSpan[] }> = ({ text, sceneDur, sentenceSpans }) => {
  const { t } = useT();
  const sentences = text.split(/(?<=[.!?。])\s+/).filter(Boolean);
  const total = sentences.reduce((a, s) => a + s.length, 0);
  let spans: { s: string; start: number; end: number }[];
  if (sentenceSpans && sentenceSpans.length === sentences.length) {
    // exact timing from timeline.json; each subtitle holds until the next sentence starts
    spans = sentences.map((s, i) => ({ s, start: i === 0 ? 0 : sentenceSpans[i].start, end: i + 1 < sentences.length ? sentenceSpans[i + 1].start : sceneDur }));
  } else {
    // fallback: allocate time proportionally to characters
    let acc = 0;
    spans = sentences.map((s) => {
      const start = (acc / total) * sceneDur;
      acc += s.length;
      const end = (acc / total) * sceneDur;
      return { s, start, end };
    });
  }
  const cur = spans.find((x) => t >= x.start && t < x.end) ?? spans[spans.length - 1];
  const local = t - cur.start;
  const op = interpolate(local, [0, 0.25], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const y = interpolate(local, [0, 0.25], [14, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <div
      style={{
        position: "absolute",
        bottom: 72,
        left: 0,
        right: 0,
        display: "flex",
        justifyContent: "center",
      }}
    >
      <div
        style={{
          maxWidth: 1500,
          fontFamily: FONT,
          fontSize: 38,
          lineHeight: 1.35,
          fontWeight: 500,
          color: C.text,
          background: "rgba(11,11,16,0.72)",
          border: `1px solid ${C.line}`,
          padding: "16px 32px",
          borderRadius: 16,
          textAlign: "center",
          wordBreak: "keep-all",
          opacity: op,
          transform: `translateY(${y}px)`,
          backdropFilter: "blur(6px)",
        }}
      >
        {cur.s}
      </div>
    </div>
  );
};

// ---------- building blocks ----------
export const Chip: React.FC<{
  children: React.ReactNode;
  delay?: number;
  color?: string;
  mono?: boolean;
  size?: number;
}> = ({ children, delay = 0, color = C.accent, mono, size = 30 }) => {
  const { frame, fps } = useT();
  const s = pop(frame, fps, delay, 12);
  return (
    <div
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 12,
        fontFamily: mono ? MONO : FONT,
        fontSize: size,
        fontWeight: 600,
        color: C.text,
        background: `${color}22`,
        border: `2px solid ${color}`,
        borderRadius: 14,
        padding: "12px 22px",
        opacity: s,
        transform: `scale(${0.6 + 0.4 * s}) translateY(${(1 - s) * 20}px)`,
        whiteSpace: "nowrap",
      }}
    >
      {children}
    </div>
  );
};

export const Panel: React.FC<{
  children: React.ReactNode;
  style?: React.CSSProperties;
  delay?: number;
  title?: string;
}> = ({ children, style, delay = 0, title }) => {
  const { frame, fps } = useT();
  const s = pop(frame, fps, delay, 16);
  return (
    <div
      style={{
        position: "relative",
        background: C.panel,
        border: `1px solid ${C.line}`,
        borderRadius: 20,
        boxShadow: "0 30px 80px rgba(0,0,0,0.45)",
        overflow: "hidden",
        opacity: s,
        transform: `translateY(${(1 - s) * 50}px) scale(${0.95 + 0.05 * s})`,
        ...style,
      }}
    >
      {title !== undefined && (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10,
            padding: "14px 18px",
            borderBottom: `1px solid ${C.line}`,
            background: "#161620",
          }}
        >
          <span style={{ width: 14, height: 14, borderRadius: 7, background: "#FF5F57" }} />
          <span style={{ width: 14, height: 14, borderRadius: 7, background: "#FEBC2E" }} />
          <span style={{ width: 14, height: 14, borderRadius: 7, background: "#28C840" }} />
          <span style={{ marginLeft: 14, fontFamily: MONO, fontSize: 20, color: C.muted }}>{title}</span>
        </div>
      )}
      {children}
    </div>
  );
};

/** Typewriter text; `start` = frame when typing begins, cps = chars per second */
export const Typed: React.FC<{
  text: string;
  start: number;
  cps?: number;
  color?: string;
  size?: number;
  cursor?: boolean;
}> = ({ text, start, cps = 28, color = C.text, size = 28, cursor = true }) => {
  const { frame, fps } = useT();
  const n = Math.max(0, Math.floor(((frame - start) / fps) * cps));
  const shown = text.slice(0, n);
  const done = n >= text.length;
  const blink = Math.floor(frame / 15) % 2 === 0;
  return (
    <span style={{ fontFamily: MONO, fontSize: size, color, whiteSpace: "pre" }}>
      {shown}
      {cursor && (!done || blink) && frame >= start ? (
        <span style={{ background: C.accent, color: C.accent }}>&nbsp;</span>
      ) : null}
    </span>
  );
};

export const Counter: React.FC<{ to: number; start: number; dur?: number; suffix?: string; size?: number }> = ({
  to,
  start,
  dur = 40,
  suffix = "",
  size = 120,
}) => {
  const { frame } = useT();
  const v = interpolate(frame, [start, start + dur], [0, to], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.out(Easing.cubic),
  });
  return (
    <span style={{ fontFamily: MONO, fontSize: size, fontWeight: 800, color: C.accent, letterSpacing: -4 }}>
      {Math.round(v).toLocaleString()}
      {suffix}
    </span>
  );
};

export const Arrow: React.FC<{ delay?: number; width?: number; color?: string }> = ({
  delay = 0,
  width = 120,
  color = C.accent,
}) => {
  const { frame, fps } = useT();
  const s = pop(frame, fps, delay, 18);
  return (
    <svg width={width} height={40} viewBox={`0 0 ${width} 40`} style={{ opacity: s }}>
      <line x1={0} y1={20} x2={(width - 16) * s} y2={20} stroke={color} strokeWidth={5} strokeLinecap="round" />
      <polygon
        points={`${width - 20},6 ${width},20 ${width - 20},34`}
        fill={color}
        style={{ opacity: s > 0.9 ? 1 : 0 }}
      />
    </svg>
  );
};

export const Pulse: React.FC<{ x: number; y: number; color?: string; r?: number }> = ({ x, y, color = C.accent, r = 60 }) => {
  const { t } = useT();
  const k = (t * 1.2) % 1;
  return (
    <div
      style={{
        position: "absolute",
        left: x - r * (1 + k),
        top: y - r * (1 + k),
        width: 2 * r * (1 + k),
        height: 2 * r * (1 + k),
        borderRadius: "50%",
        border: `3px solid ${color}`,
        opacity: 1 - k,
      }}
    />
  );
};
