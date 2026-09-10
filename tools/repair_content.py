#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TRANSCRIPTS = ROOT / "app/src/main/assets/transcripts"
AUDIO = ROOT / "app/src/main/res/raw"

TITLE_CORRECTIONS = {
    1: "အားလုံးမေတ္တာထား",
    38: "Pussy Cat",
    42: "လိမ်တာ၊ ညာတာ မကောင်းပါ",
    46: "ပန်းနှင်းဆီ",
    57: "လိမ်တာ၊ ညာတာ မကောင်းပါ",
}

TEXT_REPLACEMENTS = {
    1: {"အားလုံးမေတ္တာဘာ": "အားလုံးမေတ္တာထား"},
    38: {"Pusy Cat": "Pussy Cat", "Pusy Dog": "Puppy Dog"},
    42: {"လိမ်လိမ္မာမာ": "လိမ္မာလိမ္မာ", "လိမ်လိမ်မာမာ": "လိမ္မာလိမ္မာ"},
    46: {"ပနှင်းဆီ": "ပန်းနှင်းဆီ"},
    57: {"လိမ်လိမ္မာမာ": "လိမ္မာလိမ္မာ", "လိမ်လိမ်မာမာ": "လိမ္မာလိမ္မာ"},
}

REBUILT = {
    58: {
        "version": 4,
        "source": "Audio-reviewed karaoke draft; repeated lyrics and timings reconstructed from the bundled recording.",
        "title": "သရက်သီး",
        "lines": [
            {"start": 7.0, "end": 10.0, "text": "အို သရက်သီး၊ အို သရက်သီး။"},
            {"start": 10.0, "end": 13.0, "text": "အို သရက်သီး၊ သိပ်ကောင်းတယ်။"},
            {"start": 13.0, "end": 16.0, "text": "အမှည့်လည်းစားနိုင်၊ အစိမ်းလည်းစားနိုင်။"},
            {"start": 16.0, "end": 19.0, "text": "အို သရက်သီး၊ ဘယ်သူစားမလဲ။"},
            {"start": 19.0, "end": 23.0, "text": "အို သရက်သီး၊ အို သရက်သီး။"},
            {"start": 23.0, "end": 25.0, "text": "အို သရက်သီး၊ သိပ်ကောင်းတယ်။"},
            {"start": 25.0, "end": 28.0, "text": "အမှည့်လည်းစားနိုင်၊ အစိမ်းလည်းစားနိုင်။"},
            {"start": 28.0, "end": 31.0, "text": "အို သရက်သီး၊ တို့စားမယ်။"},
            {"start": 36.0, "end": 39.0, "text": "အို သရက်သီး၊ အို သရက်သီး။"},
            {"start": 39.0, "end": 42.0, "text": "အို သရက်သီး၊ သိပ်ကောင်းတယ်။"},
            {"start": 42.0, "end": 45.0, "text": "အမှည့်လည်းစားနိုင်၊ အစိမ်းလည်းစားနိုင်။"},
            {"start": 45.0, "end": 49.0, "text": "အို သရက်သီး၊ ဘယ်သူစားမလဲ။"},
            {"start": 49.0, "end": 53.0, "text": "အို သရက်သီး၊ အို သရက်သီး။"},
            {"start": 53.0, "end": 54.0, "text": "အို သရက်သီး၊ သိပ်ကောင်းတယ်။"},
            {"start": 54.0, "end": 57.0, "text": "အမှည့်လည်းစားနိုင်၊ အစိမ်းလည်းစားနိုင်။"},
            {"start": 57.0, "end": 61.0, "text": "အို သရက်သီး၊ တို့စားမယ်။"},
        ],
    },
    59: {
        "version": 4,
        "source": "Audio-reviewed karaoke draft; lyrics checked against the matching bundled poem text.",
        "title": "ဘဲရုပ်ဆိုး",
        "lines": [
            {"start": 8.0, "end": 10.0, "text": "ဘဲကလေးရုပ်ဆိုး၊ ဘဲကလေးရုပ်ဆိုး။"},
            {"start": 10.0, "end": 13.0, "text": "ခြေထောက်တို၊ မျက်လုံးပြူး။"},
            {"start": 14.0, "end": 17.0, "text": "လည်ပင်းကြီးကတော့ရှည်၊ နှုတ်ခမ်းကြီးကပြားနေ။"},
            {"start": 17.0, "end": 20.0, "text": "သီချင်းမဆိုတတ်၊ ဂတ် ဂတ် ဂတ်။"},
            {"start": 21.0, "end": 23.0, "text": "ဘဲကလေးရုပ်ဆိုး၊ ဘဲကလေးရုပ်ဆိုး။"},
            {"start": 24.0, "end": 26.0, "text": "ခြေထောက်တို၊ မျက်လုံးပြူး။"},
            {"start": 27.0, "end": 30.0, "text": "လည်ပင်းကြီးကတော့ရှည်၊ နှုတ်ခမ်းကြီးကပြားနေ။"},
            {"start": 30.0, "end": 33.0, "text": "သီချင်းမဆိုတတ်၊ ဂတ် ဂတ် ဂတ်။"},
        ],
    },
    60: {
        "version": 4,
        "source": "Audio-reviewed karaoke draft; wording normalized against the matching bundled poem text.",
        "title": "ပန်းနှင်းဆီ",
        "lines": [
            {"start": 11.0, "end": 15.5, "text": "ပန်းနှင်းဆီ၊ ပန်းနှင်းဆီ၊ ပြုံးပြုံးလေး နေတတ်သည်။"},
            {"start": 15.5, "end": 20.0, "text": "ပန်းနှင်းဆီ ဘာတွေရေး၊ ကကြီး ခခွေး ရေး။"},
            {"start": 20.0, "end": 24.5, "text": "လက်ရေးကလေးက ဝိုင်းလို့ညီ၊ ပန်းကလေးလိုစီ။"},
            {"start": 24.5, "end": 29.0, "text": "ပန်းနှင်းဆီ ပါးအို့လေး၊ ပန်းကလေးလိုမွှေး။"},
            {"start": 38.0, "end": 43.0, "text": "ပန်းနှင်းဆီ၊ ပန်းနှင်းဆီ၊ ပြုံးပြုံးလေး နေတတ်သည်။"},
            {"start": 43.0, "end": 47.5, "text": "ပန်းနှင်းဆီ ဘာတွေရေး၊ ကကြီး ခခွေး ရေး။"},
            {"start": 47.5, "end": 52.0, "text": "လက်ရေးကလေးက ဝိုင်းလို့ညီ၊ ပန်းကလေးလိုစီ။"},
            {"start": 52.0, "end": 56.5, "text": "ပန်းနှင်းဆီ ပါးအို့လေး၊ ပန်းကလေးလိုမွှေး။"},
        ],
    },
    63: {
        "version": 4,
        "source": "Audio-reviewed karaoke draft; repeated lyrics and timings reconstructed from the bundled recording.",
        "title": "ကော်ဖီသောက်မယ်",
        "lines": [
            {"start": 8.0, "end": 11.0, "text": "ကော်ဖီသောက်မယ် မမရယ်။"},
            {"start": 11.0, "end": 14.0, "text": "ကော်ဖီသောက်မယ် မမရယ်။"},
            {"start": 14.0, "end": 17.0, "text": "ကော်ဖီသောက်မယ် မမရယ်။"},
            {"start": 17.0, "end": 21.0, "text": "အားလုံးအတွက် ဖျော်ပါ။"},
            {"start": 21.0, "end": 24.0, "text": "အိုးကိုချပါ စုစုရယ်။"},
            {"start": 24.0, "end": 27.0, "text": "အိုးကိုချပါ စုစုရယ်။"},
            {"start": 27.0, "end": 30.0, "text": "အိုးကိုချပါ စုစုရယ်။"},
            {"start": 30.0, "end": 34.0, "text": "အားလုံးအတူတူ သောက်မယ်ကွယ်။"},
            {"start": 34.0, "end": 37.0, "text": "ကော်ဖီသောက်မယ် မမရယ်။"},
            {"start": 37.0, "end": 40.0, "text": "ကော်ဖီသောက်မယ် မမရယ်။"},
            {"start": 40.0, "end": 43.0, "text": "ကော်ဖီသောက်မယ် မမရယ်။"},
            {"start": 43.0, "end": 47.0, "text": "အားလုံးအတွက် ဖျော်ပါ။"},
            {"start": 47.0, "end": 50.0, "text": "အိုးကိုချပါ စုစုရယ်။"},
            {"start": 50.0, "end": 53.0, "text": "အိုးကိုချပါ စုစုရယ်။"},
            {"start": 53.0, "end": 56.0, "text": "အိုးကိုချပါ စုစုရယ်။"},
            {"start": 56.0, "end": 60.0, "text": "အားလုံးအတူတူ သောက်မယ်ကွယ်။"},
        ],
    },
}


