// One bounded real AI/RAG scenario on the existing disposable Compose database.
// Invoked only by the explicitly opt-in workflow, after deterministic acceptance passes.
import {readFileSync,mkdirSync,writeFileSync} from 'node:fs';
import assert from 'node:assert/strict';
const env=Object.fromEntries(readFileSync('.env','utf8').trim().split('\n').map(l=>{let i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
const cookies=new Map();let csrf;
async function req(path,body,form=false){const headers={cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; ')};if(body!==undefined){headers['content-type']=form?'application/x-www-form-urlencoded':'application/json';headers['x-xsrf-token']=csrf;}
const r=await fetch('http://127.0.0.1:3000/api'+path,{method:body===undefined?'GET':'POST',headers,body:body===undefined?undefined:form?new URLSearchParams(body):JSON.stringify(body),redirect:'manual',signal:AbortSignal.timeout(120000)});for(const c of r.headers.getSetCookie()){const p=c.split(';')[0],i=p.indexOf('=');cookies.set(p.slice(0,i),p.slice(i+1));}assert(r.ok,`${path}: HTTP ${r.status}`);return r.status===204?null:r.json();}
for(let n=0;;n++){try{csrf=(await req('/csrf')).token;break;}catch(e){if(n>=60)throw e;await new Promise(r=>setTimeout(r,2000));}}
await req('/login',{username:'developer',password:env.NEXUS_PASSWORD},true);csrf=(await req('/csrf')).token;
// Existing deterministic memory is embedded using exactly one actual record, through the real model.
// Reindex is deliberately limited to one row by the CI fixture before this script starts.
const indexed=await req('/memory/reindex',{});assert.equal(indexed.indexed,1);
await req('/scan',{});assert((await req('/services')).every(s=>s.snapshot?.healthy&&s.snapshot.anomalies.length===0));
await req('/demo/fault',{service:'orders',mode:'ERROR'});
await req('/scan',{});await req('/scan',{});
let i=(await req('/incidents')).find(i=>i.service==='orders'&&!['RESOLVED','CLOSED'].includes(i.status));assert(i);assert.equal(i.diagnosis.mode,'SPRING_AI_ASSISTED');assert.equal(i.diagnosis.rootService,'orders');assert(i.diagnosis.similarIncidents.some(h=>h.retrieval==='vector'));
const findings=i.diagnosis.findings.filter(f=>f.agent.startsWith('Spring AI '));assert.equal(findings.length,3);
const ids=new Set(i.evidence.filter(e=>e.data.available!==false).map(e=>e.id));assert(findings.every(f=>f.evidenceIds.length>0&&f.evidenceIds.every(id=>ids.has(id))));
i=await req(`/incidents/${i.id}/validate`,{});assert.equal(i.validation.passed,true);i=await req(`/incidents/${i.id}/approve`,{version:i.version,confirmed:true});assert.equal(i.status,'RESOLVED');
const memory=await req('/memory?q=orders%20ERROR');assert(memory.some(h=>h.retrieval==='vector'));
mkdirSync('evidence',{recursive:true});writeFileSync('evidence/ai-rag.json',JSON.stringify({at:new Date().toISOString(),incidentId:i.id,mode:i.diagnosis.mode,citedRoles:findings.length,historicalVectorContext:true,persistedVectorRetrieval:true,verifiedRecovery:i.outcome.success,chatModel:'nvidia/nemotron-3-ultra-550b-a55b:free',embeddingModel:'nvidia/nemotron-3.5-embed-1b:free'},null,2));console.log('One real Spring AI scenario: three cited roles, historical vector context, persisted vector retrieval and verified recovery.');
