"""Check attribution, bundled asset hashes, aliases and board references."""
from pathlib import Path
import hashlib,json,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
a=r/'app/src/main/assets'
p=json.loads((a/'symbols-provenance.json').read_text(encoding='utf8'))
for rec in p['records']:
    assert rec['license'] in ('MIT','CC BY-SA 4.0')
    assert hashlib.sha256((a/rec['file']).read_bytes()).hexdigest()==rec['sha256'],rec['file']
    assert hashlib.sha256((r/rec['source']).read_bytes()).hexdigest()==rec['sourceSha256'],rec['source']
    if rec.get('aliasOf'):assert (a/rec['file']).read_bytes()==(a/rec['aliasOf']).read_bytes()
documented={rec['file'] for rec in p['records']}
assert {'symbols/'+f.name for f in (a/'symbols').iterdir()}==documented
board=json.loads((a/'default.json').read_text(encoding='utf8'))
buttons=[b for profile in board['profiles'] for page in profile['pages'] for b in page['buttons']]
assert len(buttons)==166
for b in buttons:assert b['symbol'] in documented,b['label']
manifest=ET.parse(r/'app/src/main/AndroidManifest.xml').getroot()
assert not manifest.findall('uses-permission')
assert manifest.attrib['{http://schemas.android.com/apk/res/android}versionName']=='0.1.1'
assert (r/'LICENSE').is_file() and (r/'THIRD_PARTY_NOTICES.md').is_file()
print(f'Asset checks passed: {len(buttons)} buttons, {len(documented)} PNGs, attribution and source hashes, no app permissions')
