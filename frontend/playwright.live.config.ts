import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./live-tests',workers:1,retries:0,timeout:60000,reporter:'list',use:{baseURL:'http://127.0.0.1:3000',trace:'off',screenshot:'off'}});
