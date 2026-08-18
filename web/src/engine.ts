/**
 * engine.ts
 * ---------------------------------------------------------------------
 * Singlish -> Sinhala Unicode phonetic transliteration + Wijesekara key
 * mapping, ported 1:1 in structure from the Android
 * SinglishTransliterationEngine.kt / WijesekaraEngine.kt so the web
 * simulator and the native IME behave identically.
 */

// ---- Consonants (longest key first) --------------------------------
const CONSONANTS: [string, string][] = [
  ["ndh", "ඳ"], ["nd", "ඬ"], ["mb", "ඹ"], ["ngg", "ඟ"], ["ng", "ඞ්ග"],
  ["kh", "ඛ"], ["gh", "ඝ"], ["chh", "ඡ"], ["ch", "ච"], ["jh", "ඣ"],
  ["th", "ත"], ["dh", "ද"], ["ph", "ඵ"], ["bh", "භ"],
  ["sh", "ශ"], ["Sh", "ෂ"], ["SH", "ෂ"], ["gn", "ඤ"], ["ny", "ඤ"],
  ["T", "ට"], ["D", "ඩ"], ["N", "ණ"], ["L", "ළ"], ["R", "ර"],
  ["k", "ක"], ["g", "ග"], ["c", "ච"], ["j", "ජ"],
  ["t", "ට"], ["d", "ඩ"], ["n", "න"], ["p", "ප"],
  ["b", "බ"], ["m", "ම"], ["y", "ය"], ["r", "ර"],
  ["l", "ල"], ["v", "ව"], ["w", "ව"], ["s", "ස"],
  ["h", "හ"], ["f", "ෆ"], ["z", "ස"], ["x", "ක්ස්"], ["q", "ක"],
].sort((a, b) => b[0].length - a[0].length);

// ---- Vowel signs (applied after a consonant) ------------------------
const VOWEL_SIGNS: [string, string][] = [
  ["aEE", "ෑ"], ["aee", "ෑ"],
  ["aa", "ා"], ["A", "ා"],
  ["aE", "ැ"], ["ae", "ැ"],
  ["ii", "ී"], ["I", "ී"], ["ee", "ී"],
  ["uu", "ූ"], ["U", "ූ"], ["oo", "ූ"],
  ["ai", "ෙයි"], ["au", "ෞ"],
  ["i", "ි"], ["u", "ු"],
  ["E", "ේ"], ["e", "ෙ"],
  ["O", "ෝ"], ["o", "ො"],
].sort((a, b) => b[0].length - a[0].length);

// ---- Independent vowels (word start / after another vowel) ----------
const INDEPENDENT_VOWELS: [string, string][] = [
  ["aEE", "ඈ"], ["aee", "ඈ"],
  ["aa", "ආ"], ["A", "ආ"],
  ["aE", "ඇ"], ["ae", "ඇ"],
  ["ii", "ඊ"], ["I", "ඊ"],
  ["uu", "ඌ"], ["U", "ඌ"],
  ["ai", "ඓ"], ["au", "ඖ"],
  ["i", "ඉ"], ["u", "උ"],
  ["E", "ඒ"], ["e", "එ"],
  ["O", "ඕ"], ["o", "ඔ"],
  ["a", "අ"],
].sort((a, b) => b[0].length - a[0].length);

const YANSAYA = "්‍ය";
const RAKARANSAYA = "්‍ර";
const HAL_KIRIMA = "්";
const ANUSVARA = "ං";

function matchFrom(list: [string, string][], word: string, pos: number): [string, string] | null {
  for (const [key, val] of list) {
    if (word.startsWith(key, pos)) return [key, val];
  }
  return null;
}

function isVowelStart(word: string, pos: number): boolean {
  if (pos >= word.length) return false;
  return "aeiouAEIOU".includes(word[pos]);
}

