import type {Metadata} from 'next';
import './globals.css';
export const metadata: Metadata={title:'NEXUS · Reliability control',description:'Evidence-grounded diagnosis and developer-approved recovery for cloud-native services.'};
export default function Layout({children}:{children:React.ReactNode}){return <html lang="en"><body>{children}</body></html>;}
