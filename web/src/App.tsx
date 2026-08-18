import React, { useCallback, useMemo, useRef, useState } from "react";
import {
  Palette, HelpCircle, Copy, Share2, Delete, CornerDownLeft,
  Smile, ClipboardList, Pin, Trash2, X, Upload, Download,
  MessageCircle, NotebookPen, Search, Mail, Globe2,
} from "lucide-react";
import {
  transliterateWord, suggestionsFor, wijesekaraMap, WIJESEKARA_MODIFIERS, QUICK_PHRASES,
} from "./engine";
import { BUILT_IN_THEMES, ThemeSpec, toFlorisJson, fromFlorisJson } from "./themes";
import { playKeySound, triggerHaptic, SoundProfile } from "./sound";

type Mode = "singlish" | "wijesekara" | "english" | "symbols" | "emoji" | "clipboard";
type Shift = 0 | 1 | 2; // off / shift-once / caps-lock
type SimApp = "chat" | "notepad" | "search" | "email";

interface ClipboardEntry { id: string; text: string; pinned: boolean; ts: number; }

const SINGLISH_ROWS = ["qwertyuiop", "asdfghjkl", "zxcvbnm"].map((r) => r.split(""));
const EMOJI_SET = ["😀", "😂", "🥰", "😎", "🙏", "👍", "❤️", "🔥", "🎉", "😢", "😅", "🤔", "😴", "🙌", "👏", "💯", "✨", "🌸", "🍚", "☕"];
const SYMBOL_ROWS = ["1234567890", "@#₹&*-+()/", "!\"':;,.?~"].map((r) => r.split(""));
const DIACRITICS = ["ං", "ඃ", "්", "ා", "ැ", "ෑ", "ි", "ී", "ු", "ූ", "ෘ", "ෙ", "ේ", "ො", "ෝ", "ෞ"];

const GUIDE_ROWS: { rule: string; type: string; example: string }[] = [
  { rule: "k, g, ch, j, t, d, n, p, b, m, y, r, l, v, s, h", type: "Base consonant (inherent 'a')", example: "ka → ක" },
  { rule: "consonant only (word end / before another consonant)", type: "Hal kirima", example: "k → ක්" },
  { rule: "+ aa / A", type: "Alapilla (long ‘aa’)", example: "kaa → කා" },
  { rule: "+ i / ii / I", type: "Kombuva family", example: "ki → කි, kii → කී" },
  { rule: "+ u / uu / U / oo", type: "Papilla family", example: "ku → කු, koo → කූ" },
  { rule: "+ e / E", type: "Kombuva", example: "ke → කෙ, kE → කේ" },
  { rule: "+ o / O", type: "Kombu + Paapilla", example: "ko → කො, kO → කෝ" },
  { rule: "+ y + vowel", type: "Yansaya", example: "kya → ක්‍ය" },
  { rule: "+ r + vowel", type: "Rakaransaya", example: "kra → ක්‍ර" },
  { rule: "nd, mb, ng", type: "Bandi akuru (prenasalized)", example: "kanda-style words" },
  { rule: "T, D, N, L (capital)", type: "Retroflex letters", example: "Tikak → ටිකක්" },
  { rule: "sh / Sh", type: "Sibilants", example: "sha → ශ, Sha → ෂ" },
];

function uid() { return Math.random().toString(36).slice(2, 10); }

