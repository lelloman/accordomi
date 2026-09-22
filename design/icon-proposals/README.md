# Accordomi icon study 03 — Leaning fork

[Gallery](index.html) · [Contact sheet](contact-sheet.png)

Twelve focused refinements of the selected F09 Leaning fork. L01 preserves its
geometry exactly. All use the same flat, three-part construction, 11-unit stroke
weight, 100 × 100 transparent SVG canvas, and LelloDesign Android palette.

| IDs | Comparison |
| --- | --- |
| L01–L04 | Original 18°, then 12°, 24°, 30° tilt |
| L05–L08 | Longer, shorter, narrower, broader tines and fork proportions |
| L09–L12 | Shallow, deep, softly squared, rounded-V bowls |

Shape studies keep the original 18° tilt. The white tile is presentation only.
The gallery has a fresh shortlist independent of earlier rounds and the same
16/24/32/48/100/192 px previews, dark/light surfaces and downloadable masters.

Editorial preference: L05's longer tines emphasize the tuning-fork metaphor;
L01 remains the direct reference. No production launcher resources are changed.

Previous rounds: [study 02](../archive/icon-study-02/index.html),
[study 01](../archive/icon-study-01/index.html).

Design basis: [LelloDesign icon guidelines](../../../lellodesign/icons/guidelines.md).
Reference SVGs in `references/` are unchanged copies from LelloDesign.

Regenerate:

```sh
python3 design/icon-proposals/generate.py
rsvg-convert -o design/icon-proposals/contact-sheet.png design/icon-proposals/contact-sheet.svg
```
