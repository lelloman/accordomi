#!/usr/bin/env python3
"""Plot CLI JSONL observations as a standalone SVG. No DSP or external packages."""
import argparse
import html
import json
import math
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    rows = [json.loads(line) for line in args.input.read_text().splitlines() if line.strip()]
    usable = [r for r in rows if r.get('method') == 'piano' and r.get('status') == 'usable']
    if not usable:
        parser.error('No usable piano measurements to plot; inspect the JSONL rejection statuses.')
    sources = {(r['source'], r['midi'], r['reference_hz']) for r in usable}
    if len(sources) != 1:
        parser.error('Plot one recording/note/reference at a time.')
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" width="1000" height="950" viewBox="0 0 1000 950">',
             '<rect width="1000" height="950" fill="white"/>',
             '<style>text{font-family:sans-serif;fill:#172b4d} .grid{stroke:#dde3ec} </style>']

    def text(x, y, value, size=14):
        parts.append(f'<text x="{x}" y="{y}" font-size="{size}">{html.escape(str(value))}</text>')

    def plot(top, title, points, xlabel, color='#176ca4'):
        left, width, height = 100, 840, 180
        xs, ys = zip(*points)
        xmin, xmax = min(xs), max(xs)
        ymin, ymax = min(ys), max(ys)
        xpad = (xmax - xmin) * 0.03 or 0.5
        ypad = (ymax - ymin) * 0.1 or max(abs(ymin) * 0.01, 1e-6)
        xmin, xmax = xmin - xpad, xmax + xpad
        ymin, ymax = ymin - ypad, ymax + ypad
        text(left, top - 20, title, 17)
        for tick in range(5):
            fraction = tick / 4
            y = top + height * fraction
            parts.append(f'<path d="M{left},{y}h{width}" class="grid"/>')
            text(10, y + 5, f'{ymax - fraction * (ymax-ymin):.5g}', 12)
            x = left + width * fraction
            text(x - 20, top + height + 20, f'{xmin + fraction * (xmax-xmin):.4g}', 12)
        coordinates = [(left + (x - xmin)/(xmax-xmin)*width,
                        top + (ymax - y)/(ymax-ymin)*height) for x, y in points]
        parts.append(f'<polyline fill="none" stroke="{color}" stroke-width="1.5" points="' +
                     ' '.join(f'{x:.3f},{y:.3f}' for x, y in coordinates) + '"/>')
        for x, y in coordinates:
            parts.append(f'<circle cx="{x:.3f}" cy="{y:.3f}" r="3" fill="{color}"/>')
        text(left + width/2 - 70, top + height + 42, xlabel)

    first = usable[0]
    text(35, 35, f'MIDI {first["midi"]}: {Path(first["source"]).name}', 22)
    text(35, 60, f'{len(usable)}/{len(rows)} usable windows; heuristic fits, not validated tuning targets')
    expected = first['reference_hz'] * 2 ** ((first['midi'] - 69)/12)
    plot(115, 'First partial: cents relative to equal temperament (diagnostic only)',
         [(r['time_seconds'], 1200 * math.log2(r['first_partial_hz']/expected)) for r in usable], 'Window start (seconds)')
    plot(400, 'Estimated inharmonicity B',
         [(r['time_seconds'], r['B']) for r in usable], 'Window start (seconds)')
    selected = max(usable, key=lambda r: r['quality'])
    plot(685, f'Partial residuals (cents), best-quality window at {selected["time_seconds"]:.3f}s',
         [(p['number'], p['residual_cents']) for p in selected['partials']], 'Partial number', '#ad4c22')
    text(35, 935, 'Residual plot includes rejected partials. Inspect JSONL "used" flags; gaps between usable windows may contain rejections.', 12)
    parts.append('</svg>')
    args.output.write_text('\n'.join(parts))


if __name__ == '__main__':
    main()
