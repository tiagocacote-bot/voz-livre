"""Generate the English and Spanish starter boards from default.json and board-translations.json.

python scripts/build-boards.py          writes default-en.json and default-es.json
python scripts/build-boards.py --check  fails if they are missing or out of date
"""
from pathlib import Path
import copy,json,sys
r=Path(__file__).resolve().parents[1]
a=r/'app/src/main/assets'
board=json.loads((a/'default.json').read_text(encoding='utf8'))
tr=json.loads((r/'scripts/board-translations.json').read_text(encoding='utf8'))
text=tr['text']
def translate(s,i):
    if not s:return s
    if s not in text:raise SystemExit(f'Missing translation: {s!r}')
    return text[s][i]
stale=[]
for i,lang in enumerate(['en','es']):
    b=copy.deepcopy(board)
    for p in b['profiles']:
        p['name']=tr['profile'][lang]['name']
        p['settings']['language']=tr['profile'][lang]['language']
        for pg in p['pages']:
            pg['title']=translate(pg['title'],i)
            for x in pg['buttons']:
                x['label']=translate(x['label'],i)
                x['message']=translate(x['message'],i)
        for rt in p['routines']:
            rt['title']=translate(rt['title'],i)
            for st in rt['steps']:st['text']=translate(st['text'],i)
    out=json.dumps(b,ensure_ascii=False,indent=2)+'\n'
    f=a/f'default-{lang}.json'
    if '--check' in sys.argv:
        if not f.exists() or f.read_text(encoding='utf8')!=out:stale.append(f.name)
    else:f.write_text(out,encoding='utf8')
if stale:raise SystemExit('Run python scripts/build-boards.py: out of date: '+', '.join(stale))
print('Starter boards:',', '.join(f'default-{l}.json' for l in ('en','es')),'up to date' if '--check' in sys.argv else 'written')
