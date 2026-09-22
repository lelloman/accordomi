# Accordomi icon study 02

[Open gallery](index.html) · [Contact sheet](contact-sheet.png)

Only the three selected directions remain in the active study:

- **F01–F12: tuning forks.** Bowl geometry, tine length, width, segmentation, tilt and color balance.
- **K01–K12: three piano keys.** Key proportions, cutouts, rounding, a pressed key, orientation and color balance.
- **P01–P12: grand-piano silhouettes.** Soundboard contours, body/keyboard proportions, orientation and color balance.

F01 retains study 01's 02 Solid fork, K01 retains 07 Three keys, and P01 retains
11 Grand silhouette. The other 33 proposals are variations of these concepts.
The previous study is archived at [study 01](../icon-study-01/index.html);
its discarded directions are absent from the active gallery.

All masters use transparent 100 × 100 SVGs, the exact LelloDesign Android greens
(`#b9ffd2`, `#3ddc84`, `#006c45`), and flat geometry. The white 22%-radius tile
is separate presentation. Gallery controls provide 16/24/32/48/100/192 px sizes,
light/dark backgrounds, transparent/tile presentation, SVG downloads and a new,
independent shortlist for this round.

Design references: [icon guidelines](../../../../lellodesign/icons/guidelines.md)
and [examples](../../../../lellodesign/icons/examples.md). `references/` contains
unchanged copies of the five LelloDesign reference marks for comparison.
The app launcher has not been changed.

Regenerate with:

```sh
python3 design/icon-proposals/generate.py
rsvg-convert -o design/icon-proposals/contact-sheet.png design/icon-proposals/contact-sheet.svg
```

`generate.py` defines the geometry. `gallery.template.html` defines the gallery;
`proposals.json` and `svg/` are generated outputs.
