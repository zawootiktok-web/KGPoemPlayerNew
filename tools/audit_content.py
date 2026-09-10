#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TRANSCRIPTS = ROOT / "app/src/main/assets/transcripts"
AUDIO = ROOT / "app/src/main/res/raw"

errors = []
rows = []
for number in range(1, 64):
    stem = f"poem_{number:02d}"
    json_path = TRANSCRIPTS / f"{stem}.json"
    txt_path = TRANSCRIPTS / f"{stem}.txt"
    audio_path = AUDIO / f"{stem}.mp3"

    for path in (json_path, txt_path, audio_path):
        if not path.is_file():
            errors.append(f"Missing: {path.relative_to(ROOT)}")

    if not json_path.is_file():
        continue

    try:
        data = json.loads(json_path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"Invalid JSON {json_path.name}: {exc}")
        continue

    title = str(data.get("title", "")).strip()
    lines = data.get("lines", [])
    if not title:
        errors.append(f"Empty title: {json_path.name}")
    if not isinstance(lines, list) or not lines:
        errors.append(f"No lyric lines: {json_path.name}")
        lines = []

    previous_start = -1.0
    for index, line in enumerate(lines, 1):
        try:
            start = float(line["start"])
            end = float(line["end"])
            text = str(line["text"]).strip()
        except Exception as exc:
            errors.append(f"Bad line {json_path.name}:{index}: {exc}")
            continue
        if start < previous_start:
            errors.append(f"Unsorted timing {json_path.name}:{index}")
        if end <= start:
            errors.append(f"Invalid timing {json_path.name}:{index} ({start}..{end})")
        if not text:
            errors.append(f"Empty lyric {json_path.name}:{index}")
        previous_start = start

    duration = None
    if audio_path.is_file():
        try:
            result = subprocess.run(
                ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", str(audio_path)],
                check=True,
                capture_output=True,
                text=True,
            )
            duration = float(result.stdout.strip())
        except (FileNotFoundError, subprocess.SubprocessError, ValueError):
            pass

    last_end = max((float(line.get("end", 0)) for line in lines), default=0.0)
    if duration is not None and last_end > duration + 1.0:
        errors.append(f"Timing exceeds audio {json_path.name}: {last_end:.1f}s > {duration:.1f}s")

    rows.append((number, title, len(lines), duration, last_end))

print("NUMBER\tTITLE\tLINES\tAUDIO_SECONDS\tLAST_LYRIC_END")
for number, title, count, duration, last_end in rows:
    duration_text = f"{duration:.2f}" if duration is not None else "unknown"
    print(f"{number:02d}\t{title}\t{count}\t{duration_text}\t{last_end:.2f}")

print(f"\nAudited {len(rows)} poems; issues={len(errors)}")
for error in errors:
    print(f"ERROR: {error}")

raise SystemExit(1 if errors else 0)
