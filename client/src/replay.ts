import * as THREE from 'three';
import {OrbitControls} from 'three/addons/controls/OrbitControls.js';
type Nao=[number,number,number,number,string,number,number];
type Frame={tick:number,naos:Nao[],running?:boolean,speed?:number};
const live=new URLSearchParams(location.search).get('live')==='1';
const api=(path:string)=>new URL('api/'+path,location.href).href;
const status=document.getElementById('status')!;
async function start(){
 const response=await fetch(live?api('state'):import.meta.env.BASE_URL+'nao-replay.json');
 if(!response.ok)throw Error('Replay '+response.status);
 const data=await response.json();const frames:Frame[]=live?[data]:data;
 let current=frames[0];
 const scene=new THREE.Scene();scene.background=new THREE.Color(0x9ac9ef);
 const camera=new THREE.PerspectiveCamera(55,innerWidth/innerHeight,.1,1000);camera.position.set(95,110,110);
 const renderer=new THREE.WebGLRenderer({antialias:true});renderer.setSize(innerWidth,innerHeight);document.body.appendChild(renderer.domElement);
 const controls=new OrbitControls(camera,renderer.domElement);controls.target.set(94,0,10);controls.update();
 scene.add(new THREE.HemisphereLight(0xffffff,0x666644,2));
 const geo=new THREE.BoxGeometry(1,1,1),wood=new THREE.MeshStandardMaterial({color:0x79502e}),ground=new THREE.MeshStandardMaterial({color:0x80a45c}),skin=new THREE.MeshStandardMaterial({color:0xe8a4bb});
 const actors=frames[0].naos.map(([id,x,y,z])=>{
  const plot=new THREE.Mesh(new THREE.BoxGeometry(8,1,5),ground);plot.position.set(x+1.5,0,z);scene.add(plot);
  const avatar=new THREE.Mesh(new THREE.BoxGeometry(.6,1.8,.6),skin);scene.add(avatar);
  const logs=Array.from({length:8},(_,j)=>{const m=new THREE.Mesh(geo,wood);m.position.set(x+4+Math.floor(j/4),1,z-2+j%4);scene.add(m);return m});
  const posts=Array.from({length:8},(_,j)=>{const m=new THREE.Mesh(geo,wood);m.position.set(x+(j%2?1:-1),1+Math.floor(j/4),z+(j%4>=2?1:-1));scene.add(m);return m});
  const option=document.createElement('option');option.value=String(id);option.textContent='奈央 '+(id+1);document.querySelector('#npc')!.appendChild(option);
  return {avatar,logs,posts};
 });
 let time=0,playing=true;const clock=new THREE.Clock();
 const toggle=document.querySelector<HTMLButtonElement>('#toggle')!;
 toggle.onclick=()=>{playing=!playing;toggle.textContent=playing?'停止':'再生'};
 document.querySelector<HTMLButtonElement>('#reset')!.onclick=()=>{time=0};
 const speed=document.querySelector<HTMLSelectElement>('#speed')!,npc=document.querySelector<HTMLSelectElement>('#npc')!;
 if(live){
  let busy=false;
  const sync=async(cmd?:string)=>{
   if(busy)return;busy=true;
   try{const res=await fetch(api(cmd?'control':'state'),cmd?{method:'POST',headers:{'X-DNMS-Control':'dnms-local'},body:cmd}:{cache:'no-store'});
    if(!res.ok)throw Error(String(res.status));current=await res.json();playing=!!current.running;toggle.textContent=playing?'停止':'再生';speed.value=String(current.speed);
   }catch(e){status.textContent='通信エラー: '+String(e)}finally{busy=false}
  };
  toggle.onclick=()=>{void sync(playing?'pause':'resume')};
  document.querySelector<HTMLButtonElement>('#reset')!.onclick=()=>{void sync('reset')};
  speed.onchange=()=>{void sync(speed.value)};
  setInterval(()=>{void sync()},100);
 }
 npc.onchange=()=>{const id=Number(npc.value);if(id>=0){const p=actors[id].avatar.position;camera.position.copy(p).add(new THREE.Vector3(8,8,8));controls.target.copy(p)}else{camera.position.set(95,110,110);controls.target.set(94,0,10)}controls.update()};
 addEventListener('resize',()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight)});
 renderer.setAnimationLoop(()=>{const dt=Math.min(clock.getDelta(),.1);if(playing)time=Math.min(frames.length-1,time+dt*20*Number(speed.value));const frame=live?current:frames[Math.floor(time)];
 frame.naos.forEach(([id,x,y,z,goal,collected,built])=>{const a=actors[id];a.avatar.position.set(x,y+.4,z);a.logs.forEach((m,j)=>m.visible=j>=collected);a.posts.forEach((m,j)=>m.visible=j<built)});
 const selected=Number(npc.value);if(selected>=0){const p=actors[selected].avatar.position;camera.position.add(p.clone().sub(controls.target));controls.target.copy(p)}
 status.textContent=(live?'ライブ | tick ':'記録再生 | tick ')+frame.tick+' | 完成 '+frame.naos.filter(n=>n[6]===8).length+'/'+actors.length+(selected>=0?' | '+frame.naos[selected][4]:'');controls.update();renderer.render(scene,camera)});
}
start().catch(e=>status.textContent=String(e));