/** Curated high-frequency dictionary — overrides generic rules for common / irregular words. */
export const COMMON_WORDS: Record<string, string> = {
  ayubowan: "ආයුබෝවන්", suba: "සුබ", udasanak: "උදෑසනක්", udesanak: "උදෑසනක්",
  amma: "අම්මා", thaththa: "තාත්තා", thatha: "තාත්තා", lanka: "ලංකා",
  sinhala: "සිංහල", sthuthiyi: "ස්තූතියි", sthutiyi: "ස්තූතියි",
  kohomada: "කොහොමද", kohomadha: "කොහොමද", mama: "මම", oyaa: "ඔයා", oya: "ඔයා",
  api: "අපි", eyaa: "එයා", eya: "එයා", mokakda: "මොකක්ද", monawada: "මොනවද",
  hodai: "හොඳයි", hondai: "හොඳයි", narakai: "නරකයි", namaskara: "නමස්කාර",
  sri: "ශ්‍රී", lankawa: "ලංකාව", wadak: "වැඩක්", wadaa: "වැඩ",
  sellam: "සෙල්ලම්", iskole: "ඉස්කෝලේ", gedara: "ගෙදර", gedhara: "ගෙදර",
  kaema: "කෑම", bath: "බත්", watura: "වතුර", vathura: "වතුර",
  hitha: "හිත", hithanawa: "හිතනවා", yanawa: "යනවා", enawa: "එනවා",
  karanawa: "කරනවා", puluwan: "පුළුවන්", baha: "බෑ", ne: "නෑ", nedha: "නැද්ද",
  puthaa: "පුතා", duwa: "දුව", malli: "මල්ලි", akka: "අක්කා", aiya: "අයියා",
  seeya: "සීයා", aachchi: "ආච්චි", hitapan: "හිටපන්", enna: "එන්න",
  yanna: "යන්න", kiyanawa: "කියනවා", adha: "අද", heta: "හෙට", iiye: "ඊයේ",
  davasak: "දවසක්", aluth: "අලුත්", avurudu: "අවුරුදු", puthu: "පුතු",
  gaha: "ගහ", gasa: "ගස", kolamba: "කොළඹ", rate: "රටේ", rata: "රට",
};

export const QUICK_PHRASES: { label: string; phrase: string }[] = [
  { label: "Hello", phrase: "ආයුබෝවන්!" },
  { label: "Thanks", phrase: "ස්තූතියි!" },
  { label: "Good day", phrase: "සුබ දවසක්!" },
  { label: "Good morning", phrase: "සුබ උදෑසනක්!" },
  { label: "Good night", phrase: "සුබ රාත්‍රියක්!" },
  { label: "How are you?", phrase: "කොහොමද?" },
  { label: "I'm fine", phrase: "මම හොඳින් ඉන්නවා" },
  { label: "See you", phrase: "පසුව හම්බෙමු" },
  { label: "Congratulations", phrase: "සුභ පැතුම්!" },
  { label: "Happy birthday", phrase: "සුභ උපන්දිනයක්!" },
];

/** Core syllable parser used as a fallback for words not in the dictionary. */
export function transliterateSyllables(word: string): string {
  let out = "";
  let i = 0;
  while (i < word.length) {
    const cons = matchFrom(CONSONANTS, word, i);
    if (cons) {
      const [key, base] = cons;
      let pos = i + key.length;

      if (base.includes(HAL_KIRIMA)) {
        // digraph result already fully formed (e.g. "ng" -> ඞ්ග)
        out += base;
        i = pos;
        continue;
      }

      let glyph = base;
      if (pos < word.length && word[pos] === "y" && key !== "y") {
        glyph = base + YANSAYA;
        pos += 1;
      } else if (pos < word.length && word[pos] === "r" && key !== "r" && key !== "R") {
        glyph = base + RAKARANSAYA;
        pos += 1;
      }

      const vowel = matchFrom(VOWEL_SIGNS, word, pos);
      if (vowel) {
        out += glyph + vowel[1];
        pos += vowel[0].length;
      } else {
        const isLastChar = pos >= word.length;
        const nextIsConsonant = pos < word.length && !isVowelStart(word, pos);
        if (isLastChar || nextIsConsonant) {
          out += glyph + HAL_KIRIMA;
        } else {
          out += glyph;
        }
      }
      i = pos;
      continue;
    }

    const indep = matchFrom(INDEPENDENT_VOWELS, word, i);
    if (indep) {
      out += indep[1];
      i += indep[0].length;
      continue;
    }

    if (word[i] === "n" && word[i + 1] === "g" && i + 2 >= word.length) {
      out += ANUSVARA;
      i += 2;
      continue;
    }

    out += word[i];
    i += 1;
  }
  return out;
}

