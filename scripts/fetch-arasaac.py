"""Download the original ARASAAC pictograms used by the Voz Livre prototype.

Run where static.arasaac.org and api.arasaac.org are reachable (CI). Writes
symbols/<id>.png (256x256, transparent) and scripts/arasaac-meta.json.
After a run, update symbols-provenance.json hashes and run scripts/check-assets.py.
"""
from pathlib import Path
import io,json,sys,time,urllib.parse,urllib.request
from PIL import Image
r=Path(__file__).resolve().parents[1]
a=r/'app/src/main/assets'
p=json.loads((a/'symbols-provenance.json').read_text(encoding='utf8'))
ids=sorted({Path(rec['file']).stem for rec in p['records'] if Path(rec['file']).stem.isdigit()},key=int)
# Starter words that had no ARASAAC pictogram in the prototype: take ARASAAC's best match.
EXTRA=['comida','corpo','não gostar']  # 'delicioso' and 'confortável' had no result; 'avó' only matched a grandfather
def get(url,tries=5):
    for n in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'voz-livre'}),timeout=60) as f:return f.read()
        except Exception as e:
            if n==tries-1:raise
            time.sleep(2**n)
meta={}
found={}
missing=[]
for term in EXTRA:
    hit=None
    for kind in ('bestsearch','search'):
        try:
            hits=json.loads(get(f'https://api.arasaac.org/v1/pictograms/pt/{kind}/'+urllib.parse.quote(term),tries=2))
            if hits:hit=hits[0]['_id'];break
        except Exception as e:print('search failed',kind,term,e,flush=True)
    if hit is None:missing.append(term);continue
    found[str(hit)]=term
    print('search',term,'->',hit,flush=True)
ids+=[i for i in found if i not in ids]
terms=json.loads((r/'scripts/arasaac-terms.json').read_text(encoding='utf8'))['terms']
picked={}
for label,term in terms.items():
    hit=None
    for kind in ('bestsearch','search'):
        try:
            hits=json.loads(get(f'https://api.arasaac.org/v1/pictograms/es/{kind}/'+urllib.parse.quote(term),tries=2))
            if hits:hit=str(hits[0]['_id']);break
        except Exception as e:print('search failed',kind,term,e,flush=True)
    if hit is None:missing.append(label);continue
    picked[label]=hit
    if hit not in ids:ids.append(hit)
    found.setdefault(hit,label)
    print('term',label,term,'->',hit,flush=True)
old=json.loads((r/'scripts/arasaac-meta.json').read_text(encoding='utf8')) if (r/'scripts/arasaac-meta.json').exists() else {}
for i in ids:
    if (a/'symbols'/f'{i}.png').exists() and i in old and i not in picked.values():
        meta[i]=old[i];continue
    png=get(f'https://static.arasaac.org/pictograms/{i}/{i}_500.png')
    im=Image.open(io.BytesIO(png)).convert('RGBA')
    im.thumbnail((256,256),Image.LANCZOS)
    out=Image.new('RGBA',(256,256),(0,0,0,0));out.paste(im,((256-im.width)//2,(256-im.height)//2))
    out.save(a/'symbols'/f'{i}.png',optimize=True)
    info=json.loads(get(f'https://api.arasaac.org/v1/pictograms/pt/{i}'))
    meta[i]={'search':found.get(i),'keywords':[k.get('keyword') for k in info.get('keywords',[])],'url':f'https://arasaac.org/pictograms/pt/{i}'}
    print(i,meta[i]['keywords'][:3],flush=True)
meta['_missing']=missing
meta['_terms']=picked
(r/'scripts/arasaac-meta.json').write_text(json.dumps(meta,ensure_ascii=False,indent=1)+'\n',encoding='utf8')
print(len(ids),'pictograms')
