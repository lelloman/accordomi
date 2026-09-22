"""Generate Android launcher artwork from the selected canonical SVG.
Requires rsvg-convert and ImageMagick convert for legacy WebP exports.
"""
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parent
RES=ROOT.parents[1]/'app/src/main/res'
svg=ET.parse(ROOT/'accordomi.svg').getroot()
group=svg.find('{http://www.w3.org/2000/svg}g')
assert group.get('transform')=='rotate(12 50 50)'
paths=list(group)
ANDROID='http://schemas.android.com/apk/res/android'
ET.register_namespace('android',ANDROID)
def attrs(**values):return {f'{{{ANDROID}}}{k}':str(v) for k,v in values.items()}
for mono in (False,True):
    vector=ET.Element('vector',attrs(width='108dp',height='108dp',viewportWidth=108,viewportHeight=108))
    # Canonical 100-unit canvas occupies the 72-unit visible launcher region.
    # Actual mark sits inside Android's 66-unit safe circle.
    outer=ET.SubElement(vector,'group',attrs(translateX=18,translateY=18,scaleX=.72,scaleY=.72))
    inner=ET.SubElement(outer,'group',attrs(rotation=12,pivotX=50,pivotY=50))
    for p in paths:
        ET.SubElement(inner,'path',attrs(fillColor='#00000000',pathData=p.get('d'),strokeColor='#000000' if mono else p.get('stroke'),strokeWidth=p.get('stroke-width'),strokeLineCap='round',strokeLineJoin='round'))
    ET.indent(vector)
    name='ic_launcher_monochrome.xml' if mono else 'ic_launcher_foreground.xml'
    ET.ElementTree(vector).write(RES/'drawable'/name,encoding='utf-8',xml_declaration=True)
(RES/'drawable/ic_launcher_background.xml').write_text('<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n    <solid android:color="#FFFFFF" />\n</shape>\n')
body=ET.tostring(group,encoding='unicode').replace('ns0:','').replace(':ns0','')
def artwork(background):return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">{background}{body}</svg>'
with tempfile.TemporaryDirectory(prefix='accordomi-icon-') as temp:
    for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
        for round_icon in (False,True):
            bg='<circle cx="50" cy="50" r="50" fill="white"/>' if round_icon else '<rect width="100" height="100" rx="22" fill="white"/>'
            source=Path(temp)/'icon.svg';source.write_text(artwork(bg))
            png=Path(temp)/'icon.png'
            subprocess.run(['rsvg-convert','-w',str(size),'-h',str(size),'-o',str(png),str(source)],check=True)
            name='ic_launcher_round.webp' if round_icon else 'ic_launcher.webp'
            subprocess.run(['convert',str(png),'-define','webp:lossless=true',str(RES/f'mipmap-{density}'/name)],check=True)
(ROOT/'accordomi-tile.svg').write_text(artwork('<rect width="100" height="100" rx="22" fill="white"/>'))
subprocess.run(['rsvg-convert','-w','512','-h','512','-o',str(ROOT/'accordomi-tile.png'),str(ROOT/'accordomi-tile.svg')],check=True)
print('Generated adaptive, monochrome, and all ten legacy launcher icons.')
