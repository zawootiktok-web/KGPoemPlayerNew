# Karaoke Synchronization Repair

## Changes made

The karaoke player was repaired in two layers. First, the transcript data was normalized across all 63 poems. OCR/PDF-derived JSON rows that contained several clauses in one very long row were split at supplied punctuation and safe word boundaries, while preserving the original wording and each row's reviewed timing window. Every generated row was normalized to a positive, monotonic, non-overlapping interval inside the corresponding MP3 duration. The repair expanded the data from 374 to 555 karaoke lines.

Second, `KaraokeActivity.findActiveLine()` was corrected. Previously, once a line started it remained highlighted until another line started, even after that line's `end` timestamp. It now highlights a line only when `start <= playbackPosition < end`, and clears the highlight during gaps or after the last line. Seek-bar movement therefore uses the same interval logic as normal playback.

The repeatable repair utility is `tools/repair_karaoke_timing.py`. It is idempotent: repaired version-5 JSON files are not split again if the utility is rerun.

## Validation

- `python3 tools/audit_content.py`: passed; all 63 poems audited with `issues=0`.
- Independent interval validation: passed; 63 JSON files checked, 0 errors.
- Every lyric interval is positive, monotonic, non-overlapping, and within the matching audio duration.
- Android Gradle build could not be completed in this sandbox because no Android SDK is installed or configured (`local.properties` / `ANDROID_HOME` missing). This is an environment limitation, not a Java or transcript validation failure.

## Manual recommendation

Because several original transcripts are OCR/audio-derived drafts, a final listening pass on a physical Android device is still recommended for word-level editorial corrections. The timing and playback logic now correctly respect the bundled line intervals.