export default function App() {
  const [mode, setMode] = useState<Mode>("singlish");
  const [shift, setShift] = useState<Shift>(0);
  const [wordBuffer, setWordBuffer] = useState("");
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [theme, setTheme] = useState<ThemeSpec>(BUILT_IN_THEMES[0]);
  const [soundProfile, setSoundProfile] = useState<SoundProfile>("mechanical");
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [hapticEnabled, setHapticEnabled] = useState(true);
  const [clipboard, setClipboard] = useState<ClipboardEntry[]>([]);
  const [showTheme, setShowTheme] = useState(false);
  const [showGuide, setShowGuide] = useState(false);
  const [activeApp, setActiveApp] = useState<SimApp>("chat");
  const [fields, setFields] = useState<Record<SimApp, string>>({ chat: "", notepad: "", search: "", email: "" });
  const [toast, setToast] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const text = fields[activeApp];
  const setText = useCallback((updater: (prev: string) => string) => {
    setFields((f) => ({ ...f, [activeApp]: updater(f[activeApp]) }));
  }, [activeApp]);

  const charCount = text.length;
  const wordCount = useMemo(() => text.trim() ? text.trim().split(/\s+/).length : 0, [text]);

  const flashToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(null), 1600); };

  const press = useCallback(() => {
    if (soundEnabled) playKeySound(soundProfile, 0.5);
    triggerHaptic(hapticEnabled);
  }, [soundEnabled, soundProfile, hapticEnabled]);

  const consumeShift = () => setShift((s) => (s === 1 ? 0 : s));

  const addClipboard = (value: string) => {
    if (!value.trim()) return;
    setClipboard((prev) => {
      const dedup = prev.filter((c) => c.text !== value);
      return [{ id: uid(), text: value, pinned: false, ts: Date.now() }, ...dedup].slice(0, 50);
    });
  };

  // ---- Singlish live re-render of the current word -----------------
  const applyWordBuffer = (buf: string) => {
    const rendered = buf ? transliterateWord(buf) : "";
    setText((prev) => {
      const lastSpace = Math.max(prev.lastIndexOf(" "), prev.lastIndexOf("\n"));
      const base = lastSpace >= 0 ? prev.slice(0, lastSpace + 1) : "";
      return base + rendered;
    });
    setSuggestions(buf ? suggestionsFor(buf) : []);
  };

  const handleSinglishKey = (key: string) => {
    press();
    const nextBuf = wordBuffer + key;
    setWordBuffer(nextBuf);
    applyWordBuffer(nextBuf);
    consumeShift();
  };

  const handleEnglishKey = (key: string) => {
    press();
    const ch = shift !== 0 ? key.toUpperCase() : key;
    setText((prev) => prev + ch);
    consumeShift();
  };

  const handleGlyph = (glyph: string) => {
    press();
    setWordBuffer("");
    setSuggestions([]);
    setText((prev) => prev + glyph);
  };

  const handleSpace = () => {
    press();
    setWordBuffer("");
    setSuggestions([]);
    setText((prev) => prev + " ");
  };

  const handleEnter = () => {
    press();
    setWordBuffer("");
    setSuggestions([]);
    setText((prev) => prev + "\n");
  };

  const handleBackspace = () => {
    press();
    if (wordBuffer) {
      const nextBuf = wordBuffer.slice(0, -1);
      setWordBuffer(nextBuf);
      applyWordBuffer(nextBuf);
    } else {
      setText((prev) => prev.slice(0, -1));
    }
  };

  const selectSuggestion = (candidate: string) => {
    press();
    setText((prev) => {
      const lastSpace = Math.max(prev.lastIndexOf(" "), prev.lastIndexOf("\n"));
      const base = lastSpace >= 0 ? prev.slice(0, lastSpace + 1) : "";
      return base + candidate + " ";
    });
    addClipboard(candidate);
    setWordBuffer("");
    setSuggestions([]);
  };

  const switchMode = (m: Mode) => {
    setWordBuffer("");
    setSuggestions([]);
    setMode(m);
  };

  const quickCopy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      flashToast("Copied to clipboard");
    } catch {
      flashToast("Copy not supported here");
    }
  };

  const shareIntent = async () => {
    if (navigator.share) {
      try { await navigator.share({ text }); } catch { /* user cancelled */ }
    } else {
      flashToast("Share not supported in this browser");
    }
  };

  const exportTheme = () => {
    const blob = new Blob([JSON.stringify(toFlorisJson(theme), null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${theme.id}.florisboard-theme.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const importTheme = (file: File) => {
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const json = JSON.parse(String(reader.result));
        setTheme(fromFlorisJson(json, theme));
        flashToast("Theme imported");
      } catch {
        flashToast("Invalid theme JSON");
      }
    };
    reader.readAsText(file);
  };

  return (
    <div
      className="min-h-screen w-full flex items-center justify-center p-6"
      style={{ background: "radial-gradient(circle at 30% 20%, #1a1a2e, #0a0a12 70%)" }}
    >
      <div className="w-full max-w-sm">
        <header className="text-center mb-4">
          <h1 className="text-2xl font-bold tracking-tight" style={{ color: theme.accent }}>LakaBoard v2.0</h1>
          <p className="text-xs text-slate-400 mt-1">Sinhala &amp; English Phonetic Keyboard — Web Simulator</p>
        </header>

        <PhoneFrame
          theme={theme}
          activeApp={activeApp}
          setActiveApp={setActiveApp}
          text={text}
          charCount={charCount}
          wordCount={wordCount}
          onCopy={quickCopy}
          onShare={shareIntent}
          onOpenTheme={() => setShowTheme(true)}
          onOpenGuide={() => setShowGuide(true)}
        >
          <Keyboard
            mode={mode}
            shift={shift}
            theme={theme}
            suggestions={suggestions}
            onSuggestion={selectSuggestion}
            onSinglishKey={handleSinglishKey}
            onEnglishKey={handleEnglishKey}
            onGlyph={handleGlyph}
            onSpace={handleSpace}
            onEnter={handleEnter}
            onBackspace={handleBackspace}
            onShiftCycle={() => setShift((s) => ((s + 1) % 3) as Shift)}
            onModeChange={switchMode}
            clipboard={clipboard}
            onPin={(id) => setClipboard((cs) => [...cs].map((c) => c.id === id ? { ...c, pinned: !c.pinned } : c).sort((a, b) => Number(b.pinned) - Number(a.pinned)))}
            onDeleteClip={(id) => setClipboard((cs) => cs.filter((c) => c.id !== id))}
            onPasteClip={(txt) => { press(); setText((p) => p + txt); }}
          />
        </PhoneFrame>

        <SettingsBar
          theme={theme}
          soundProfile={soundProfile}
          setSoundProfile={setSoundProfile}
          soundEnabled={soundEnabled}
          setSoundEnabled={setSoundEnabled}
          hapticEnabled={hapticEnabled}
          setHapticEnabled={setHapticEnabled}
        />
      </div>

      {showTheme && (
        <ThemeModal
          theme={theme}
          onSelect={setTheme}
          onClose={() => setShowTheme(false)}
          onExport={exportTheme}
          onImportClick={() => fileInputRef.current?.click()}
        />
      )}
      <input
        ref={fileInputRef}
        type="file"
        accept="application/json"
        className="hidden"
        onChange={(e) => { const f = e.target.files?.[0]; if (f) importTheme(f); e.currentTarget.value = ""; }}
      />

      {showGuide && <GuideModal onClose={() => setShowGuide(false)} />}

      {toast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 bg-black/80 text-white text-sm px-4 py-2 rounded-full shadow-lg">
          {toast}
        </div>
      )}
    </div>
  );
}

