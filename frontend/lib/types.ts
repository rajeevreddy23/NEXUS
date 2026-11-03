export type Evidence={id:string;source:string;at:string;data:Record<string,unknown>};
export type Snapshot={service:string;at:string;healthy:boolean;latencyMs:number;cpu:number|null;memoryBytes:number|null;dbConnections:number|null;errorRate:number|null;anomalies:string[];evidence:Evidence[]};
export type Service={id:string;dependencies:string[];snapshot:Snapshot|null};
export type Incident={id:string;service:string;status:string;openedAt:string;updatedAt:string;version:number;approvedBy:string|null;evidence:Evidence[];validation:Record<string,unknown>|null;outcome:Record<string,unknown>|null;diagnosis:{mode:string;rootCause:string;rootService:string;confidence:number;findings:{agent:string;conclusion:string;evidenceIds:string[]}[];similarIncidents:Record<string,unknown>[];plan:{action:string;target:string;rollback:string;risk:string};modelExplanation:string|null}|null};
export type User={username:string;roles:{authority:string}[];demoEnabled:boolean;applicationMode?:boolean};
