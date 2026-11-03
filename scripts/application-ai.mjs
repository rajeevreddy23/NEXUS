// One explicitly enabled provider integration scenario in disposable application infrastructure.
import {readFileSync,mkdirSync,writeFileSync} from 'node:fs';
import assert from 'node:assert/strict';
if(process.env.NEXUS_DISPOSABLE_TESTS!=='true')throw Error('Requires disposable test infrastructure');
const env=Object.fromEntries(readFileSync('.env','utf8').split(/\r?\n/).filter(l=>l.includes('=')).map(l=>{let i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
function session(prefix){const cookies=new Map();let csrf;
 return async function req(path,body,form=false){
 const headers={cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; ')};
 if(body!==undefined){headers['content-type']=form?'application/x-www-form-urlencoded':'application/json';headers['x-xsrf-token']=csrf;}
 const r=await fetch('http://127.0.0.1:3000'+prefix+path,{method:body===undefined?'GET':'POST',headers,body:body===undefined?undefined:form?new URLSearchParams(body):JSON.stringify(body),signal:AbortSignal.timeout(180000)});
 for(const c of r.headers.getSetCookie()){const p=c.split(';')[0],i=p.indexOf('=');cookies.set(p.slice(0,i),p.slice(i+1));}
 assert(r.ok,`${path}: HTTP ${r.status}`);const data=r.status===204?null:await r.json();if(path==='/csrf')csrf=data.token;return data;
 };}
async function login(req,user,password){await req('/csrf');await req('/login',{username:user,password},true);await req('/csrf');await req('/me');}
const dev=session('/api'),admin=session('/work-api');mkdirSync('evidence',{recursive:true});let incident;
try{
 let ready=false;for(let n=0;n<90;n++){try{await login(dev,'developer',env.NEXUS_PASSWORD);ready=true;break;}catch{await new Promise(r=>setTimeout(r,2000));}}assert(ready,'AI-enabled controller failed to start');await login(admin,'admin',env.WORK_ADMIN_PASSWORD);
 for(const old of await dev('/incidents'))if(!['RESOLVED','CLOSED'].includes(old.status))await dev(`/incidents/${old.id}/close`,{reason:'Disposable AI acceptance baseline'});
 // Bound provider work: index just one recorded real maintenance incident as historical context.
 await dev('/memory/reindex',{});await admin('/admin/maintenance',{enabled:true,reason:'One provider-assisted maintenance acceptance trial'});await dev('/scan',{});await dev('/scan',{});
 incident=(await dev('/incidents')).find(x=>x.service==='tasks'&&!['RESOLVED','CLOSED'].includes(x.status));assert(incident);const d=incident.diagnosis;assert.equal(d.plan.action,'RESUME_TRAFFIC');assert.equal(d.mode,'SPRING_AI_ASSISTED','All three provider roles must complete');assert.equal(d.findings.filter(f=>f.agent.startsWith('Spring AI ')&&f.evidenceIds.length>0).length,3);const ids=new Set(incident.evidence.filter(e=>e.data.available!==false).map(e=>e.id));for(const f of d.findings.filter(f=>f.agent.startsWith('Spring AI ')))for(const id of f.evidenceIds)assert(ids.has(id));assert(d.similarIncidents.some(h=>h.retrieval==='vector'),'Real historical vector context required');
 incident=await dev(`/incidents/${incident.id}/validate`,{});assert.equal(incident.validation.passed,true);incident=await dev(`/incidents/${incident.id}/approve`,{version:incident.version,confirmed:true});assert.equal(incident.status,'RESOLVED');const memory=await dev('/memory?q=tasks%20MAINTENANCE');assert(memory.some(h=>h.id===incident.id&&h.retrieval==='vector'),'New real application outcome must be retrievable by vector');
 writeFileSync('evidence/application-ai.json',JSON.stringify({at:new Date().toISOString(),incidentId:incident.id,mode:d.mode,acceptedRoles:3,historicalVectorContext:true,newOutcomeVectorRetrievable:true,recoveryVerified:true,claim:'Citation identity is verified; semantic correctness requires human review'},null,2));console.log('Real application provider, citations, PGvector and recovery passed');
}catch(e){writeFileSync('evidence/application-ai.json',JSON.stringify({at:new Date().toISOString(),passed:false,error:e.message,mode:incident?.diagnosis?.mode,providerFindings:incident?.diagnosis?.findings.filter(f=>f.agent.startsWith('Spring AI ')).map(f=>({agent:f.agent,conclusion:f.conclusion}))},null,2));console.error(e.message);process.exitCode=1;}