// ======================================================================
// Phone frame with top bar + simulated apps
// ======================================================================
function PhoneFrame(props: {
  theme: ThemeSpec;
  activeApp: SimApp;
  setActiveApp: (a: SimApp) => void;
  text: string;
  charCount: number;
  wordCount: number;
  onCopy: () => void;
  onShare: () => void;
  onOpenTheme: () => void;
  onOpenGuide: () => void;
  children: React.ReactNode;
}) {
  const { theme, activeApp, setActiveApp, text, charCount, wordCount, onCopy, onShare, onOpenTheme, onOpenGuide, children } = props;
  const apps: { id: SimApp; label: string; icon: React.ReactNode }[] = [
    { id: "chat", label: "Chat", icon: <MessageCircle size={14} /> },
    { id: "notepad", label: "Notepad", icon: <NotebookPen size={14} /> },
    { id: "search", label: "Search", icon: <Search size={14} /> },
    { id: "email", label: "Email", icon: <Mail size={14} /> },
  ];

  return (
    <div className="rounded-[2.5rem] border-4 border-black/60 bg-[#111318] shadow-2xl p-3">
      {/* Top bar */}
      <div className="flex items-center justify-between px-1 pb-2">
        <span className="text-[10px] text-slate-400">{charCount} chars · {wordCount} words</span>
        <div className="flex items-center gap-1">
          <button onClick={onCopy} className="p-1.5 rounded-full hover:bg-white/10" title="Copy">
            <Copy size={14} color="#cbd5e1" />
          </button>
          <button onClick={onShare} className="p-1.5 rounded-full hover:bg-white/10" title="Share">
            <Share2 size={14} color="#cbd5e1" />
          </button>
          <button onClick={onOpenGuide} className="p-1.5 rounded-full hover:bg-white/10" title="Transliteration Guide">
            <HelpCircle size={14} color="#cbd5e1" />
          </button>
          <button onClick={onOpenTheme} className="p-1.5 rounded-full hover:bg-white/10" title="Theme">
            <Palette size={14} color={theme.accent} />
          </button>
        </div>
      </div>

      {/* App tabs */}
      <div className="flex gap-1.5 px-1 pb-2">
        {apps.map((a) => (
          <button
            key={a.id}
            onClick={() => setActiveApp(a.id)}
            className={`flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-full border transition-colors ${
              activeApp === a.id ? "border-transparent" : "border-white/10 text-slate-400"
            }`}
            style={activeApp === a.id ? { background: theme.accent, color: "#111318" } : {}}
          >
            {a.icon}{a.label}
          </button>
        ))}
      </div>

      {/* App content */}
      <div className="rounded-2xl bg-white min-h-[110px] max-h-[130px] overflow-y-auto p-3 mb-2 text-sm text-slate-800">
        <AppPreview app={activeApp} text={text} />
      </div>

      {children}
    </div>
  );
}

