export interface ThemeSpec {
  id: string;
  name: string;
  keyBg: string;
  keyBgPressed: string;
  keyText: string;
  boardBg: string;
  accent: string;
  isDark: boolean;
  backgroundImage?: string | null;
  backgroundBlur?: number;
  keyOpacity?: number;
}

export const BUILT_IN_THEMES: ThemeSpec[] = [
  {
    id: "cyber_dark", name: "Cyber Dark",
    keyBg: "#16161f", keyBgPressed: "#2a2a3d", keyText: "#f5d67f",
    boardBg: "#0b0b12", accent: "#f5c34d", isDark: true, keyOpacity: 1,
  },
  {
    id: "emerald_gold", name: "Emerald Gold",
    keyBg: "#0f2a22", keyBgPressed: "#1b4636", keyText: "#e9c46a",
    boardBg: "#08211a", accent: "#e9c46a", isDark: true, keyOpacity: 1,
  },
  {
    id: "neon_sunset", name: "Neon Sunset",
    keyBg: "#241436", keyBgPressed: "#3b1e57", keyText: "#ff6fa5",
    boardBg: "#160b26", accent: "#ff9e4f", isDark: true, keyOpacity: 1,
  },
  {
    id: "amoled_black", name: "AMOLED Pure Black",
    keyBg: "#000000", keyBgPressed: "#1c1c1c", keyText: "#ffffff",
    boardBg: "#000000", accent: "#3ddc84", isDark: true, keyOpacity: 1,
  },
  {
    id: "arctic_light", name: "Arctic Light",
    keyBg: "#ffffff", keyBgPressed: "#e2ecf5", keyText: "#1b2733",
    boardBg: "#eff4f8", accent: "#3b82f6", isDark: false, keyOpacity: 1,
  },
];

/** Converts a ThemeSpec to a FlorisBoard-compatible JSON theme object. */
export function toFlorisJson(theme: ThemeSpec) {
  return {
    schema: "https://schemas.florisboard.org/theme/v2/theme.json",
    name: theme.name,
    authors: ["LakaBoard v2.0"],
    isNightTheme: theme.isDark,
    properties: {
      keyBackground: theme.keyBg,
      keyBackgroundPressed: theme.keyBgPressed,
      keyForeground: theme.keyText,
      windowBackground: theme.boardBg,
      accentColor: theme.accent,
      backgroundImage: theme.backgroundImage ?? null,
      backgroundBlur: theme.backgroundBlur ?? 0,
      keyOpacity: theme.keyOpacity ?? 1,
    },
  };
}

/** Parses a FlorisBoard-compatible theme JSON (best-effort, defaults filled in). */
export function fromFlorisJson(json: any, fallback: ThemeSpec): ThemeSpec {
  const p = json?.properties ?? {};
  return {
    id: `imported_${Date.now()}`,
    name: json?.name ?? "Imported Theme",
    keyBg: p.keyBackground ?? fallback.keyBg,
    keyBgPressed: p.keyBackgroundPressed ?? fallback.keyBgPressed,
    keyText: p.keyForeground ?? fallback.keyText,
    boardBg: p.windowBackground ?? fallback.boardBg,
    accent: p.accentColor ?? fallback.accent,
    isDark: json?.isNightTheme ?? fallback.isDark,
    backgroundImage: p.backgroundImage ?? null,
    backgroundBlur: p.backgroundBlur ?? 0,
    keyOpacity: p.keyOpacity ?? 1,
  };
}
