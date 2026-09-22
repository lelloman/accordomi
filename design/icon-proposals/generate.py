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
    code=f'L{i:02d}'
    slug=code.lower()+'-'+name.lower().replace(' ','-')
    svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><title>{escape(name)}</title>{shapes}</svg>'
    (ROOT/'svg').mkdir(exist_ok=True)
    (ROOT/'svg'/f'{slug}.svg').write_text(svg+'\n')
    items.append(dict(id=code,name=name,family=family,idea=idea,file=f'svg/{slug}.svg',body=shapes))

def fork(family,name,idea,left='M31 21V48',right='M69 21V48',base='M31 61Q50 75 69 61M50 68V82',angle=18,width=11):
    shapes=path(left,L,width)+path(right,M,width)+path(base,S,width)
    add(family,name,idea,f'<g transform="rotate({angle} 50 50)">{shapes}</g>')
# One variable at a time; all shape studies keep the original 18 degree angle.
fork('Tilt','Original F09','Reference: the exact F09 geometry, at 18 degrees.')
fork('Tilt','Gentle lean','12 degrees; original shape and stroke weight.',angle=12)
fork('Tilt','Lively lean','24 degrees; original shape and stroke weight.',angle=24)
fork('Tilt','Bold lean','30 degrees; original shape and stroke weight.',angle=30)
fork('Proportions','Long tines','Tines extend upward by 5 units; original bowl and handle.',left='M31 16V48',right='M69 16V48')
fork('Proportions','Short tines','Tines start 6 units lower; original bowl and handle.',left='M31 27V48',right='M69 27V48')
fork('Proportions','Narrow fork','Tines and bowl are 8 units narrower; same height.',left='M35 21V48',right='M65 21V48',base='M35 61Q50 75 65 61M50 68V82')
fork('Proportions','Broad fork','Tines and bowl are 8 units wider; same height.',left='M27 21V48',right='M73 21V48',base='M27 61Q50 75 73 61M50 68V82')
fork('Bowl','Shallow bowl','A flatter bowl; the handle starts at its new center.',base='M31 61Q50 69 69 61M50 65V82')
fork('Bowl','Deep bowl','A deeper curved root, with the same overall height.',base='M31 61Q50 83 69 61M50 72V82')
fork('Bowl','Soft square','A short flat bottom with rounded shoulders.',base='M31 61Q31 69 39 69H61Q69 69 69 61M50 69V82')
fork('Bowl','Rounded V','Slanted shoulders converge at a gently rounded root.',base='M31 61L45 70Q50 73 55 70L69 61M50 72V82')
(ROOT/'proposals.json').write_text(json.dumps([{k:v for k,v in x.items() if k!='body'} for x in items],indent=2)+'\n')
# A standalone, vector contact sheet; white tiles remain separate from the marks.
parts=['<svg xmlns="http://www.w3.org/2000/svg" width="1120" height="920" viewBox="0 0 1120 920">','<rect width="1120" height="920" fill="#f3f4f6"/>','<g font-family="sans-serif" fill="#111827">','<text x="40" y="50" font-size="30" font-weight="bold">Accordomi / Leaning fork refinements</text>','<text x="40" y="80" font-size="16">LelloDesign · Android green · flat geometry · September 2026</text>']
for idx,it in enumerate(items):
    col,row=idx%4,idx//4
    x,y=40+col*270,110+row*260
    if col==0: parts.append(f'<text x="40" y="{y+16}" font-size="15" font-weight="bold">{it["family"].upper()}</text>')
    parts.append(f'<rect x="{x+32}" y="{y+30}" width="160" height="160" rx="35.2" fill="white"/>')
    parts.append(f'<g transform="translate({x+32} {y+30}) scale(1.6)">{it["body"]}</g>')
    parts.append(f'<text x="{x+112}" y="{y+216}" font-size="17" text-anchor="middle">{it["id"]} · {it["name"]}</text>')
parts.append('</g></svg>')
(ROOT/'contact-sheet.svg').write_text(''.join(parts)+'\n')
print(f'Wrote {len(items)} SVG proposals and contact sheet')
manifest=[{k:v for k,v in x.items() if k!='body'} for x in items]
(ROOT/'index.html').write_text((ROOT/'gallery.template.html').read_text().replace('__PROPOSALS__',json.dumps(manifest)))
