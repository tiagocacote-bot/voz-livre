// Windows x64 development tools only. Nothing here is bundled into the application.
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
const root=path.dirname(fileURLToPath(import.meta.url));
const destination=path.resolve(root,'.toolchain');
await fs.mkdir(destination,{recursive:true});
const lock=JSON.parse(await fs.readFile(path.join(root,'tools.lock.json'),'utf8'));
for(const item of lock){
 const target=path.join(destination,item.name);
 try{const old=await fs.readFile(target);if(crypto.createHash('sha256').update(old).digest('hex')===item.sha256){console.log('Verificado:',item.name);continue;}}catch{}
 console.log('Baixando:',item.name);
 const response=await fetch(item.url,{signal:AbortSignal.timeout(300000)});
 if(!response.ok)throw Error(`${item.name}: HTTP ${response.status}`);
 const bytes=item.name==='android.jar'?Buffer.from(await response.text(),'base64'):Buffer.from(await response.arrayBuffer());
 if(crypto.createHash('sha256').update(bytes).digest('hex')!==item.sha256)throw Error(`Hash inesperado: ${item.name}. Arquivo não instalado.`);
 await fs.writeFile(target,bytes);
}
// Fixed script; archive paths are passed as arguments, never interpolated into shell code.
const script=path.join(root,'setup-tools.ps1');
execFileSync('powershell.exe',['-NoProfile','-ExecutionPolicy','Bypass','-File',script,'-ToolsDirectory',destination],{stdio:'inherit',windowsHide:true});
console.log('Ferramentas prontas. Execute build.ps1 com Python 3 disponível.');
