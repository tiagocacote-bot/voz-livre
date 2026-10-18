"""Print release notes for a version: its CHANGELOG section plus the download link."""
from pathlib import Path
import re,sys
v=sys.argv[1]
text=(Path(__file__).resolve().parents[1]/'CHANGELOG.md').read_text(encoding='utf8')
m=re.search(r'^## '+re.escape(v)+r'\b.*?$(.*?)(?=^## |\Z)',text,re.M|re.S)
body=m.group(1).strip() if m else ''
print(f"""**Baixar / Download:** [Voz-Livre.apk](https://github.com/tiagocacote-bot/voz-livre/releases/latest/download/Voz-Livre.apk) (Android 8+)

{body}

Atualiza a versão anterior sem perder dados (mesma assinatura). Faça um backup em **Ajustes** antes de atualizar.
Updates the previous version in place (same signing key). Export a backup from **Ajustes** first.
""")