function AppPreview({ app, text }: { app: SimApp; text: string }) {
  if (app === "chat") {
    return (
      <div className="flex justify-end">
        <div className="bg-[#dcf8c6] rounded-2xl rounded-tr-sm px-3 py-2 max-w-[85%] whitespace-pre-wrap break-words">
          {text || <span className="text-slate-400">Type a message…</span>}
        </div>
      </div>
    );
  }
  if (app === "notepad") {
    return <div className="whitespace-pre-wrap break-words">{text || <span className="text-slate-400">Start writing your Sinhala note…</span>}</div>;
  }
  if (app === "search") {
    return (
      <div className="flex items-center gap-2 border rounded-full px-3 py-2">
        <Search size={14} className="text-slate-400" />
        <span className="break-words">{text || <span className="text-slate-400">Search Sinhala or English…</span>}</span>
      </div>
    );
  }
  return (
    <div>
      <div className="text-[11px] text-slate-400">To: someone@example.com</div>
      <div className="text-[11px] text-slate-400 mb-1">Subject: (no subject)</div>
      <div className="whitespace-pre-wrap break-words">{text || <span className="text-slate-400">Write your email…</span>}</div>
    </div>
  );
}

// ======================================================================
// Keyboard
// ======================================================================
function Keyboard(props: {
  mode: Mode; shift: Shift; theme: ThemeSpec;
  suggestions: string[]; onSuggestion: (s: string) => void;
  onSinglishKey: (k: string) => void; onEnglishKey: (k: string) => void;
  onGlyph: (g: string) => void; onSpace: () => void; onEnter: () => void; onBackspace: () => void;
  onShiftCycle: () => void; onModeChange: (m: Mode) => void;
  clipboard: ClipboardEntry[]; onPin: (id: string) => void; onDeleteClip: (id: string) => void; onPasteClip: (t: string) => void;
}) {
  const { mode, shift, theme, suggestions, onSuggestion } = props;

  return (
    <div
      className="rounded-2xl p-2 mt-1"
      style={{ background: theme.boardBg }}
    >
      {mode === "singlish" && suggestions.length > 0 && (
        <div className="flex gap-2 px-1 pb-2 overflow-x-auto">
          {suggestions.map((s, idx) => (
            <button
              key={idx}
              onClick={() => onSuggestion(s)}
              className="px-3 py-1.5 rounded-lg text-sm whitespace-nowrap"
              style={{ background: theme.keyBg, color: theme.keyText }}
            >
              {s}
            </button>
          ))}
        </div>
      )}

      {mode === "singlish" && (
        <RowsLayout theme={theme} rows={SINGLISH_ROWS} render={(k) => k} onKey={props.onSinglishKey} />
      )}
      {mode === "english" && (
        <>
          <RowsLayout
            theme={theme}
            rows={SINGLISH_ROWS}
            render={(k) => (shift !== 0 ? k.toUpperCase() : k)}
            onKey={props.onEnglishKey}
            shiftKey={{ active: shift !== 0, capsLock: shift === 2, onClick: props.onShiftCycle }}
          />
        </>
      )}
      {mode === "wijesekara" && (
        <WijesekaraLayout theme={theme} shifted={shift !== 0} capsLock={shift === 2} onShift={props.onShiftCycle} onGlyph={props.onGlyph} />
      )}
      {mode === "symbols" && <SymbolsLayout theme={theme} onKey={props.onEnglishKey} />}
      {mode === "emoji" && <EmojiPhrasesLayout theme={theme} onGlyph={props.onGlyph} />}
      {mode === "clipboard" && (
        <ClipboardLayout theme={theme} items={props.clipboard} onPin={props.onPin} onDelete={props.onDeleteClip} onPaste={props.onPasteClip} />
      )}

      <BottomRow
        theme={theme} mode={mode}
        onSpace={props.onSpace} onEnter={props.onEnter} onBackspace={props.onBackspace}
        onModeChange={props.onModeChange}
      />
    </div>
  );
}

