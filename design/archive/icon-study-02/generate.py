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
    i=sum(x['family']==family for x in items)+1
    code={'Forks':'F','Three keys':'K','Silhouettes':'P'}[family]+f'{i:02d}'
    slug=code.lower()+'-'+name.lower().replace(' ','-')
    svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><title>{escape(name)}</title>{shapes}</svg>'
    (ROOT/'svg').mkdir(exist_ok=True)
    (ROOT/'svg'/f'{slug}.svg').write_text(svg+'\n')
    items.append(dict(id=code,name=name,family=family,idea=idea,file=f'svg/{slug}.svg',body=shapes))

# Fork: vary the bowl, tine proportions, segmentation, and orientation.
def fork(name,idea,left,right,base,colors=(L,M,S),width=11,angle=0):
    body=path(left,colors[0],width)+path(right,colors[1],width)+path(base,colors[2],width)
    if angle: body=f'<g transform="rotate({angle} 50 50)">{body}</g>'
    add('Forks',name,idea,body)
add('Forks','Solid original','The retained solid-fork direction from study 01.',rect(26,18,15,34,L,7)+rect(59,18,15,34,M,7)+path('M33 60Q50 76 67 60M50 69V83',S,11))
fork('Long tines','Tall tines and a compact curved root.','M31 18V50','M69 18V50','M31 63Q50 77 69 63M50 70V84')
fork('Square bowl','A squared U with soft, generous corners.','M29 20V49','M71 20V49','M29 62Q29 68 36 68H64Q71 68 71 62M50 68V83')
fork('Open root','The split bowl leaves air above the handle.','M29 21V44Q29 57 39 57','M71 21V44Q71 57 61 57','M50 71V83')
fork('Round bowl','Two curved shoulders surround an independent stem.','M29 21V43Q29 59 40 60','M71 21V43Q71 59 60 60','M50 74V84',width=10)
fork('Narrow fork','A slender, instrument-like tuning fork.','M35 18V49','M65 18V49','M35 62Q50 75 65 62M50 69V84',width=10)
fork('Broad fork','A broad stance with short tines.','M23 26V45','M77 26V45','M23 59Q50 80 77 59M50 70V83',width=12)
fork('Dark tines','Strong tines give the silhouette more contrast.','M29 20V49','M71 20V49','M29 62Q50 77 71 62M50 70V83',colors=(S,M,L),width=12)
fork('Leaning fork','A gentle tilt keeps the fork immediately legible.','M31 21V48','M69 21V48','M31 61Q50 75 69 61M50 68V82',angle=18)
fork('Deep cup','Long shoulders form a deeper U-shaped cup.','M28 19V43Q28 60 39 62','M72 19V43Q72 60 61 62','M50 75V85',width=10)
add('Forks','Key handle','A wide key-shaped stem beneath two fork tines.',path('M28 20V44Q28 54 39 54',L,12)+path('M72 20V44Q72 54 61 54',M,12)+rect(41,67,18,19,S,5))
fork('Continuous side','One tine joins the handle; the other two pieces float.','M29 20V45Q29 64 50 64V82','M71 20V43','M70 57Q67 64 62 64',colors=(S,L,M),width=11)

# Three keys: preserve key-shaped cutouts, rather than generic equalizer bars.
def key(x,top,bottom,w,notch,depth,color,r=3):
    if not notch:return rect(x,top,w,bottom-top,color,r)
    return fill(f'M{x+r} {top}H{x+w-notch}V{top+depth}H{x+w}V{bottom-r}Q{x+w} {bottom} {x+w-r} {bottom}H{x+r}Q{x} {bottom} {x} {bottom-r}V{top+r}Q{x} {top} {x+r} {top}Z',color)
def keys(name,idea,xs=(19,43,67),tops=(20,20,20),ends=(80,80,80),w=18,notches=(7,7,0),depth=29,colors=(L,M,S),angle=0,r=3):
    body=''.join(key(x,t,b,w,n,depth,c,r) for x,t,b,n,c in zip(xs,tops,ends,notches,colors))
    if angle:body=f'<g transform="rotate({angle} 50 50)">{body}</g>'
    add('Three keys',name,idea,body)
