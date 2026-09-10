#!/usr/bin/env python3
"""Normalize karaoke line granularity and keep every line inside its audio window.

The original JSON was built from OCR/PDF paragraphs, so several recordings had one
very long karaoke row. This tool keeps the supplied wording, splits only at clear
clause boundaries (or safe word boundaries), and proportionally maps the resulting
rows to the reviewed timing window already present in each JSON file.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TRANSCRIPTS = ROOT / "app" / "src" / "main" / "assets" / "transcripts"

# Burmese clause punctuation plus the separators used by the supplied transcripts.
CLAUSE_RE = re.compile(r"(?<=[၊။,;:!?|—–-])\s*|\s+(?=xx(?:\b|$))")
SPACE_RE = re.compile(r"\s+")
MAX_CHARS = 48


def clean(text: str) -> str:
    text = SPACE_RE.sub(" ", text.replace("\u200b", " ")).strip()
    return text


def split_text(text: str) -> list[str]:
    text = clean(text)
    if not text:
        return []
    parts = [clean(p) for p in CLAUSE_RE.split(text) if clean(p)]
    result: list[str] = []
    for part in parts:
        if len(part) <= MAX_CHARS:
            result.append(part)
            continue
        # Long OCR paragraphs often have no punctuation. Break them at whitespace,
        # retaining words and avoiding tiny orphan lines.
        words = part.split(" ")
        current = ""
        for word in words:
            candidate = word if not current else current + " " + word
            if current and len(candidate) > MAX_CHARS:
                result.append(current)
                current = word
            else:
                current = candidate
        if current:
            result.append(current)
    return result


def redistribute(line: dict, parts: list[str]) -> list[dict]:
    start = max(0.0, float(line.get("start", 0.0)))
    end = max(start + 0.1, float(line.get("end", start + 0.1)))
    if len(parts) == 1:
        return [{"start": round(start, 2), "end": round(end, 2), "text": parts[0]}]
    weights = [max(1, len(p.replace(" ", ""))) for p in parts]
    total = sum(weights)
    result = []
    cursor = start
    for index, (part, weight) in enumerate(zip(parts, weights)):
        next_cursor = end if index == len(parts) - 1 else cursor + (end - start) * weight / total
        result.append({"start": round(cursor, 2), "end": round(next_cursor, 2), "text": part})
        cursor = next_cursor
    return result


def repair_file(path: Path) -> tuple[int, int]:
    data = json.loads(path.read_text(encoding="utf-8"))
    original = data.get("lines", [])
    if int(data.get("version", 1)) >= 5:
        return len(original), len(original)
    repaired: list[dict] = []
    for line in original:
        parts = split_text(str(line.get("text", "")))
        repaired.extend(redistribute(line, parts))

    # Enforce monotonic, non-overlapping intervals after rounding. This is the
    # invariant consumed by KaraokeActivity.findActiveLine().
    for index, line in enumerate(repaired):
        start = float(line["start"])
        next_start = float(repaired[index + 1]["start"]) if index + 1 < len(repaired) else float(line["end"])
        end = min(float(line["end"]), next_start)
        line["start"] = round(start, 2)
        line["end"] = round(max(start + 0.1, end), 2)

    if repaired:
        data["lines"] = repaired
        data["version"] = max(5, int(data.get("version", 1)))
        data["source"] = "Wording preserved from supplied transcript; clauses split and timing windows normalized for karaoke playback."
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return len(original), len(repaired)


def main() -> None:
    total_before = total_after = 0
    for path in sorted(TRANSCRIPTS.glob("poem_*.json")):
        before, after = repair_file(path)
        total_before += before
        total_after += after
        print(f"{path.name}: {before} -> {after} lines")
    print(f"Total: {total_before} -> {total_after} karaoke lines")


if __name__ == "__main__":
    main()
