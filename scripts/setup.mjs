import {existsSync,writeFileSync} from 'node:fs';
import {randomBytes} from 'node:crypto';
if(!existsSync('.env')) {
 const values=Object.fromEntries(['DB_PASSWORD','DEMO_DB_PASSWORD','SANDBOX_DB_PASSWORD','NEXUS_PASSWORD','OBSERVER_PASSWORD','DEMO_TOKEN'].map(k=>[k,randomBytes(24).toString('hex')]));
 writeFileSync('.env',Object.entries({...values,NEXUS_USER:'developer',AI_ENABLED:'false'}).map(([k,v])=>`${k}=${v}`).join('\n')+'\n',{mode:0o600});
}
