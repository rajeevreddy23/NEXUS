// Full real application check. All accounts/data created here belong to disposable CI databases.
import {readFileSync,mkdirSync,writeFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import assert from 'node:assert/strict';
const env=Object.fromEntries(readFileSync('.env','utf8').split(/\r?\n/).filter(l=>l.includes('=')).map(l=>{let i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
const results=[];mkdirSync('evidence',{recursive:true});
function record(check){results.push({check,passed:true,at:new Date().toISOString()});writeFileSync('evidence/application-acceptance.json',JSON.stringify(results,null,2));console.log('Passed: '+check);}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,limit=90){let error;for(let n=0;n<limit;n++){try{return await fn();}catch(e){error=e;await sleep(2000);}}throw error;}
function session(prefix){const cookies=new Map();let csrf;
 return async function req(path,body,status=200,form=false,noCsrf=false){
 const headers={cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; ')};
 if(body!==undefined){headers['content-type']=form?'application/x-www-form-urlencoded':'application/json';if(!noCsrf)headers['x-xsrf-token']=csrf;}
 const r=await fetch('http://127.0.0.1:3000'+prefix+path,{method:body===undefined?'GET':'POST',headers,body:body===undefined?undefined:form?new URLSearchParams(body):JSON.stringify(body),redirect:'manual',signal:AbortSignal.timeout(120000)});
 for(const c of r.headers.getSetCookie()){const p=c.split(';')[0],i=p.indexOf('=');cookies.set(p.slice(0,i),p.slice(i+1));}
 if(status==='reject'){assert(r.status>=400&&r.status<500,`${path}: expected rejection, got ${r.status}`);return;}
 assert.equal(r.status,status,`${path}: unexpected response`);const data=r.status===204?null:await r.json();if(path==='/csrf')csrf=data.token;return data;
 };}
async function login(req,username,password){await req('/csrf');await req('/login',{username,password},204,true);await req('/csrf');return req('/me');}
const admin=session('/work-api'),alice=session('/work-api'),bob=session('/work-api'),dev=session('/api'),observer=session('/api'),anon=session('/work-api');
const compose=(...args)=>execFileSync('docker',['compose',...args],{stdio:'pipe',timeout:90000});
try{
 await until(()=>admin('/csrf'));await until(()=>login(admin,'admin',env.WORK_ADMIN_PASSWORD));await anon('/projects',undefined,'reject');
 const password='temporary-ci-password-123';for(const username of ['alice','bob'])await admin('/admin/users',{username,password,role:'MEMBER'});
 await login(alice,'alice',password);await login(bob,'bob',password);await login(dev,'developer',env.NEXUS_PASSWORD);await login(observer,'observer',env.OBSERVER_PASSWORD);
 await alice('/admin/users',undefined,'reject');await alice('/projects',{name:'CSRF rejected'},'reject',false,true);record('real authentication, roles and CSRF');
 const p=await alice('/projects',{name:'Release engineering',description:'Persistent project owned by Alice'});
 await bob(`/projects/${p.id}/tasks`,undefined,404);await bob(`/projects/${p.id}/members`,{username:'bob'},404);await alice(`/projects/${p.id}/members`,{username:'bob'});const members=await alice(`/projects/${p.id}/members`);const bobId=members.find(m=>m.username==='bob').id;
 const task={title:'Prepare release',description:'Review deployment and recovery evidence',status:'TODO',priority:'HIGH',assigneeId:bobId,dueDate:'2026-12-31',version:0};const t=await bob(`/projects/${p.id}/tasks`,task);await bob(`/tasks/${t.id}/update`,{...task,status:'DONE'});await alice(`/tasks/${t.id}/update`,task,409);assert.equal((await alice(`/projects/${p.id}/tasks`)).items[0].status,'DONE');assert((await alice(`/projects/${p.id}/audit`)).length>=4);record('project membership, task assignment, optimistic concurrency and audit');
 compose('restart','tasks');await until(()=>login(alice,'alice',password));await login(admin,'admin',env.WORK_ADMIN_PASSWORD);assert.equal((await alice(`/projects/${p.id}/tasks`)).items[0].status,'DONE');record('business data and accounts survive application restart');
 await until(async()=>{await dev('/scan',{});assert((await dev('/services')).every(s=>s.snapshot?.healthy));});
 for(const i of await dev('/incidents'))if(!['RESOLVED','CLOSED'].includes(i.status))await dev(`/incidents/${i.id}/close`,{reason:'Disposable startup baseline is healthy'});
 const config=await dev('/me');assert.equal(config.applicationMode,true);assert.equal(config.demoEnabled,false);await dev('/demo/fault',{service:'tasks',mode:'ERROR'},'reject');
 await admin('/admin/maintenance',{enabled:true,reason:'Administrator planned maintenance for acceptance'});await alice('/projects',undefined,503);await dev('/scan',{});await dev('/scan',{});
 let i=(await dev('/incidents')).find(x=>x.service==='tasks'&&!['RESOLVED','CLOSED'].includes(x.status));assert(i);assert.equal(i.diagnosis.plan.action,'RESUME_TRAFFIC');await dev(`/incidents/${i.id}/approve`,{version:i.version,confirmed:true},'reject');i=await dev(`/incidents/${i.id}/validate`,{});assert.equal(i.validation.passed,true);await observer(`/incidents/${i.id}/approve`,{version:i.version,confirmed:true},'reject');await dev(`/incidents/${i.id}/approve`,{version:i.version-1,confirmed:true},'reject');
 // An administrator changes the maintenance reason/version after validation. Approval must abort without resuming traffic.
 await admin('/admin/maintenance',{enabled:true,reason:'Maintenance extended after validation'});i=await dev(`/incidents/${i.id}/approve`,{version:i.version,confirmed:true});assert.equal(i.status,'RECOVERY_FAILED');assert.equal((await admin('/admin/operations')).maintenance,true);await dev(`/incidents/${i.id}/close`,{reason:'Expected changed-state rejection in acceptance'});record('approval refuses changed live operational state');
 await dev('/scan',{});await dev('/scan',{});i=(await dev('/incidents')).find(x=>x.service==='tasks'&&!['RESOLVED','CLOSED'].includes(x.status));assert(i);i=await dev(`/incidents/${i.id}/validate`,{});assert.equal(i.validation.passed,true);const version=i.version;i=await dev(`/incidents/${i.id}/approve`,{version,confirmed:true});assert.equal(i.status,'RESOLVED');assert.equal(i.outcome.healthChecksPassed,true);await dev(`/incidents/${i.id}/approve`,{version,confirmed:true},'reject');assert.equal((await admin('/admin/operations')).maintenance,false);assert.equal((await alice(`/projects/${p.id}/tasks`)).items[0].status,'DONE');assert((await dev('/memory?q=tasks')).length>0);record('isolated validation, explicit approval, three live probes and historical memory');
 await until(async()=>{await dev('/scan',{});const s=(await dev('/services'))[0].snapshot;const payload=source=>s.evidence.find(e=>e.source===source)?.data.payload;assert(payload('prometheus:tasks')?.data?.result?.length>0);assert(payload('loki:tasks')?.data?.result?.length>0);assert(payload('jaeger:tasks')?.data?.length>0);});record('nonempty real Prometheus, Loki and Jaeger telemetry');
 await alice(`/projects/${p.id}/update`,{name:p.name,description:p.description,archived:true,version:p.version});await bob(`/projects/${p.id}/tasks`,task,409);record('archived projects reject writes');
}catch(e){console.error(e.message);process.exitCode=1;}
