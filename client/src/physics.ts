export type Body={x:number,y:number,z:number,vy:number,grounded:boolean};
export type Solid=(x:number,y:number,z:number)=>boolean;
export function collides(b:Body,solid:Solid){
 for(let x=Math.floor(b.x-.3+.5);x<=Math.floor(b.x+.3+.5-1e-7);x++)
 for(let y=Math.floor(b.y+.5);y<=Math.floor(b.y+1.8+.5-1e-7);y++)
 for(let z=Math.floor(b.z-.3+.5);z<=Math.floor(b.z+.3+.5-1e-7);z++)
 if(solid(x,y,z))return true;
 return false;
}
export function step(b:Body,solid:Solid,dx:number,dz:number,jump:boolean,dt:number){
 dt=Math.max(0,Math.min(dt,.05));
 if(jump&&b.grounded)b.vy=7;
 b.vy=Math.max(-20,b.vy-20*dt);b.grounded=false;
 const n=Math.max(1,Math.ceil(Math.max(Math.abs(dx*dt),Math.abs(dz*dt),Math.abs(b.vy*dt))/.05));
 for(let i=0;i<n;i++){
 for(const [axis,delta] of [['x',dx*dt/n],['z',dz*dt/n],['y',b.vy*dt/n]] as const){
  b[axis]+=delta;
  if(collides(b,solid)){b[axis]-=delta;if(axis==='y'){if(delta<0)b.grounded=true;b.vy=0;}}
 }
 }
}
