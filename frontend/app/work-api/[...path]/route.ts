import {NextRequest,NextResponse} from 'next/server';
export const dynamic='force-dynamic';
async function proxy(request:NextRequest,{params}:{params:Promise<{path:string[]}>}){
 const {path}=await params;
 if(path.some(segment=>!/^[-a-zA-Z0-9_]+$/.test(segment)))return NextResponse.json({error:'Invalid path'},{status:400});
 const headers=new Headers();
 for(const key of ['content-type','cookie','x-xsrf-token']){const value=request.headers.get(key);if(value)headers.set(key,value);}
 try {
  const response=await fetch(`${process.env.WORKLOAD_URL||'http://127.0.0.1:8101'}/api/${path.join('/')}${request.nextUrl.search}`,{
   method:request.method,headers,body:request.method==='GET'?undefined:await request.text(),cache:'no-store',redirect:'manual',signal:AbortSignal.timeout(120000)});
  const outgoing=new Headers({'Cache-Control':'no-store'});
  const type=response.headers.get('content-type');if(type)outgoing.set('content-type',type);
  for(const cookie of response.headers.getSetCookie())outgoing.append('set-cookie',cookie);
  return new NextResponse(response.status===204?null:await response.arrayBuffer(),{status:response.status,headers:outgoing});
 }catch{return NextResponse.json({error:'Task application is unavailable. Please retry shortly.'},{status:503});}
}
export {proxy as GET,proxy as POST};
