// Real Compose APIs only. No model credentials or raw telemetry are written to artifacts.
import {readFileSync,mkdirSync,writeFileSync} from 'node:fs';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
const env=Object.fromEntries(readFileSync('.env','utf8').trim().split('\n').map(l=>{let i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
const results=[];mkdirSync('evidence',{recursive:true});
function record(check,data={}){results.push({check,at:new Date().toISOString(),...data});writeFileSync('evidence/acceptance.json',JSON.stringify(results,null,2));console.log(JSON.stringify(results.at(-1)));}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,limit=120){let error;const deadline=Date.now()+limit*2000;for(let n=0;n<limit&&Date.now()<deadline;n++){try{return await fn();}catch(e){error=e;await sleep(2000);}}throw error;}
function session(){const cookies=new Map();let csrf;
 return async function req(path,{method='GET',body,form=false,status=200,noCsrf=false}={}) {
 const headers={cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; ')};
 if(body!==undefined){headers['content-type']=form?'application/x-www-form-urlencoded':'application/json';if(!noCsrf)headers['x-xsrf-token']=csrf;}
 const r=await fetch('http://127.0.0.1:3000/api'+path,{method,headers,body:body===undefined?undefined:form?new URLSearchParams(body):JSON.stringify(body),redirect:'manual',signal:AbortSignal.timeout(120000)});
 for(const c of r.headers.getSetCookie()){const p=c.split(';')[0],i=p.indexOf('=');cookies.set(p.slice(0,i),p.slice(i+1));}
 if(status==='reject'){assert(r.status>=400&&r.status<500,`${path}: expected rejection, got ${r.status}`);return r.status;}
 assert.equal(r.status,status,`${path}: HTTP ${r.status}`);const data=r.status===204?null:await r.json();if(path==='/csrf')csrf=data.token;return data;
 };}
const dev=session(),observer=session(),anon=session();
try {
 await until(()=>dev('/csrf'));await anon('/services',{status:'reject'});
 for(const [req,user,password] of [[dev,'developer',env.NEXUS_PASSWORD],[observer,'observer',env.OBSERVER_PASSWORD]]){
 await req('/csrf');await req('/login',{method:'POST',form:true,body:{username:user,password},status:204});await req('/csrf');await req('/me');}
 await observer('/scan',{method:'POST',body:{},status:'reject'});await dev('/scan',{method:'POST',body:{},noCsrf:true,status:'reject'});record('authentication-role-csrf');
 await until(async()=>{await dev('/scan',{method:'POST',body:{}});assert((await dev('/services')).every(s=>s.snapshot?.healthy&&s.snapshot.anomalies.length===0));return true;},60);
 for(const old of await dev('/incidents'))if(!['RESOLVED','CLOSED'].includes(old.status))await dev(`/incidents/${old.id}/close`,{method:'POST',body:{reason:'Disposable cloud startup baseline: all service observations are now healthy'}});
 record('healthy-startup-baseline');
 for(const mode of ['ERROR','LATENCY','DB_POOL']){
 const start=Date.now();await dev('/demo/fault',{method:'POST',body:{service:'inventory',mode}});
 await dev('/scan',{method:'POST',body:{}});await dev('/scan',{method:'POST',body:{}});
 if(mode==='DB_POOL'){const snapshot=(await dev('/services')).find(s=>s.id==='inventory').snapshot;assert(snapshot.dbConnections>=3);assert.equal(snapshot.evidence.find(e=>e.source==='state:inventory').data.payload.heldConnections,3);}
 let i=(await dev('/incidents')).find(i=>i.service==='inventory'&&!['RESOLVED','CLOSED'].includes(i.status));assert(i);assert.equal(i.diagnosis.rootService,'inventory');
 await dev(`/incidents/${i.id}/approve`,{method:'POST',body:{version:i.version,confirmed:true},status:'reject'});
 i=await dev(`/incidents/${i.id}/validate`,{method:'POST',body:{}});assert.equal(i.validation.passed,true);
 await observer(`/incidents/${i.id}/approve`,{method:'POST',body:{version:i.version,confirmed:true},status:'reject'});
 await dev(`/incidents/${i.id}/approve`,{method:'POST',body:{version:i.version,confirmed:false},status:'reject'});
 await dev(`/incidents/${i.id}/approve`,{method:'POST',body:{version:i.version-1,confirmed:true},status:'reject'});
 const version=i.version,recoveryStart=Date.now();i=await dev(`/incidents/${i.id}/approve`,{method:'POST',body:{version,confirmed:true}});assert.equal(i.status,'RESOLVED');assert.equal(i.outcome.success,true);
 await dev(`/incidents/${i.id}/approve`,{method:'POST',body:{version,confirmed:true},status:'reject'});
 await dev('/scan',{method:'POST',body:{}});assert((await dev('/services')).every(s=>s.snapshot?.healthy));
 assert((await dev('/memory?q=inventory')).length>0);assert((await dev(`/incidents/${i.id}/audit`)).length>0);
 record('recovery-'+mode,{incidentId:i.id,durationMs:Date.now()-start,detectionMs:new Date(i.openedAt).getTime()-start,recoveryMs:Date.now()-recoveryStart,correctRoot:true,success:true});
 await until(async()=>{await dev('/scan',{method:'POST',body:{}});assert((await dev('/services')).every(s=>s.snapshot?.healthy&&s.snapshot.anomalies.length===0));return true;},45);
 for(const old of await dev('/incidents'))if(!['RESOLVED','CLOSED'].includes(old.status))await dev(`/incidents/${old.id}/close`,{method:'POST',body:{reason:'Cloud acceptance: dependency healthy after inventory recovery'}});
 }
 for(const gate of ['expired-validation','changed-live-state']){
 await dev('/demo/fault',{method:'POST',body:{service:'inventory',mode:'ERROR'}});
 await dev('/scan',{method:'POST',body:{}});await dev('/scan',{method:'POST',body:{}});
 let i=(await dev('/incidents')).find(i=>i.service==='inventory'&&!['RESOLVED','CLOSED'].includes(i.status));assert(i);
 i=await dev(`/incidents/${i.id}/validate`,{method:'POST',body:{}});assert.equal(i.validation.passed,true);
 if(gate==='expired-validation'){
  assert.match(i.id,/^[0-9a-f-]{36}$/);
  // Controlled test fixture: age only the sandbox-validation timestamp in the disposable database.
  execFileSync('docker',['compose','exec','-T','postgres','psql','-U','nexus','-d','nexus','-v','ON_ERROR_STOP=1','-c',`update incident set validation=jsonb_set(validation,'{at}',to_jsonb(to_char((now() at time zone 'UTC')-interval '6 minutes','YYYY-MM-DD"T"HH24:MI:SS"Z"'))) where id='${i.id}'`],{stdio:'pipe'});
  await dev(`/incidents/${i.id}/approve`,{method:'POST',body:{version:i.version,confirmed:true},status:'reject'});
  assert.equal((await dev(`/incidents/${i.id}`)).status,'AWAITING_APPROVAL');
 }else{
  await dev('/demo/fault',{method:'POST',body:{service:'inventory',mode:'LATENCY'}});
  i=await dev(`/incidents/${i.id}/approve`,{method:'POST',body:{version:i.version,confirmed:true}});
  assert.equal(i.status,'RECOVERY_FAILED');assert.equal(i.outcome.success,false);assert.equal(i.outcome.beforeMode,'LATENCY');
 }
 // Rejection must leave the fault active; only this explicit test cleanup clears it.
 await dev('/scan',{method:'POST',body:{}});assert.equal((await dev('/services')).find(s=>s.id==='inventory').snapshot.evidence.find(e=>e.source==='state:inventory').data.payload.mode,gate==='changed-live-state'?'LATENCY':'ERROR');
 await dev('/demo/fault',{method:'POST',body:{service:'inventory',mode:'NONE'}});
 await until(async()=>{await dev('/scan',{method:'POST',body:{}});assert((await dev('/services')).every(s=>s.snapshot?.healthy&&s.snapshot.anomalies.length===0));return true;},45);
 for(const old of await dev('/incidents'))if(!['RESOLVED','CLOSED'].includes(old.status))await dev(`/incidents/${old.id}/close`,{method:'POST',body:{reason:'Cloud negative approval test cleanup after verified healthy probes'}});
 record(gate+'-rejected-without-reset');
 }
 await until(async()=>{const r=await fetch('http://127.0.0.1:9090/api/v1/query?query=up');const d=await r.json();assert(d.data.result.filter(x=>x.value[1]==='1').length>=4);return true;},30);record('prometheus-four-live-targets');
 for(const metric of ['http_server_requests_seconds_count','jvm_memory_used_bytes']){const r=await fetch('http://127.0.0.1:9090/api/v1/query?query='+encodeURIComponent(metric+'{application="inventory"}'));const d=await r.json();assert(d.data.result.some(x=>Number(x.value[1])>0));}record('prometheus-real-micrometer-series');
 await until(async()=>{const r=await fetch('http://127.0.0.1:3100/loki/api/v1/query_range?query='+encodeURIComponent('{service_name="inventory"}')+'&since=10m');const d=await r.json();assert(d.data.result.some(x=>x.values.length));return true;},30);record('loki-nonempty-real-logs');
 await until(async()=>{const r=await fetch('http://127.0.0.1:16686/api/traces?service=inventory&limit=5');const d=await r.json();assert(d.data.some(x=>x.spans.length));return true;},30);record('jaeger-nonempty-real-spans');
 record('complete');
}catch(e){record('FAILED',{error:String(e).slice(0,500),cause:e.cause?.code});process.exitCode=1;}
