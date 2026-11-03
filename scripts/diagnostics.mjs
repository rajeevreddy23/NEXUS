import {execFileSync} from 'node:child_process';
import {readFileSync} from 'node:fs';
const secrets=readFileSync('.env','utf8').split('\n').map(l=>l.slice(l.indexOf('=')+1)).filter(v=>v.length>=12);if(process.env.OPENAI_API_KEY)secrets.push(process.env.OPENAI_API_KEY);
function clean(s){for(const value of secrets)s=s.split(value).join('[REDACTED]');return s.split('\n').filter(l=>!/authorization|set-cookie|api.key|password|bearer/i.test(l)).join('\n');}
const target=process.env.COMPOSE_FILE?.includes('application')?'tasks':'inventory';
for(const args of [['compose','ps','-a'],['compose','logs','--no-color','--tail=80','backend',target,'recovery-sandbox','otel-collector','loki']]){
try{const s=execFileSync('docker',args,{encoding:'utf8',maxBuffer:2*1024*1024,stdio:['ignore','pipe','pipe']});console.log(clean(s));}catch{console.log('Diagnostic command unavailable');}}
