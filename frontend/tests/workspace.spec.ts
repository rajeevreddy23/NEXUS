import {test,expect} from '@playwright/test';

test('workspace waits for task data, filters projects, and fits a mobile screen',async({page})=>{
 let releaseTasks!:()=>void;
 const pending=new Promise<void>(resolve=>{releaseTasks=resolve;});
 await page.route('**/work-api/**',async route=>{
  const path=new URL(route.request().url()).pathname;
  let data:unknown={};
  if(path.endsWith('/me'))data={id:'member-1',username:'member',role:'MEMBER'};
  else if(path==='/work-api/projects')data=[{id:'project-1',name:'Release checklist',description:'Release preparation',version:0,archived:false,owner_id:'member-1',task_count:1}];
  else if(path.endsWith('/tasks')){await pending;data={total:1,items:[{id:'task-1',title:'Review acceptance criteria',description:'Check the release',status:'TODO',priority:'MEDIUM',assignee_id:null,due_date:null,version:0}]};}
  else if(path.endsWith('/members'))data=[{id:'member-1',username:'member'}];
  else if(path.endsWith('/audit'))data=[];
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data)});
 });
 await page.goto('/work');
 await page.getByRole('button',{name:'Release checklist 1 tasks'}).click();
 try{
  await expect(page.getByRole('status')).toContainText('Loading workspace data');
  await expect(page.getByText('No tasks yet',{exact:true})).toHaveCount(0);
 }finally{releaseTasks();}
 await expect(page.getByText('Review acceptance criteria',{exact:true})).toBeVisible();
 await page.getByRole('textbox',{name:'Search projects'}).fill('missing project');
 await expect(page.getByText('No projects match your search.')).toBeVisible();
 await page.getByRole('textbox',{name:'Search projects'}).fill('release');
 await expect(page.locator('.work-project')).toHaveCount(1);
 await page.setViewportSize({width:390,height:844});
 expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
});
