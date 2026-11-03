import {test,expect} from '@playwright/test';
import {readFileSync,mkdirSync} from 'node:fs';
const env=Object.fromEntries(readFileSync('../.env','utf8').split(/\r?\n/).filter(l=>l.includes('=')).map(l=>{let i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
test('real workspace saves projects and tasks and reloads persisted work',async({page})=>{
 await page.goto('/work');await page.getByLabel('Username',{exact:true}).fill('admin');await page.getByLabel('Password',{exact:true}).fill(env.WORK_ADMIN_PASSWORD);await page.getByRole('button',{name:'Sign in',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Your projects'})).toBeVisible();await page.locator('summary').filter({hasText:'Create project'}).click();await page.getByLabel('Project name',{exact:true}).fill('Browser release plan');await page.getByLabel('Description',{exact:true}).fill('Created through the real application');await page.getByRole('button',{name:'Create project',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Browser release plan',exact:true})).toBeVisible();await page.getByText('Add task',{exact:true}).click();await page.getByLabel('Title',{exact:true}).fill('Verify browser workflow');await page.getByRole('button',{name:'Save task',exact:true}).click();await expect(page.locator('.work-task').getByText('Verify browser workflow',{exact:true})).toBeVisible();await page.reload();await page.getByRole('button',{name:/Browser release plan/}).click();await expect(page.locator('.work-task').getByText('Verify browser workflow',{exact:true})).toBeVisible();
 mkdirSync('../evidence',{recursive:true});await page.screenshot({path:'../evidence/real-workspace.png',fullPage:true});
});
test('control room monitors actual application without demo controls',async({page})=>{
 await page.goto('/');await page.getByLabel('Username').fill('developer');await page.getByLabel('Password').fill(env.NEXUS_PASSWORD);await page.getByRole('button',{name:'Enter control room',exact:true}).click();await expect(page.locator('.react-flow__node')).toHaveCount(1);await expect(page.getByRole('button',{name:'Demo lab'})).toHaveCount(0);await expect(page.getByRole('link',{name:'Open task workspace ↗'})).toBeVisible();await expect(page.locator('.react-flow__node').getByText('tasks',{exact:true})).toBeVisible();
});