export function transliterateWord(word: string): string {
  if (!word) return word;
  const lower = word.toLowerCase();
  if (COMMON_WORDS[lower]) return COMMON_WORDS[lower];
  return transliterateSyllables(word);
}

/** Transliterate a full buffer, preserving whitespace/punctuation, word by word. */
export function transliterate(input: string): string {
  if (!input) return input;
  let out = "";
  let buf = "";
  for (const ch of input) {
    if (/[a-zA-Z]/.test(ch)) {
      buf += ch;
    } else {
      if (buf) { out += transliterateWord(buf); buf = ""; }
      out += ch;
    }
  }
  if (buf) out += transliterateWord(buf);
  return out;
}

/** Up to 3 live suggestion candidates for a partially-typed word. */
export function suggestionsFor(partial: string): string[] {
  if (!partial) return [];
  const lower = partial.toLowerCase();
  const results: string[] = [];
  if (COMMON_WORDS[lower]) results.push(COMMON_WORDS[lower]);
  for (const key of Object.keys(COMMON_WORDS)) {
    if (results.length >= 3) break;
    if (key.startsWith(lower) && key !== lower) results.push(COMMON_WORDS[key]);
  }
  const algo = transliterateSyllables(partial);
  if (!results.includes(algo)) results.push(algo);
  return results.slice(0, 3);
}

// ---- Wijesekara layout ------------------------------------------------
export const WIJESEKARA_LOWER: Record<string, string> = {
  "1": "1", "2": "2", "3": "3", "4": "4", "5": "5", "6": "6", "7": "7", "8": "8", "9": "9", "0": "0",
  q: "ං", w: "ඉ", e: "ට", r: "එ", t: "ර", y: "ත", u: "ය", i: "උ", o: "ි", p: "ප",
  a: "ෙ", s: "ස", d: "ද", f: "ෆ", g: "ග", h: "හ", j: "ජ", k: "ක", l: "ල",
  z: "ෘ", x: "ං", c: "ච", v: "ව", b: "බ", n: "න", m: "ම", ",": ",", ".": ".",
};

export const WIJESEKARA_UPPER: Record<string, string> = {
  q: "ඃ", w: "ඊ", e: "ඨ", r: "ඒ", t: "ණ", y: "ථ", u: "ූ", i: "ඌ", o: "ී", p: "ඵ",
  a: "ේ", s: "ෂ", d: "ධ", f: "ඩ", g: "ඝ", h: "ඃ", j: "ඣ", k: "ඛ", l: "ළ",
  z: "ෲ", x: "඼", c: "ඡ", v: "ඝ", b: "භ", n: "ඤ", m: "ං", ",": "<", ".": ">",
};

export const WIJESEKARA_MODIFIERS = {
  halKirima: HAL_KIRIMA,
  yansaya: YANSAYA,
  rakaransaya: RAKARANSAYA,
  rephaya: "ර්‍",
};

export function wijesekaraMap(char: string, shifted: boolean): string {
  const table = shifted ? WIJESEKARA_UPPER : WIJESEKARA_LOWER;
  return table[char.toLowerCase()] ?? char;
}
