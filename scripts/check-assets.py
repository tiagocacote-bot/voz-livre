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
assert len(buttons)==166
for b in buttons:assert b['symbol'] in documented,b['label']
legacy=json.loads((a/'legacy-symbols.json').read_text(encoding='utf8'))
ids={b['id']:b for b in buttons}
for k,(old,new) in legacy['byButton'].items():assert old in documented and new in documented and ids[k]['symbol']==new,k
for old,new in legacy['bySymbol'].items():assert old in documented and new in documented,old
seed=json.loads((a/'prediction.json').read_text(encoding='utf8'))
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
assert (r/'LICENSE').is_file() and (r/'THIRD_PARTY_NOTICES.md').is_file()
arasaac=sum(1 for rec in p['records'] if rec['provider']=='ARASAAC')
print(f'Asset checks passed: {len(buttons)} buttons, {len(documented)} PNGs ({arasaac} ARASAAC), version {version}, attribution, hashes and migration tables, no app permissions')
