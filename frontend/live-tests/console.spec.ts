import {test,expect} from '@playwright/test';
import {readFileSync,mkdirSync} from 'node:fs';
const env=Object.fromEntries(readFileSync('../.env','utf8').trim().split('\n').map(l=>{const i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
test('real authenticated console and dependency graph',async({page})=>{
 await page.goto('/');
 await page.getByLabel('Username').fill('developer');
 await page.getByLabel('Password').fill(env.NEXUS_PASSWORD);
 await page.getByRole('button',{name:'Enter control room',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Your systems, in focus.'})).toBeVisible();
 await expect(page.locator('.react-flow__node')).toHaveCount(3);
 await expect(page.locator('.react-flow__edge')).toHaveCount(2);
 await expect(page.getByText('Waiting for backend telemetry',{exact:true})).toHaveCount(0);
 await expect(page.locator('.react-flow__node').getByText('Operational',{exact:true})).toHaveCount(3);
 mkdirSync('../evidence',{recursive:true});
 await page.screenshot({path:'../evidence/live-console.png',fullPage:true});
});
