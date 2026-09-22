# Accordomi icon — Gentle lean

Selected by the user: study 03 **L02 Gentle lean**, a 12° clockwise tuning fork.

`accordomi.svg` is the canonical transparent 100 × 100 mark. Preserve its
geometry, orientation, proportions and color order. The three strokes use
LelloDesign Android green: `#b9ffd2`, `#3ddc84`, `#006c45`.

`accordomi-tile.svg` and `.png` present it on a white tile with a 22% radius.
The tile is separate from the identity artwork.

Android adaptive foregrounds map the complete 100-unit canvas uniformly into
72 units of the 108-unit viewport, centered with an 18-unit translation. The
mark stays within the 66-unit safe circle. The background is white. Android's
themed monochrome layer preserves the same three strokes and 12° rotation,
using black as the tintable mask. Legacy normal and round WebP icons are
provided at all five existing densities.

Regenerate from this repository's root with:

```sh
python3 design/brand/generate.py
```

Requires `rsvg-convert` and ImageMagick `convert`. Proposal galleries are design
history; this directory is the selected artwork source.
