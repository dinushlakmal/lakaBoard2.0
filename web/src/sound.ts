/**
 * sound.ts
 * ---------------------------------------------------------------------
 * In-memory key-click synthesis via the Web Audio API (no audio assets),
 * mirroring the envelope recipes used by the native SoundHapticHelper.
 */

export type SoundProfile = "none" | "mechanical" | "bubble" | "pop" | "typewriter";

let audioCtx: AudioContext | null = null;

function getCtx(): AudioContext | null {
  if (typeof window === "undefined") return null;
  if (!audioCtx) {
    const Ctx = window.AudioContext || (window as any).webkitAudioContext;
    if (!Ctx) return null;
    audioCtx = new Ctx();
  }
  if (audioCtx.state === "suspended") audioCtx.resume().catch(() => {});
  return audioCtx;
}

/** Synthesizes and plays a short click for the given profile. Safe to call rapidly. */
export function playKeySound(profile: SoundProfile, volume = 0.5) {
  if (profile === "none") return;
  const ctx = getCtx();
  if (!ctx) return;

  const duration = { mechanical: 0.035, bubble: 0.09, pop: 0.06, typewriter: 0.05 }[profile];
  const sampleRate = ctx.sampleRate;
  const length = Math.max(1, Math.floor(sampleRate * duration));
  const buffer = ctx.createBuffer(1, length, sampleRate);
  const data = buffer.getChannelData(0);

  for (let i = 0; i < length; i++) {
    const t = i / sampleRate;
    const progress = i / length;
    let sample = 0;
    switch (profile) {
      case "mechanical": {
        const freq = 1800;
        const env = Math.exp(-progress * 18);
        sample = Math.sin(2 * Math.PI * freq * t) * env;
        break;
      }
      case "bubble": {
        const freq = 420 + 260 * progress;
        const env = Math.sin(Math.PI * progress);
        sample = Math.sin(2 * Math.PI * freq * t) * env;
        break;
      }
      case "pop": {
        const freq = 900 * Math.exp(-progress * 3);
        const env = Math.exp(-progress * 9);
        sample = Math.sin(2 * Math.PI * freq * t) * env;
        break;
      }
      case "typewriter": {
        const freq = 2200;
        const env = progress < 0.15 ? progress / 0.15 : Math.exp(-(progress - 0.15) * 14);
        const noise = (Math.random() * 2 - 1) * 0.15;
        sample = (Math.sin(2 * Math.PI * freq * t) + noise) * env;
        break;
      }
    }
    data[i] = sample * volume;
  }

  const src = ctx.createBufferSource();
  src.buffer = buffer;
  src.connect(ctx.destination);
  src.start();
}

/** Fires a short vibration pulse where supported (mobile Chrome, etc.). */
export function triggerHaptic(enabled: boolean) {
  if (!enabled) return;
  if (typeof navigator !== "undefined" && "vibrate" in navigator) {
    navigator.vibrate(12);
  }
}