def audio_duration(number: int) -> float:
    path = AUDIO / f"poem_{number:02d}.mp3"
    result = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", str(path)],
        check=True,
        capture_output=True,
        text=True,
    )
    return float(result.stdout.strip())


def main() -> None:
    for number, data in REBUILT.items():
        path = TRANSCRIPTS / f"poem_{number:02d}.json"
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    for number in range(1, 64):
        path = TRANSCRIPTS / f"poem_{number:02d}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        duration = audio_duration(number)

        if number in TITLE_CORRECTIONS:
            data["title"] = TITLE_CORRECTIONS[number]

        replacements = TEXT_REPLACEMENTS.get(number, {})
        lines = data.get("lines", [])
        for line in lines:
            text = str(line.get("text", ""))
            for old, new in replacements.items():
                text = text.replace(old, new)
            line["text"] = text.strip()

            start = max(0.0, float(line.get("start", 0.0)))
            if start > duration * 10.0:
                start /= 1000.0
            line["start"] = round(start, 2)

        max_start = max((float(line["start"]) for line in lines), default=0.0)
        if max_start >= duration and max_start > 0.0:
            scale = (duration - 0.5) / max_start
            for line in lines:
                line["start"] = round(float(line["start"]) * scale, 2)

        for index, line in enumerate(lines):
            start = float(line["start"])
            end = float(line.get("end", start + 0.1))
            if end > duration * 10.0:
                end /= 1000.0
            next_start = float(lines[index + 1]["start"]) if index + 1 < len(lines) else duration
            if end <= start + 0.2 or end > next_start + 0.1 or end > duration:
                end = next_start if index + 1 < len(lines) else duration - 0.1
            end = min(duration - 0.05, max(start + 0.1, end))
            line["end"] = round(end, 2)

        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print("Repaired titles, karaoke text, and timing bounds for 63 poems.")


if __name__ == "__main__":
    main()
