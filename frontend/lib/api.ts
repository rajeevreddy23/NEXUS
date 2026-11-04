let csrf:string|null=null;
export async function api<T>(path:string,body?:unknown):Promise<T>{
 if(body!==undefined&&!csrf){const response=await fetch('/api/csrf',{cache:'no-store'});if(!response.ok)throw new Error('Cannot connect to the Java backend');csrf=(await response.json()).token;}
 const response=await fetch('/api'+path,{method:body===undefined?'GET':'POST',headers:body===undefined?{}:{'Content-Type':'application/json','X-XSRF-TOKEN':csrf!},body:body===undefined?undefined:JSON.stringify(body),cache:'no-store'});
 if(!response.ok){if(response.status===401)throw new Error('Please sign in to continue');if(response.status===403){csrf=null;throw new Error('Action denied. A developer account and fresh session are required.');}const error=await response.json().catch(()=>({error:`Request failed (${response.status})`}));throw new Error(error.error||`Request failed (${response.status})`);}
 return response.status===204?undefined as T:response.json();
}
export async function login(username:string,password:string){
 csrf=null;const c=await fetch('/api/csrf',{cache:'no-store'});if(!c.ok)throw new Error('Backend unavailable. The service may be starting; please retry shortly.');csrf=(await c.json()).token;
 const response=await fetch('/api/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded','X-XSRF-TOKEN':csrf!},body:new URLSearchParams({username,password})});csrf=null;
 if(!response.ok)throw new Error(response.status===401?'Incorrect username or password':'Sign-in unavailable. Check the backend connection.');
}
