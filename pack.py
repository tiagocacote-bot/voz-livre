from pathlib import Path
import zipfile,struct
root=Path(__file__).resolve().parent
out=root/'build'/'unsigned.apk'
with zipfile.ZipFile(root/'build'/'base.apk') as base,zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=8) as apk:
    def add(name,data,stored=False):
        info=zipfile.ZipInfo(name,(2026,10,16,0,0,0));info.compress_type=zipfile.ZIP_STORED if stored else zipfile.ZIP_DEFLATED
        if stored:
            offset=apk.fp.tell()+30+len(name.encode('utf-8'))
            if offset%4:
                pad=(-offset-4)%4
                info.extra=struct.pack('<HH',0xd935,pad)+bytes(pad)
        apk.writestr(info,data)
    for f in base.infolist():add(f.filename,base.read(f.filename),f.compress_type==zipfile.ZIP_STORED)
    for f in sorted((root/'build'/'dex').glob('*.dex')):add(f.name,f.read_bytes())
    for f in sorted((root/'app'/'src'/'main'/'assets').rglob('*')):
        if f.is_file():add('assets/'+f.relative_to(root/'app'/'src'/'main'/'assets').as_posix(),f.read_bytes())
print('APK empacotado',out.stat().st_size)
