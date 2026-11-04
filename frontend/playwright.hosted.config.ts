import {defineConfig} from '@playwright/test';
if(!process.env.NEXUS_BASE_URL)throw new Error('Set NEXUS_BASE_URL to the deployed application URL');
export default defineConfig({testDir:'./application-tests',workers:1,retries:0,timeout:120000,expect:{timeout:30000},reporter:'list',use:{baseURL:process.env.NEXUS_BASE_URL,trace:'off',screenshot:'off'}});
