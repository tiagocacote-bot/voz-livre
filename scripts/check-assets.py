"""Check attribution, bundled asset hashes, migration tables and board references."""
from pathlib import Path
import hashlib,json,re,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
a=r/'app/src/main/assets'
p=json.loads((a/'symbols-provenance.json').read_text(encoding='utf8'))
for rec in p['records']:
    assert rec['license'] in ('MIT','CC BY-SA 4.0','CC BY-NC-SA 4.0'),rec['file']
    assert hashlib.sha256((a/rec['file']).read_bytes()).hexdigest()==rec['sha256'],rec['file']
    if rec['provider']=='ARASAAC':
        assert rec['license']=='CC BY-NC-SA 4.0' and rec['author']=='Sergio Palao' and rec['url'].startswith('https://arasaac.org/'),rec['file']
    else:assert hashlib.sha256((r/rec['source']).read_bytes()).hexdigest()==rec['sourceSha256'],rec['source']
    if rec.get('aliasOf'):assert (a/rec['file']).read_bytes()==(a/rec['aliasOf']).read_bytes()
documented={rec['file'] for rec in p['records']}
assert {'symbols/'+f.name for f in (a/'symbols').iterdir()}==documented
board=json.loads((a/'default.json').read_text(encoding='utf8'))
assert board['symbolSet']=='arasaac'
buttons=[b for profile in board['profiles'] for page in profile['pages'] for b in page['buttons']]
assert len(buttons)==261 and board['starterVersion']==2
for b in buttons:assert b['symbol'] in documented,b['label']
for profile in board['profiles']:
    for routine in profile['routines']:
        for step in routine['steps']:assert step['symbol'] in documented,step['text']
legacy=json.loads((a/'legacy-symbols.json').read_text(encoding='utf8'))
ids={b['id']:b for b in buttons}
for k,(old,new) in legacy['byButton'].items():assert old in documented and new in documented and ids[k]['symbol']==new,k
for old,new in legacy['bySymbol'].items():assert old in documented and new in documented,old
assert all(k in legacy['byButton'] for k in legacy['since030'])
starter_pages={pg['id'] for pg in board['profiles'][0]['pages']}
assert set(legacy['pages030'])<=starter_pages
for seedfile in ('prediction.json','prediction-en.json','prediction-es.json'):
  seed=json.loads((a/seedfile).read_text(encoding='utf8'))
  assert all(isinstance(v,list) and all(isinstance(w,str) and w for w in v) for v in seed['next'].values())
ns='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(r/'app/src/main/AndroidManifest.xml').getroot()
assert not manifest.findall('uses-permission')
version=manifest.attrib[ns+'versionName']
assert re.search(r'VERSION = "'+re.escape(version)+'"',(r/'app/src/main/java/br/pessoal/vozlivre/BuildInfo.java').read_text(encoding='utf8'))
assert f"versionName '{version}'" in (r/'app/build.gradle').read_text(encoding='utf8')
assert f"versionCode {manifest.attrib[ns+'versionCode']}" in (r/'app/build.gradle').read_text(encoding='utf8')
assert json.loads((r/'package.json').read_text(encoding='utf8'))['version']==version
assert f'## {version}' in (r/'CHANGELOG.md').read_text(encoding='utf8')
import subprocess,sys
subprocess.run([sys.executable,str(r/'scripts/build-boards.py'),'--check'],check=True)
for lang in ('en','es'):
    other=json.loads((a/f'default-{lang}.json').read_text(encoding='utf8'))
    assert other['profiles'][0]['settings']['language']==lang
    assert [b['symbol'] for p in other['profiles'] for pg in p['pages'] for b in pg['buttons']]==[b['symbol'] for b in buttons]
keys=set()
for f in (r/'app/src/main/java/br/pessoal/vozlivre').glob('*.java'):
    for m in re.finditer(r'(?:I18n\.t|\bt)\(("(?:[^"\\\n]|\\.)*")\)',f.read_text(encoding='utf8')):keys.add(json.loads(m.group(1)))
for lang in ('en','es'):
    strings=json.loads((a/f'i18n/{lang}.json').read_text(encoding='utf8'))['strings']
    missing=sorted(keys-set(strings))
    assert not missing,(lang,'untranslated',missing[:5])
    assert all(v.strip() for v in strings.values()),lang
assert (r/'LICENSE').is_file() and (r/'THIRD_PARTY_NOTICES.md').is_file()
arasaac=sum(1 for rec in p['records'] if rec['provider']=='ARASAAC')
print(f'Asset checks passed: {len(buttons)} buttons, {len(documented)} PNGs ({arasaac} ARASAAC), version {version}, attribution, hashes and migration tables, no app permissions')
