import {readdirSync,readFileSync,mkdirSync,writeFileSync} from 'node:fs';
import assert from 'node:assert/strict';
const suites=[];
for(const module of ['backend','demo','workload'])for(const file of readdirSync(`${module}/target/surefire-reports`).filter(f=>/^TEST-.*\.xml$/.test(f))){
const text=readFileSync(`${module}/target/surefire-reports/${file}`,'utf8'),tag=text.match(/<testsuite\b[^>]+>/)?.[0];assert(tag);
const attrs=Object.fromEntries([...tag.matchAll(/([\w-]+)="([^"]*)"/g)].map(m=>[m[1],m[2]]));
suites.push({name:attrs.name,tests:Number(attrs.tests),failures:Number(attrs.failures),errors:Number(attrs.errors),skipped:Number(attrs.skipped),seconds:Number(attrs.time)});
}
const db=suites.find(s=>s.name==='edu.nexus.PostgresIntegrationTest');assert(db&&db.tests===1&&db.skipped===0&&db.failures===0&&db.errors===0,'Docker PGvector test must execute, not skip');
const work=suites.find(s=>s.name==='edu.nexus.work.WorkspaceIntegrationTest');assert(work&&work.tests>=4&&work.skipped===0&&work.failures===0&&work.errors===0,'Real workspace PostgreSQL tests must execute, not skip');
mkdirSync('evidence',{recursive:true});writeFileSync('evidence/java.json',JSON.stringify({at:new Date().toISOString(),suites},null,2));console.log('Real PGvector test executed; safe Java test counts recorded.');
