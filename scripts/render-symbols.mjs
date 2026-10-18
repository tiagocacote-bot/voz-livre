// Run npm install, then npm run symbols. Existing SVGs are the editable source.
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import sharp from 'sharp';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const assets=path.join(root,'app/src/main/assets');
const provenance=JSON.parse(await fs.readFile(path.join(assets,'symbols-provenance.json'),'utf8'));
const rendered=new Map();
for(const rec of provenance.records){
 if(!rec.source)continue; // ARASAAC PNGs come from scripts/fetch-arasaac.py
 let bytes=rendered.get(rec.source);
 const svg=await fs.readFile(path.join(root,rec.source));
 if(!bytes){bytes=await sharp(svg).resize(256,256,{fit:'contain',background:{r:255,g:255,b:255,alpha:0}}).png().toBuffer();rendered.set(rec.source,bytes);}
 await fs.writeFile(path.join(assets,rec.file),bytes);
 rec.sha256=crypto.createHash('sha256').update(bytes).digest('hex');
 rec.sourceSha256=crypto.createHash('sha256').update(svg).digest('hex');
}
await fs.writeFile(path.join(assets,'symbols-provenance.json'),JSON.stringify(provenance,null,2)+'\n');
console.log(`Rendered ${rendered.size} SVGs. Review changes before committing.`);