function KeyCap({ label, theme, flex = 1, small = false, onClick, active = false }: {
  label: React.ReactNode; theme: ThemeSpec; flex?: number; small?: boolean; onClick: () => void; active?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        flex,
        background: active ? theme.keyBgPressed : theme.keyBg,
        color: theme.keyText,
        opacity: theme.keyOpacity ?? 1,
      }}
      className={`m-[2px] rounded-lg h-10 flex items-center justify-center select-none active:scale-95 transition-transform ${small ? "text-xs" : "text-sm"}`}
    >
      {label}
    </button>
  );
}

function RowsLayout({ theme, rows, render, onKey, shiftKey }: {
  theme: ThemeSpec; rows: string[][]; render: (k: string) => string; onKey: (k: string) => void;
  shiftKey?: { active: boolean; capsLock: boolean; onClick: () => void };
}) {
  return (
    <div>
      {rows.map((row, idx) => (
        <div key={idx} className="flex">
          {idx === 2 && shiftKey && (
            <KeyCap label={shiftKey.capsLock ? "⇪" : "⇧"} theme={theme} flex={1.5} onClick={shiftKey.onClick} active={shiftKey.active} />
          )}
          {row.map((k) => (
            <KeyCap key={k} label={render(k)} theme={theme} onClick={() => onKey(k)} />
          ))}
        </div>
      ))}
    </div>
  );
}

