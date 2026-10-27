// Node 24. This exercises real APIs; it does not fabricate telemetry or bypass approval.
import {readFileSync,writeFileSync} from 'node:fs';
import {resolve} from 'node:path';
const env=Object.fromEntries(readFileSync(resolve(import.meta.dirname,'../.env'),'utf8').split(/\r?\n/).filter(l=>l&&!l.startsWith('#')).map(l=>{const n=l.indexOf('=');return [l.slice(0,n),l.slice(n+1)];}));
const base=process.env.NEXUS_URL||'http://127.0.0.1:3000',cookies=new Map();let csrf;
async function request(path,{method='GET',body,form=false}={}){
 const headers={cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; ')};
 if(body!==undefined){headers['content-type']=form?'application/x-www-form-urlencoded':'application/json';headers['x-xsrf-token']=csrf;}
 const res=await fetch(base+'/api'+path,{method,headers,body:body===undefined?undefined:form?new URLSearchParams(body):JSON.stringify(body),redirect:'manual',signal:AbortSignal.timeout(120000)});
 for(const c of res.headers.getSetCookie()){const pair=c.split(';')[0],n=pair.indexOf('=');cookies.set(pair.slice(0,n),pair.slice(n+1));}
 if(!res.ok)throw new Error(`${path}: ${res.status} ${await res.text()}`);return res.status===204?null:res.json();
}
csrf=(await request('/csrf')).token;
await request('/login',{method:'POST',form:true,body:{username:env.NEXUS_USER||'developer',password:env.NEXUS_PASSWORD}});
csrf=(await request('/csrf')).token;
await request('/scan',{method:'POST',body:{}});
const baseline=await request('/services');if(baseline.some(s=>!s.snapshot?.healthy))throw new Error('Baseline is unhealthy. Reset old demo faults and resolve or close old incidents first.');
const injectedAt=Date.now();await request('/demo/fault',{method:'POST',body:{service:'inventory',mode:'ERROR'}});console.log('Injected inventory ERROR; collecting two real observations.');
await request('/scan',{method:'POST',body:{}});await request('/scan',{method:'POST',body:{}});
let incident=(await request('/incidents')).find(i=>i.service==='inventory'&&!['RESOLVED','CLOSED'].includes(i.status));
if(!incident)throw new Error('Detector did not open an inventory incident');
if(incident.diagnosis?.rootService!=='inventory')throw new Error('Root cause target did not match injected fault');
console.log(`Incident ${incident.id}: ${incident.diagnosis.rootCause}`);
incident=await request(`/incidents/${incident.id}/validate`,{method:'POST',body:{}});
if(!incident.validation?.passed)throw new Error('Sandbox validation failed');console.log('Sandbox validation passed.');
if(!process.argv.includes('--approve')){console.log('Stopped before execution. Review this incident in the console and approve recovery there, or explicitly rerun a fresh demo with --approve.');process.exit(0);}
console.log('The --approve flag supplies explicit developer approval for this local demo RESET_FAULT action.');
const recoveryStarted=Date.now();incident=await request(`/incidents/${incident.id}/approve`,{method:'POST',body:{version:incident.version,confirmed:true}});
if(incident.status!=='RESOLVED'||!incident.outcome?.success)throw new Error('Recovery did not verify');
await request('/scan',{method:'POST',body:{}});const after=await request('/services');if(after.some(s=>!s.snapshot?.healthy))throw new Error('Dependency chain has not recovered');
const row={incidentId:incident.id,scenario:'inventory:ERROR',injectedAt:new Date(injectedAt).toISOString(),detectedMs:new Date(incident.openedAt).getTime()-injectedAt,diagnosisAndValidationMs:recoveryStarted-injectedAt,recoveryMs:Date.now()-recoveryStarted,correctRoot:incident.diagnosis.rootService==='inventory',success:incident.outcome.success,mode:incident.diagnosis.mode};
const file=resolve(import.meta.dirname,'../docs/latest-demo-result.json');writeFileSync(file,JSON.stringify(row,null,2));console.log('Verified live recovery. Measurements saved to docs/latest-demo-result.json.');
console.log('Dependent-service incident records remain open for review; close them with notes once their probes pass.');
