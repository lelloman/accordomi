"""Rebuild the Accordomi LelloDesign SVG study and comparison sheet."""
from pathlib import Path
import json
from html import escape

ROOT = Path(__file__).resolve().parent
L, M, S = '#b9ffd2', '#3ddc84', '#006c45'
def path(d, c, w=10):
    return f'<path d="{d}" fill="none" stroke="{c}" stroke-width="{w}" stroke-linecap="round" stroke-linejoin="round"/>'
def fill(d, c): return f'<path d="{d}" fill="{c}"/>'
def rect(x,y,w,h,c,r=5): return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{c}"/>'
def dot(x,y,r,c): return f'<circle cx="{x}" cy="{y}" r="{r}" fill="{c}"/>'
items=[]
def add(family, name, idea, shapes):
    i=len(items)+1
    slug=f'{i:02d}-'+name.lower().replace(' ','-')
    svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><title>{escape(name)}</title>{shapes}</svg>'
    (ROOT/'svg').mkdir(exist_ok=True)
    (ROOT/'svg'/f'{slug}.svg').write_text(svg+'\n')
    items.append(dict(id=f'{i:02d}',name=name,family=family,idea=idea,file=f'svg/{slug}.svg',body=shapes))

add('Forks','Open fork','A tuning fork split into three broad strokes.',path('M29 20V43Q29 57 40 57',L)+path('M71 20V43Q71 57 60 57',M)+path('M50 70V82',S,12))
add('Forks','Solid fork','A compact fork with a separate tuning handle.',rect(26,18,15,34,L,7)+rect(59,18,15,34,M,7)+path('M33 60Q50 76 67 60M50 69V83',S,11))
add('Forks','Fork and pulse','A familiar fork, with one pulse of resonance.',path('M31 22V46Q31 61 49 61V81',S)+path('M67 22V46Q67 53 62 56',M)+path('M42 26Q49 17 56 26',L,8))
add('Forks','Angled fork','A tilted fork gives the mark a lively silhouette.',f'<g transform="rotate(25 50 50)">{path("M30 20V45Q30 56 40 56",L)}{path("M70 20V45Q70 56 60 56",M)}{path("M50 70V82",S,12)}</g>')
add('Forks','Wide fork','A low, broad fork with an unmistakable stem.',path('M22 25V42Q22 58 42 58',L,12)+path('M78 25V42Q78 58 58 58',M,12)+rect(44,66,12,19,S,6))
add('Forks','Fork key','The fork handle becomes a piano key.',path('M31 20V42Q31 54 43 54',L)+path('M69 20V42Q69 54 57 54',M)+rect(41,62,18,23,S,5))

add('Keys','Three keys','Three notched piano keys, reduced to a compact mark.',fill('M19 23Q19 19 23 19H30V48H37V77Q37 81 33 81H23Q19 81 19 77Z',L)+fill('M43 19H51V48H57V81H43Z',M)+fill('M63 19H77Q81 19 81 23V77Q81 81 77 81H63Z',S))
add('Keys','Stepped keys','A descending keyboard with generous open cuts.',fill('M19 26Q19 21 24 21H35V49H40V79H24Q19 79 19 74Z',L)+fill('M46 21H57V49H62V79H46Z',M)+rect(68,21,13,58,S,4))
add('Keys','Key fan','Three keys spread like a small chord.',f'<g transform="rotate(-18 30 52)">{rect(17,26,15,49,L)}</g>'+rect(43,18,14,63,M)+f'<g transform="rotate(18 72 52)">{rect(69,26,15,49,S)}</g>')
add('Keys','One piano key','One key, its raised neighbor, and a sounding pulse.',fill('M25 20Q25 16 29 16H45V49H58V80Q58 84 54 84H29Q25 84 25 80Z',L)+rect(51,16,15,26,S,4)+path('M71 56Q82 65 71 74',M,9))
add('Keys','Grand silhouette','A curved piano body above two broad key blocks.',fill('M23 18H37Q58 18 57 34Q56 45 77 47Q82 48 82 54V59H23Z',L)+rect(23,66,26,16,M,4)+rect(56,66,26,16,S,4))
add('Keys','Sounding keys','Two keys and a curved soundboard.',rect(22,27,17,48,L)+rect(45,19,17,56,M)+path('M74 31Q87 51 74 71',S,10))

add('Strings','Three strings','A quiet unison: three parallel strings.',path('M28 29V71',L,12)+path('M50 19V81',S,12)+path('M72 29V71',M,12))
add('Strings','Convergence','Outer strings settle toward a central reference.',path('M23 23Q43 50 23 77',L)+path('M50 20V80',S)+path('M77 23Q57 50 77 77',M))
add('Strings','String bridge','Three lengths of string share a slanted bridge.',path('M27 36V77',L,12)+path('M50 26V69',M,12)+path('M73 17V60',S,12))
add('Strings','Quiet unison','Vibrating strings surround one stable string.',path('M25 22C43 39 7 61 25 78',L,9)+path('M50 22V78',S,11)+path('M75 22C57 39 93 61 75 78',M,9))
add('Strings','Harmonic stack','Three horizontal partials of decreasing length.',path('M21 29H79',L,12)+path('M29 50H71',M,12)+path('M39 71H61',S,12))
add('Strings','String anchor','An isolated string supported by two rounded bridges.',path('M26 27Q50 12 74 27',L,11)+path('M50 35V65',S,12)+path('M26 73Q50 88 74 73',M,11))