function WijesekaraLayout({ theme, shifted, capsLock, onShift, onGlyph }: {
  theme: ThemeSpec; shifted: boolean; capsLock: boolean; onShift: () => void; onGlyph: (g: string) => void;
}) {
  return (
    <div>
      {SINGLISH_ROWS.map((row, idx) => (
        <div key={idx} className="flex">
          {idx === 2 && <KeyCap label={capsLock ? "⇪" : "⇧"} theme={theme} flex={1.5} onClick={onShift} active={shifted} />}
          {row.map((k) => {
            const glyph = wijesekaraMap(k, shifted);
            return <KeyCap key={k} label={glyph} theme={theme} onClick={() => onGlyph(glyph)} />;
          })}
        </div>
      ))}
      <div className="flex">
        <KeyCap label="්" theme={theme} onClick={() => onGlyph(WIJESEKARA_MODIFIERS.halKirima)} />
        <KeyCap label="්‍ය" theme={theme} onClick={() => onGlyph(WIJESEKARA_MODIFIERS.yansaya)} />
        <KeyCap label="්‍ර" theme={theme} onClick={() => onGlyph(WIJESEKARA_MODIFIERS.rakaransaya)} />
        <KeyCap label="ර්‍" theme={theme} onClick={() => onGlyph(WIJESEKARA_MODIFIERS.rephaya)} />
      </div>
    </div>
  );
}

function SymbolsLayout({ theme, onKey }: { theme: ThemeSpec; onKey: (k: string) => void }) {
  return (
    <div>
      {SYMBOL_ROWS.map((row, idx) => (
        <div key={idx} className="flex">
          {row.map((k) => <KeyCap key={k} label={k} theme={theme} onClick={() => onKey(k)} />)}
        </div>
      ))}
      <div className="text-[10px] px-1 pt-1 pb-0.5" style={{ color: theme.keyText, opacity: 0.7 }}>Sinhala Diacritics</div>
      <div className="grid grid-cols-8 gap-1">
        {DIACRITICS.map((d) => <KeyCap key={d} label={d} theme={theme} small onClick={() => onKey(d)} />)}
      </div>
    </div>
  );
}

function EmojiPhrasesLayout({ theme, onGlyph }: { theme: ThemeSpec; onGlyph: (g: string) => void }) {
  return (
    <div>
      <div className="text-[10px] px-1 pb-1" style={{ color: theme.keyText, opacity: 0.7 }}>Quick Sinhala Phrases</div>
      <div className="grid grid-cols-2 gap-1.5 mb-2">
        {QUICK_PHRASES.map((p) => (
          <button
            key={p.label}
            onClick={() => onGlyph(p.phrase)}
            className="rounded-lg p-2 text-left"
            style={{ background: theme.keyBg }}
          >
            <div style={{ color: theme.keyText }} className="text-sm">{p.phrase}</div>
            <div style={{ color: theme.keyText, opacity: 0.6 }} className="text-[10px]">{p.label}</div>
          </button>
        ))}
      </div>
      <div className="text-[10px] px-1 pb-1" style={{ color: theme.keyText, opacity: 0.7 }}>Emoji</div>
      <div className="grid grid-cols-8 gap-1 max-h-24 overflow-y-auto">
        {EMOJI_SET.map((e) => <KeyCap key={e} label={e} theme={theme} small onClick={() => onGlyph(e)} />)}
      </div>
    </div>
  );
}

function ClipboardLayout({ theme, items, onPin, onDelete, onPaste }: {
  theme: ThemeSpec; items: ClipboardEntry[]; onPin: (id: string) => void; onDelete: (id: string) => void; onPaste: (t: string) => void;
}) {
  return (
    <div className="max-h-40 overflow-y-auto">
      {items.length === 0 && (
        <div className="flex items-center justify-center h-24 text-xs" style={{ color: theme.keyText, opacity: 0.6 }}>
          Copied text will appear here
        </div>
      )}
      {items.map((item) => (
        <div key={item.id} className="flex items-center justify-between rounded-lg mb-1 px-2 py-1.5" style={{ background: theme.keyBg }}>
          <button onClick={() => onPaste(item.text)} className="text-sm text-left flex-1 truncate mr-2" style={{ color: theme.keyText }}>
            {item.text}
          </button>
          <div className="flex items-center gap-1">
            <button onClick={() => onPin(item.id)}><Pin size={13} color={item.pinned ? theme.accent : theme.keyText} /></button>
            <button onClick={() => onDelete(item.id)}><Trash2 size={13} color={theme.keyText} /></button>
          </div>
        </div>
      ))}
    </div>
  );
}

