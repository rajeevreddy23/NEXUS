import {cpSync} from 'node:fs';
import {resolve} from 'node:path';
import {pathToFileURL} from 'node:url';
const root=resolve(import.meta.dirname,'..');
cpSync(resolve(root,'.next/static'),resolve(root,'.next/standalone/.next/static'),{recursive:true});
process.env.HOSTNAME=process.env.NEXUS_BIND_HOST||'127.0.0.1';
await import(pathToFileURL(resolve(root,'.next/standalone/server.js')).href);
