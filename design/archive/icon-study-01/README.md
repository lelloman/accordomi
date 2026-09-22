# Accordomi icon study 01

Open [index.html](index.html) directly in a browser: 36 original SVG proposals,
family filters, 16/24/32/48/100/192 px previews, light/dark surfaces, transparent
or white-tile presentation, downloadable masters, and a local shortlist.
The gallery needs no server or dependencies.

[PNG contact sheet](contact-sheet.png) · [Vector contact sheet](contact-sheet.svg)

## Design basis

Read against LelloDesign's [product icon guidelines](../../../../lellodesign/icons/guidelines.md),
[reference marks](../../../../lellodesign/icons/examples.md), and
[design language](../../../../lellodesign/docs/design-language.md).

- Android family: light `#b9ffd2`, medium `#3ddc84`, strong `#006c45`.
- Transparent `0 0 100 100` SVG masters; flat shapes or rounded strokes.
- Broad geometry with open gaps and a small number of main pieces.
- Separate white presentation tile with a 22% corner radius.
- The same geometry and palette at every preview size and appearance.
- No shared-account hexagon, gradients, shadows, or alternate small-size artwork.

These are proposals, not adopted app assets. The launcher resources and the older
root icon-lab.html are unchanged. Adaptive-icon masking and Android monochrome
artwork will be prepared after a direction is selected.

## Directions

| IDs | Family | Intent |
| --- | --- | --- |
| 01–06 | Forks | Clearly communicate tuning across instruments |
| 07–12 | Keys | Emphasize piano identity |
| 13–18 | Strings | Communicate resonance, partials, and unisons |
| 19–24 | Meters | Communicate precise pitch alignment |
| 25–30 | Waves | Explore sound and harmonic relationships |
| 31–36 | Monograms | Give Accordomi a distinctive initial |

Initial editorial shortlist: **02 Solid fork**, **07 Three keys**,
**16 Quiet unison**, **19 Centered needle**, **34 Tuned A**.
02 has the clearest tuning metaphor; 07 is most piano-specific; 16 connects to
string measurement; 19 describes the task immediately; 34 is the strongest
name-led direction. These are design judgments, not user preference results.
The pale green naturally loses contrast on white; compare the whole silhouette
and strong-tone anchor at small sizes. The white tile is preferable on dark
surroundings, as suggested by LelloDesign.

## Files and regeneration

`svg/` contains the 36 editable masters. `proposals.json` identifies each mark.
`generate.py` is the geometry source and generates the masters, contact sheet,
manifest, and gallery from `gallery.template.html`.

```sh
python3 design/icon-proposals/generate.py
rsvg-convert -o design/icon-proposals/contact-sheet.png design/icon-proposals/contact-sheet.svg
```

`references/` contains unmodified snapshots copied from
`lellodesign/assets/icons/` on 2026-09-22, only for side-by-side comparison.
Their established blue colors remain intact; they are not Accordomi proposals.