function BottomRow({ theme, mode, onSpace, onEnter, onBackspace, onModeChange }: {
  theme: ThemeSpec; mode: Mode; onSpace: () => void; onEnter: () => void; onBackspace: () => void; onModeChange: (m: Mode) => void;
}) {
  return (
    <div className="flex items-center mt-1">
      <KeyCap
        label={mode === "singlish" ? "En" : "සිං"}
        theme={theme} flex={1.2}
        onClick={() => onModeChange(mode === "singlish" ? "english" : "singlish")}
      />
      <KeyCap label="Wij" theme={theme} onClick={() => onModeChange("wijesekara")} />
      <KeyCap label="?123" theme={theme} onClick={() => onModeChange("symbols")} />
      <button onClick={onSpace} className="flex-[3] m-[2px] h-10 rounded-lg text-xs" style={{ background: theme.keyBg, color: theme.keyText }}>
        Space
      </button>
      <button onClick={() => onModeChange("emoji")} className="m-[2px] h-10 w-10 rounded-lg flex items-center justify-center" style={{ background: theme.keyBg }}>
        <Smile size={16} color={theme.keyText} />
      </button>
      <button onClick={() => onModeChange("clipboard")} className="m-[2px] h-10 w-10 rounded-lg flex items-center justify-center" style={{ background: theme.keyBg }}>
        <ClipboardList size={16} color={theme.keyText} />
      </button>
      <button onClick={onBackspace} className="m-[2px] h-10 w-10 rounded-lg flex items-center justify-center" style={{ background: theme.keyBg }}>
        <Delete size={16} color={theme.keyText} />
      </button>
      <button onClick={onEnter} className="m-[2px] h-10 w-10 rounded-lg flex items-center justify-center" style={{ background: theme.accent }}>
        <CornerDownLeft size={16} color="#111318" />
      </button>
    </div>
  );
}

// ======================================================================
// Settings bar (sound / haptic)
// ======================================================================
function SettingsBar({ theme, soundProfile, setSoundProfile, soundEnabled, setSoundEnabled, hapticEnabled, setHapticEnabled }: {
  theme: ThemeSpec; soundProfile: SoundProfile; setSoundProfile: (p: SoundProfile) => void;
  soundEnabled: boolean; setSoundEnabled: (b: boolean) => void; hapticEnabled: boolean; setHapticEnabled: (b: boolean) => void;
}) {
  const profiles: { id: SoundProfile; label: string }[] = [
    { id: "mechanical", label: "Mechanical" }, { id: "bubble", label: "Bubble" },
    { id: "pop", label: "Pop" }, { id: "typewriter", label: "Typewriter" },
  ];
  return (
    <div className="mt-4 rounded-2xl bg-white/5 border border-white/10 p-3">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs text-slate-300">Sound</span>
        <ToggleSwitch checked={soundEnabled} onChange={setSoundEnabled} accent={theme.accent} />
      </div>
      <div className="flex gap-1.5 mb-3 flex-wrap">
        {profiles.map((p) => (
          <button
            key={p.id}
            onClick={() => setSoundProfile(p.id)}
            className="text-[11px] px-2.5 py-1 rounded-full border"
            style={soundProfile === p.id
              ? { background: theme.accent, color: "#111318", borderColor: "transparent" }
              : { borderColor: "rgba(255,255,255,0.15)", color: "#cbd5e1" }}
          >
            {p.label}
          </button>
        ))}
      </div>
      <div className="flex items-center justify-between">
        <span className="text-xs text-slate-300">Haptic feedback</span>
        <ToggleSwitch checked={hapticEnabled} onChange={setHapticEnabled} accent={theme.accent} />
      </div>
    </div>
  );
}

