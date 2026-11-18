import { Composition, staticFile } from "remotion";
import { Video, VideoProps, Timeline } from "./Video";

const FPS = 30;

export const Root = () => {
  return (
    <Composition
      id="Main"
      component={Video}
      width={1920}
      height={1080}
      fps={FPS}
      durationInFrames={FPS * 10}
      defaultProps={{ timeline: null, ref: null } as VideoProps}
      calculateMetadata={async () => {
        const res = await fetch(staticFile("timeline.json"));
        const timeline = (await res.json()) as Timeline;
        // optional: the timeline the scene animations were authored against (see Video.tsx TimeWarp)
        let ref: Timeline | null = null;
        try {
          const r = await fetch(staticFile("timeline_ref.json"));
          if (r.ok) ref = (await r.json()) as Timeline;
        } catch {}
        return {
          durationInFrames: Math.ceil(timeline.total * FPS),
          props: { timeline, ref },
        };
      }}
    />
  );
};
