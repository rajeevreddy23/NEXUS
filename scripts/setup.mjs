import {existsSync,writeFileSync,readFileSync,appendFileSync} from 'node:fs';
import {randomBytes} from 'node:crypto';
if(!existsSync('.env')) {
 const values=Object.fromEntries(['DB_PASSWORD','DEMO_DB_PASSWORD','SANDBOX_DB_PASSWORD','NEXUS_PASSWORD','OBSERVER_PASSWORD','DEMO_TOKEN'].map(k=>[k,randomBytes(24).toString('hex')]));
 writeFileSync('.env',Object.entries({...values,NEXUS_USER:'developer',AI_ENABLED:'false'}).map(([k,v])=>`${k}=${v}`).join('\n')+'\n',{mode:0o600});
}
// Add new application credentials without replacing any existing secret or configuration.
const current=readFileSync('.env','utf8');
for(const key of ['WORK_DB_PASSWORD','WORK_ADMIN_PASSWORD','OPERATIONS_TOKEN']) {
 if(!new RegExp(`^${key}=`, 'm').test(current))appendFileSync('.env',`\n${key}=${randomBytes(24).toString('hex')}\n`,{mode:0o600});
}
