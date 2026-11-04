// Creates ordinary records through the real application API in a new disposable preview.
import {readFileSync} from 'node:fs';
const env=Object.fromEntries(readFileSync('.env','utf8').split(/\r?\n/).filter(l=>l.includes('=')).map(l=>{const i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
const cookies=new Map();let csrf;
async function request(path,body,form=false){
 const headers={cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; ')};
 if(body!==undefined){headers['content-type']=form?'application/x-www-form-urlencoded':'application/json';headers['x-xsrf-token']=csrf;}
 const r=await fetch('http://127.0.0.1:3000/work-api'+path,{method:body===undefined?'GET':'POST',headers,body:body===undefined?undefined:form?new URLSearchParams(body):JSON.stringify(body),signal:AbortSignal.timeout(15000)});
 for(const c of r.headers.getSetCookie()){const p=c.split(';')[0],i=p.indexOf('=');cookies.set(p.slice(0,i),p.slice(i+1));}
 if(!r.ok)throw Error('Preview API HTTP '+r.status);const data=r.status===204?null:await r.json();if(path==='/csrf')csrf=data.token;return data;
}
let ready=false;for(let n=0;n<90;n++){try{await request('/csrf');await request('/login',{username:'admin',password:env.WORK_ADMIN_PASSWORD},true);await request('/csrf');await request('/me');ready=true;break;}catch{await new Promise(r=>setTimeout(r,2000));}}
if(!ready)throw Error('Preview application did not become ready');
const project=await request('/projects',{name:'NEXUS review workspace',description:'These preview records were saved through the real Java application. You can create projects, add tasks, and edit their status.'});
for(const [title,status,priority] of [['Explore the task workspace','TODO','HIGH'],['Inspect live monitoring in the control room','IN_PROGRESS','MEDIUM'],['Verified Java and browser acceptance','DONE','MEDIUM']])await request(`/projects/${project.id}/tasks`,{title,description:'Temporary browser-review data; this is an actual PostgreSQL record.',status,priority,version:0});
console.log('Real preview workspace is ready');