add('Three keys','Three keys original','The retained three-key mark from study 01.',fill('M19 23Q19 19 23 19H30V48H37V77Q37 81 33 81H23Q19 81 19 77Z',L)+fill('M43 19H51V48H57V81H43Z',M)+fill('M63 19H77Q81 19 81 23V77Q81 81 77 81H63Z',S))
keys('Even keys','Equal lower widths with two clear upper notches.',xs=(17,41,65))
keys('Short keys','A compact keyboard with a broad footprint.',xs=(17,41,65),tops=(27,27,27),ends=(76,76,76),depth=22)
keys('Long keys','Tall proportions suggest the length of real piano keys.',xs=(22,43,64),w=14,tops=(17,17,17),ends=(83,83,83),notches=(5,5,0),depth=32)
keys('Wide cuts','Larger negative-space cuts emphasize the keyboard.',xs=(17,41,65),notches=(9,9,0),depth=28)
keys('Shallow cuts','Shorter notches leave more of each key intact.',xs=(17,41,65),notches=(6,6,0),depth=20)
keys('Soft keys','More rounding at the ends of the three keys.',xs=(17,41,65),r=5,notches=(6,6,0))
keys('Pressed middle','The middle key is visibly depressed.',xs=(17,41,65),tops=(20,26,20),ends=(77,83,77),depth=26)
keys('Rising chord','A stepped silhouette, still built from piano keys.',xs=(17,41,65),tops=(30,24,18),ends=(82,76,70),depth=24)
keys('Dark center','The central key provides the strong-color anchor.',xs=(17,41,65),colors=(L,S,M))
keys('Tilted keyboard','A slight tilt introduces movement.',xs=(19,42,65),tops=(23,23,23),ends=(77,77,77),w=16,notches=(6,6,0),depth=24,angle=-12)
add('Three keys','Paired cuts','Two upper gaps frame a broad central piano key.',key(17,21,79,18,6,28,L)+fill('M47 21H53V49H59V76Q59 79 56 79H44Q41 79 41 76V49H47Z',M)+fill('M71 21H80Q83 21 83 24V76Q83 79 80 79H68Q65 79 65 76V49H71Z',S))

# Grand piano: one body contour and two keyboard blocks; no extra decorative detail.
def piano(name,idea,body,keyboard=(23,66,26,16,7),colors=(L,M,S),angle=0):
    x,y,w,h,gap=keyboard
    shapes=fill(body,colors[0])+rect(x,y,w,h,colors[1],4)+rect(x+w+gap,y,w,h,colors[2],4)
    if angle:shapes=f'<g transform="rotate({angle} 50 50)">{shapes}</g>'
    add('Silhouettes',name,idea,shapes)
base='M23 18H37Q58 18 57 34Q56 45 77 47Q82 48 82 54V59H23Z'
piano('Grand original','The retained piano silhouette from study 01.',base)
piano('Rounded grand','A smoother shoulder and a rounded front corner.','M25 19H37C60 19 49 42 73 45Q81 46 81 53V58H20V24Q20 19 25 19Z',(20,65,27,16,7))
piano('Concert grand','A longer tail above a compact keyboard.','M28 13H40C57 13 51 34 65 41C73 45 78 43 78 52V60H24V17Q24 13 28 13Z',(24,67,24,16,6))
piano('Baby grand','A low, wide piano with a compact body.','M22 27H38Q57 27 56 38Q55 47 74 47Q82 47 82 55V60H18V31Q18 27 22 27Z',(18,67,29,14,6))
piano('Full shoulder','A generous curved soundboard carries the identity.','M24 18H43C68 18 49 40 74 44Q82 46 82 53V60H20V22Q20 18 24 18Z',(20,67,28,15,6))
piano('Deep waist','A pronounced concave waist separates tail and keyboard.','M26 17H40Q58 17 52 33Q47 48 76 45Q82 45 82 53V59H22V21Q22 17 26 17Z',(22,66,27,17,6))
piano('Dark soundboard','A strong-color body makes the piano shape dominant.',base,colors=(S,L,M))
piano('Green soundboard','A medium-green body balances two contrasting keys.',base,colors=(M,L,S))
piano('Short keyboard','More weight in the body, less in the keys.','M24 17H39Q59 17 57 34Q55 46 77 48Q82 49 82 55V63H22V21Q22 17 24 17Z',(22,70,27,11,6))
piano('Deep keyboard','Longer key blocks emphasize the instrument.','M25 20H38Q57 20 56 33Q55 42 76 44Q81 45 81 51V55H22V24Q22 20 25 20Z',(22,62,26,21,7))
piano('Slanted grand','A modest rotation adds energy to the piano body.',base,angle=-10)
piano('Left shoulder','The piano contour mirrored, with the same color sequence.','M77 18H63Q42 18 43 34Q44 45 23 47Q18 48 18 54V59H77Z',(18,66,26,16,7))
(ROOT/'proposals.json').write_text(json.dumps([{k:v for k,v in x.items() if k!='body'} for x in items],indent=2)+'\n')
# A standalone, vector contact sheet; white tiles remain separate from the marks.
parts=['<svg xmlns="http://www.w3.org/2000/svg" width="1440" height="1690" viewBox="0 0 1440 1690">','<rect width="1440" height="1690" fill="#f3f4f6"/>','<g font-family="sans-serif" fill="#111827">','<text x="40" y="50" font-size="30" font-weight="bold">Accordomi / Forks, keys, silhouettes</text>','<text x="40" y="80" font-size="16">LelloDesign · Android green · flat geometry · September 2026</text>']
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
