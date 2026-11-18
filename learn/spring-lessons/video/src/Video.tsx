import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, staticFile, useCurrentFrame, useVideoConfig, interpolate } from "remotion";
import { Background, ProgressBar, SceneHeader, Subtitle, TimeWarp } from "./components";
import { S1Intro, S2Green, S3Types, S4Ports, S5Race } from "./scenes1";
import { S6Status, S7Leak, S8Adr, S9Outro } from "./scenes2";
import { Particles, SceneProps } from "./fx";
import { C, FONT } from "./theme";

export type Scene = {
  id: string;
  title: string;
  display: string;
  start: number;
  end: number;
  duration: number;
  sentences?: { text: string; start: number; end: number }[];
};
export type Timeline = { fps: number; sampleRate: number; total: number; scenes: Scene[] };
/** `ref` (public/timeline_ref.json, optional) is the timeline the scene animations were authored against.
 *  When the narration is regenerated (other engine/seed/voice) every scene's clock is warped so that
 *  animation cues keyed to sentence N still fire when sentence N is spoken. */
export type VideoProps = { timeline: Timeline | null; ref?: Timeline | null };

const SCENES: Record<string, React.FC<SceneProps>> = {
  s1_intro: S1Intro,
  s2_green: S2Green,
  s3_types: S3Types,
  s4_ports: S4Ports,
  s5_race: S5Race,
  s6_status: S6Status,
  s7_leak: S7Leak,
  s8_adr: S8Adr,
  s9_outro: S9Outro,
};

const GAP = 0.7; // seconds of silence between scenes (matches tts_pipeline.py)

/** Sequence window of scene i: visuals start `lead` s before the voice and hold through the gap. */
const seqWindow = (scenes: Scene[], i: number, total: number, fps: number) => {
  const sc = scenes[i];
  const lead = i === 0 ? sc.start : GAP / 2;
  const from = Math.round((sc.start - lead) * fps);
  const nextStart = i + 1 < scenes.length ? scenes[i + 1].start - GAP / 2 : total;
  const dur = Math.round(nextStart * fps) - from;
  return { lead, from, dur };
};

/** Piecewise-linear map from this scene's local frame to the reference scene's local frame,
 *  with knots at 0, every sentence start and the scene end. Identity when there is no reference. */
const makeWarp = (sc: Scene, dur: number, lead: number, ref: Scene | undefined, refDur: number, refLead: number, fps: number) => {
  if (!ref || !sc.sentences || !ref.sentences || sc.sentences.length !== ref.sentences.length) return (f: number) => f;
  const knots = (s: Scene, d: number, l: number) => [0, ...s.sentences!.map((x) => (x.start - s.start + l) * fps), d];
  const a = knots(sc, dur, lead);
  const r = knots(ref, refDur, refLead);
  for (let i = 1; i < a.length; i++) if (a[i] <= a[i - 1] || r[i] <= r[i - 1]) return (f: number) => f;
  return (f: number) => interpolate(f, a, r, { extrapolateLeft: "extend", extrapolateRight: "extend" });
};

export const Video: React.FC<VideoProps> = ({ timeline, ref }) => {
  const frame = useCurrentFrame();
  const { fps, durationInFrames } = useVideoConfig();
  if (!timeline) {
    return (
      <AbsoluteFill style={{ background: C.bg, color: C.text, fontFamily: FONT, fontSize: 40, alignItems: "center", justifyContent: "center" }}>
        timeline.json not loaded
      </AbsoluteFill>
    );
  }
  const scenes = timeline.scenes;
  const progress = frame / durationInFrames;
  return (
    <AbsoluteFill style={{ background: C.bg }}>
      <Background />
      <Particles />
      <Audio src={staticFile("narration.wav")} />
      {scenes.map((sc, i) => {
        const { lead, from, dur } = seqWindow(scenes, i, timeline.total, fps);
        const ri = ref ? ref.scenes.findIndex((s) => s.id === sc.id) : -1;
        const rw = ref && ri >= 0 ? seqWindow(ref.scenes, ri, ref.total, fps) : null;
        const warp = makeWarp(sc, dur, lead, rw ? ref!.scenes[ri] : undefined, rw?.dur ?? 0, rw?.lead ?? 0, fps);
        const Comp = SCENES[sc.id];
        // local frame at which each sentence starts; scenes key their animations to these
        const cues = (sc.sentences ?? []).map((x) => Math.round((x.start - sc.start + lead) * fps));
        return (
          <Sequence key={sc.id} from={from} durationInFrames={dur} name={sc.id}>
            <SceneShell index={i + 1} total={scenes.length} scene={sc} lead={lead} durationInFrames={dur}>
              <TimeWarp.Provider value={warp}>{Comp ? <Comp durationInFrames={rw?.dur ?? dur} cues={cues} /> : null}</TimeWarp.Provider>
            </SceneShell>
          </Sequence>
        );
      })}
      <ProgressBar progress={progress} segments={scenes.map((s) => s.start / timeline.total)} />
    </AbsoluteFill>
  );
};

const SceneShell: React.FC<{
  index: number;
  total: number;
  scene: Scene;
  lead: number;
  durationInFrames: number;
  children: React.ReactNode;
}> = ({ index, total, scene, lead, durationInFrames, children }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const fadeOutStart = durationInFrames - Math.round(0.35 * fps);
  const opacity = interpolate(frame, [0, 8, fadeOutStart, durationInFrames], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const leadFrames = Math.round(lead * fps);
  return (
    <AbsoluteFill style={{ opacity }}>
      <SceneHeader index={index} total={total} title={scene.title} />
      {children}
      <Wipe />
      {/* subtitles start when the voice starts */}
      <Sequence from={leadFrames} durationInFrames={Math.max(1, Math.round(scene.duration * fps))}>
        <Subtitle text={scene.display} sceneDur={scene.duration} sentenceSpans={scene.sentences?.map((s) => ({ start: s.start - scene.start, end: s.end - scene.start }))} />
      </Sequence>
    </AbsoluteFill>
  );
};

/** Three slanted bars sweep across at the start of every scene. */
const Wipe: React.FC = () => {
  const frame = useCurrentFrame();
  if (frame > 30) return null;
  const cols = [C.accent, C.accent2, C.text];
  return (
    <AbsoluteFill style={{ overflow: "hidden", pointerEvents: "none" }}>
      {cols.map((c, i) => {
        const x = interpolate(frame, [i * 3, i * 3 + 20], [-1500, 2500], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
          easing: Easing.inOut(Easing.cubic),
        });
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              top: -200,
              bottom: -200,
              left: 0,
              width: 760 - i * 200,
              background: c,
              opacity: 0.9 - i * 0.25,
              transform: `translateX(${x}px) skewX(-18deg)`,
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};
