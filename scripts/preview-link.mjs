// Access credentials are uploaded only as a private, short-lived repository artifact, never logged.
import {readFileSync,writeFileSync} from 'node:fs';
import {spawnSync} from 'node:child_process';
const env=Object.fromEntries(readFileSync('.env','utf8').split(/\r?\n/).filter(l=>l.includes('=')).map(l=>{const i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
let url;for(let n=0;n<60;n++){const result=spawnSync('docker',['logs','nexus-preview-tunnel'],{encoding:'utf8',timeout:10000});if(result.status!==0)throw Error('Preview tunnel exited');const logs=result.stdout+result.stderr;url=logs.match(/https:\/\/[a-z0-9-]+\.trycloudflare\.com/)?.[0];if(url)break;await new Promise(r=>setTimeout(r,2000));}
if(!url)throw Error('Temporary tunnel did not provide a preview URL');
let reachable=false;for(let n=0;n<45;n++){try{const r=await fetch(url+'/work',{signal:AbortSignal.timeout(10000)});if(r.ok){reachable=true;break;}}catch{}await new Promise(r=>setTimeout(r,2000));}
if(!reachable)throw Error('HTTPS preview is not reachable');
writeFileSync('preview-access.json',JSON.stringify({url,workspace:url+'/work',controlRoom:url,expiresAt:new Date(Date.now()+30*60*1000).toISOString(),accounts:{workspace:{username:'admin',password:env.WORK_ADMIN_PASSWORD},controlRoom:{username:env.NEXUS_USER||'developer',password:env.NEXUS_PASSWORD}}},null,2),{mode:0o600});
console.log('Temporary HTTPS preview is reachable. Access details are in the private artifact.');