function ToggleSwitch({ checked, onChange, accent }: { checked: boolean; onChange: (b: boolean) => void; accent: string }) {
  return (
    <button
      onClick={() => onChange(!checked)}
      className="w-9 h-5 rounded-full relative transition-colors"
      style={{ background: checked ? accent : "rgba(255,255,255,0.2)" }}
    >
      <span
        className="absolute top-0.5 w-4 h-4 rounded-full bg-white transition-transform"
        style={{ transform: `translateX(${checked ? 18 : 2}px)` }}
      />
    </button>
  );
}

// ======================================================================
// Modals
// ======================================================================
function ModalShell({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50">
      <div className="bg-[#15151f] border border-white/10 rounded-2xl max-w-sm w-full max-h-[80vh] overflow-y-auto p-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-white font-semibold text-sm">{title}</h2>
          <button onClick={onClose}><X size={18} color="#94a3b8" /></button>
        </div>
        {children}
      </div>
    </div>
  );
}

function ThemeModal({ theme, onSelect, onClose, onExport, onImportClick }: {
  theme: ThemeSpec; onSelect: (t: ThemeSpec) => void; onClose: () => void; onExport: () => void; onImportClick: () => void;
}) {
  return (
    <ModalShell title="Customize Theme" onClose={onClose}>
      <div className="space-y-2">
        {BUILT_IN_THEMES.map((t) => (
          <button
            key={t.id}
            onClick={() => onSelect(t)}
            className="w-full flex items-center justify-between px-3 py-2 rounded-xl border"
            style={{ borderColor: t.id === theme.id ? t.accent : "rgba(255,255,255,0.08)", background: t.id === theme.id ? `${t.accent}22` : "transparent" }}
          >
            <div className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full inline-block" style={{ background: t.boardBg, border: "1px solid rgba(255,255,255,0.15)" }} />
              <div className="text-left">
                <div className="text-white text-sm">{t.name}</div>
                <div className="text-[10px] text-slate-400">{t.isDark ? "Dark" : "Light"}</div>
              </div>
            </div>
            <span className="w-4 h-4 rounded-full inline-block" style={{ background: t.accent }} />
          </button>
        ))}
      </div>
      <div className="flex gap-2 mt-4">
        <button onClick={onImportClick} className="flex-1 flex items-center justify-center gap-1.5 text-xs py-2 rounded-lg border border-white/15 text-slate-200">
          <Upload size={13} /> Import JSON
        </button>
        <button onClick={onExport} className="flex-1 flex items-center justify-center gap-1.5 text-xs py-2 rounded-lg border border-white/15 text-slate-200">
          <Download size={13} /> Export JSON
        </button>
      </div>
      <p className="text-[10px] text-slate-500 mt-3">
        Themes export/import in a FlorisBoard-compatible JSON shape, so custom presets can be shared across keyboards.
      </p>
    </ModalShell>
  );
}

function GuideModal({ onClose }: { onClose: () => void }) {
  return (
    <ModalShell title="Singlish Transliteration Guide" onClose={onClose}>
      <p className="text-xs text-slate-400 mb-3 flex items-center gap-1">
        <Globe2 size={13} /> Type Sinhala phonetically with English letters — LakaBoard converts it live.
      </p>
      <div className="space-y-3">
        {GUIDE_ROWS.map((row, idx) => (
          <div key={idx} className="border-b border-white/5 pb-2">
            <div className="text-slate-200 text-xs font-medium">{row.type}</div>
            <div className="text-slate-500 text-[11px]">Keys: {row.rule}</div>
            <div className="text-slate-300 text-[11px]">Example: {row.example}</div>
          </div>
        ))}
      </div>
    </ModalShell>
  );
}
