"""Download the original ARASAAC pictograms used by the Voz Livre prototype.

Run where static.arasaac.org and api.arasaac.org are reachable (CI). Writes
symbols/<id>.png (256x256, transparent) and scripts/arasaac-meta.json.
"""
from pathlib import Path
import io,json,sys,time,urllib.request
from PIL import Image
r=Path(__file__).resolve().parents[1]
a=r/'app/src/main/assets'
p=json.loads((a/'symbols-provenance.json').read_text(encoding='utf8'))
ids=sorted({Path(rec['file']).stem for rec in p['records'] if Path(rec['file']).stem.isdigit()},key=int)
def get(url):
    for n in range(5):
        try:
            with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'voz-livre'}),timeout=60) as f:return f.read()
        except Exception as e:
            if n==4:raise
            time.sleep(2**n)
meta={}
for i in ids:
    png=get(f'https://static.arasaac.org/pictograms/{i}/{i}_500.png')
    im=Image.open(io.BytesIO(png)).convert('RGBA')
    im.thumbnail((256,256),Image.LANCZOS)
    out=Image.new('RGBA',(256,256),(0,0,0,0));out.paste(im,((256-im.width)//2,(256-im.height)//2))
    out.save(a/'symbols'/f'{i}.png',optimize=True)
    info=json.loads(get(f'https://api.arasaac.org/v1/pictograms/pt/{i}'))
    meta[i]={'keywords':[k.get('keyword') for k in info.get('keywords',[])],'url':f'https://arasaac.org/pictograms/pt/{i}'}
    print(i,meta[i]['keywords'][:3],flush=True)
(r/'scripts/arasaac-meta.json').write_text(json.dumps(meta,ensure_ascii=False,indent=1)+'\n',encoding='utf8')
print(len(ids),'pictograms')