add('Meters','Centered needle','An open pitch dial with a centered needle.',path('M20 57Q18 30 41 23',L,12)+path('M59 23Q82 30 80 57',M,12)+path('M50 38V74',S,12))
add('Meters','Pitch gate','Two brackets frame the tuned center.',path('M34 26H24V74H34',L,10)+path('M66 26H76V74H66',M,10)+rect(44,33,12,34,S,6))
add('Meters','Meet in tune','Two arrows meet at a single pitch reference.',path('M21 34L37 50L21 66',L,11)+path('M79 34L63 50L79 66',M,11)+path('M50 24V76',S,9))
add('Meters','Tuning horizon','A settled pitch between two floating guides.',path('M19 56H35',L,12)+path('M65 56H81',M,12)+path('M50 25V73',S,12))
add('Meters','Sweet spot','A clear center beneath a split tuning arc.',path('M20 46Q25 23 43 22',L,12)+path('M57 22Q75 23 80 46',M,12)+dot(50,65,14,S))
add('Meters','Level pitch','Two bars and a short needle express alignment.',rect(19,29,24,12,L,6)+rect(57,29,24,12,M,6)+path('M50 49V79',S,12))

add('Waves','Single cycle','A waveform in three separate sweeping strokes.',path('M17 53Q24 53 27 39',L,10)+path('M37 27C49 20 48 79 62 73',S,10)+path('M74 61Q78 47 85 47',M,10))
add('Waves','Resonance','Three open arcs with a shared acoustic center.',path('M22 40Q31 50 22 60',S,10)+path('M42 28Q62 50 42 72',M,10)+path('M63 18Q91 50 63 82',L,10))
add('Waves','Breathing chord','Three softened waves of differing amplitudes.',path('M19 31Q50 13 81 31',L,10)+path('M19 50Q50 72 81 50',M,10)+path('M32 81Q50 78 68 81',S,10))
add('Waves','Partial peaks','Three separated spectral peaks.',path('M16 70L24 38L32 70',L,9)+path('M44 70L52 20L60 70',S,9)+path('M72 70L79 49L86 70',M,9))
add('Waves','Settling wave','Oscillation becomes a stable reference line.',path('M15 45Q23 24 30 45',L,10)+path('M43 55Q50 76 57 55',M,10)+path('M71 51H84',S,10))
add('Waves','Octave waves','Two pitches meet around a steady center.',path('M20 32Q35 17 50 32T80 32',L,10)+path('M20 68Q35 83 50 68T80 68',M,10)+path('M36 50H64',S,10))

add('Monograms','Open A','Accordomi initial with a separate central bridge.',path('M23 78L41 23',L,13)+path('M59 23L77 78',M,13)+path('M42 60H58',S,11))
add('Monograms','Fork A','An A-shaped pair of tines over a short handle.',path('M24 64L41 22',L,12)+path('M59 22L76 64',M,12)+path('M41 66H59M50 70V82',S,10))
add('Monograms','Key A','Three piano-like planes form a geometric A.',fill('M19 78L37 24Q39 18 45 18H48L34 78Q33 82 28 82H23Q18 82 19 78Z',L)+fill('M56 18H59Q64 18 66 24L83 78Q85 82 79 82H73Q68 82 67 77Z',M)+rect(41,57,18,12,S,5))
add('Monograms','Tuned A','An open initial with the tuning point at its heart.',path('M22 78L41 22',L,12)+path('M59 22L78 78',M,12)+dot(50,61,9,S))
add('Monograms','Chord A','Three strokes suggest an A and its sounding bridge.',path('M22 77L41 23',S,12)+path('M57 25L78 77',M,12)+path('M40 62Q50 52 60 62',L,10))
add('Monograms','Octave A','A low, wide initial with a substantial key bridge.',path('M19 73L43 27',L,13)+path('M57 27L81 73',M,13)+rect(40,56,20,22,S,5))

(ROOT/'proposals.json').write_text(json.dumps([{k:v for k,v in x.items() if k!='body'} for x in items],indent=2)+'\n')
# A standalone, vector contact sheet; white tiles remain separate from the marks.
parts=['<svg xmlns="http://www.w3.org/2000/svg" width="1440" height="1690" viewBox="0 0 1440 1690">','<rect width="1440" height="1690" fill="#f3f4f6"/>','<g font-family="sans-serif" fill="#111827">','<text x="40" y="50" font-size="30" font-weight="bold">Accordomi / 36 icon proposals</text>','<text x="40" y="80" font-size="16">LelloDesign · Android green · flat geometry · September 2026</text>']
for idx,it in enumerate(items):
    col,row=idx%6,idx//6
    x,y=40+col*232,110+row*260
    if col==0: parts.append(f'<text x="40" y="{y+16}" font-size="15" font-weight="bold">{it["family"].upper()}</text>')
    parts.append(f'<rect x="{x+32}" y="{y+30}" width="160" height="160" rx="35.2" fill="white"/>')
    parts.append(f'<g transform="translate({x+32} {y+30}) scale(1.6)">{it["body"]}</g>')
    parts.append(f'<text x="{x+112}" y="{y+216}" font-size="17" text-anchor="middle">{it["id"]} · {it["name"]}</text>')
parts.append('</g></svg>')
(ROOT/'contact-sheet.svg').write_text(''.join(parts)+'\n')
print(f'Wrote {len(items)} SVG proposals and contact sheet')
manifest=[{k:v for k,v in x.items() if k!='body'} for x in items]
(ROOT/'index.html').write_text((ROOT/'gallery.template.html').read_text().replace('__PROPOSALS__',json.dumps(manifest)))
