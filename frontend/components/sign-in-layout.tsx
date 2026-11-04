import type {ReactNode} from 'react';
import {ArrowUpRight,GitBranch} from 'lucide-react';

export function SignInLayout({area,title,description,children,alternateHref,alternateLabel}:{area:string;title:string;description:string;children:ReactNode;alternateHref:string;alternateLabel:string}){
 return <main className="auth-page">
  <header className="auth-header"><a className="product-brand" href="/"><span className="product-mark"><GitBranch size={19}/></span>NEXUS<span className="product-area">{area}</span></a><a className="auth-switch" href={alternateHref}>{alternateLabel}<ArrowUpRight size={15}/></a></header>
  <div className="auth-body"><section className="auth-card"><span className="auth-label">{area}</span><h1>{title}</h1><p className="auth-description">{description}</p>{children}</section><p className="auth-help">First visit taking longer? Free hosting may need up to two minutes to start.</p></div>
  <footer className="auth-footer"><span>NEXUS · Application operations</span><span>Access is limited to your assigned role.</span></footer>
 </main>;
}
