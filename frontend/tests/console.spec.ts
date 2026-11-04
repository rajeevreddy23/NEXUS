import {test,expect,Page} from '@playwright/test';
// Explicit UI fixtures. These verify presentation and interaction, not live telemetry or backend security.
const id='11111111-1111-1111-1111-111111111111';
async function fixture(page:Page,role='DEVELOPER',withIncident=false){
 let status='AWAITING_APPROVAL';
 const at=new Date().toISOString();
 await page.route('**/api/**',async route=>{
  const url=new URL(route.request().url()),p=url.pathname;
  let data:unknown={};
  if(p==='/api/me')data={username:role==='DEVELOPER'?'developer':'observer',roles:[{authority:`ROLE_${role}`}],demoEnabled:true};
  else if(p==='/api/csrf')data={token:'test-csrf',headerName:'X-XSRF-TOKEN'};
  else if(p==='/api/services')data=['orders','payments','inventory'].map((service,n)=>({id:service,dependencies:n<2?[['payments','inventory'][n]]:[],snapshot:{service,at,healthy:!withIncident,latencyMs:40,cpu:.08,memoryBytes:128000000,dbConnections:0,errorRate:0,anomalies:[],evidence:[]}}));
  else if(p==='/api/incidents')data=withIncident?[{id,service:'inventory',status,openedAt:at,updatedAt:at,version:4,approvedBy:null,evidence:[{id:'evidence-1',source:'state:inventory',at,data:{available:true,payload:{mode:'ERROR'}}}],diagnosis:{mode:'DETERMINISTIC_EVIDENCE',rootCause:'Inventory reports an active ERROR fault.',rootService:'inventory',confidence:.8,findings:[{agent:'Root Cause Analyst',conclusion:'Observed inventory failure.',evidenceIds:['evidence-1']}],similarIncidents:[],plan:{action:'RESET_FAULT',target:'inventory',rollback:'Manual rollback only in isolated demo.',risk:'LOW_DEMO_ONLY'}},validation:{passed:true,at,faultMode:'ERROR'},outcome:null}]:[];
  else if(p.endsWith('/audit'))data=[];
  else if(p.endsWith('/approve')){status='RESOLVED';data={};}
  else if(p==='/api/memory')data=[];
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data)});
 });
}
test('empty overview labels real absence and renders dependency graph',async({page})=>{
 await fixture(page);await page.goto('/');await expect(page.getByRole('heading',{name:'Service overview'})).toBeVisible();
 await expect(page.getByText('No incidents to investigate')).toBeVisible();await expect(page.locator('.react-flow__node')).toHaveCount(3);
 await page.screenshot({path:'test-results/overview-fixture.png',fullPage:true});
});
test('approval requires review checkbox and sends revision plus explicit confirmation',async({page})=>{
 await fixture(page,'DEVELOPER',true);await page.goto('/');await page.getByRole('button',{name:'INC-11111111'}).click();
 await page.getByRole('button',{name:'Evidence eviden ↗'}).click();await expect(page.getByText('"mode": "ERROR"',{exact:false})).toBeVisible();
 await page.getByRole('button',{name:'Review & approve recovery'}).click();const approve=page.getByRole('button',{name:'Approve & execute'});await expect(approve).toBeDisabled();
 await page.getByRole('checkbox').check();const request=page.waitForRequest(r=>r.url().endsWith('/approve'));await approve.click();
 expect((await request).postDataJSON()).toEqual({version:4,confirmed:true});await expect(page.getByText('resolved',{exact:true})).toBeVisible();
});
test('observer cannot use demo mutation controls',async({page})=>{
 await fixture(page,'OBSERVER');await page.goto('/');await page.getByRole('button',{name:'Demo lab'}).click();await expect(page.getByRole('button',{name:'Apply scenario'})).toBeDisabled();await expect(page.getByRole('button',{name:'Collect telemetry now'})).toBeDisabled();
});
test('mobile overview fits viewport',async({page})=>{
 await page.setViewportSize({width:390,height:844});await fixture(page);await page.goto('/');await expect(page.getByRole('heading',{name:'Service overview'})).toBeVisible();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);await page.screenshot({path:'test-results/mobile-fixture.png',fullPage:true});
});
test('unavailable backend shows actionable sign-in error',async({page})=>{
 await page.route('**/api/**',r=>r.fulfill({status:503,contentType:'application/json',body:JSON.stringify({error:'unavailable'})}));await page.goto('/');
 await page.getByLabel('Password',{exact:true}).fill('example-password');await page.getByRole('button',{name:'Enter control room'}).click();await expect(page.locator('.error[role="alert"]')).toContainText('Backend unavailable');
});
