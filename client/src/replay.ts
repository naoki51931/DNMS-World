import * as THREE from 'three';
import {OrbitControls} from 'three/addons/controls/OrbitControls.js';
type Nao=[number,number,number,number,string,number,number,number?,number?,number?,number?,boolean?];
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
 const createActor=([id]:Nao)=>{
  const x=(id%16)*12+4,z=Math.floor(id/16)*12+4;
  const plot=new THREE.Mesh(new THREE.BoxGeometry(8,1,5),ground);plot.position.set(x+1.5,0,z);scene.add(plot);
  const avatar=new THREE.Mesh(new THREE.BoxGeometry(.6,1.8,.6),skin);scene.add(avatar);
  const logs=Array.from({length:8},(_,j)=>{const m=new THREE.Mesh(geo,wood);m.position.set(x+4+Math.floor(j/4),1,z-2+j%4);scene.add(m);return m});
  const posts=Array.from({length:8},(_,j)=>{const m=new THREE.Mesh(geo,wood);m.position.set(x+(j%2?1:-1),1+Math.floor(j/4),z+(j%4>=2?1:-1));scene.add(m);return m});
  const option=document.createElement('option');option.value=String(id);option.textContent='奈央 '+(id+1);document.querySelector('#npc')!.appendChild(option);
  return {avatar,logs,posts,plot,option};
 };
 const actors=frames[0].naos.map(createActor);
 const physiology=document.createElement('div');
 physiology.style.whiteSpace='pre-line';
 document.querySelector('#panel')!.appendChild(physiology);
 const labels:Record<string,string>={COLLECT:'資材採集',BUILD:'建築',REST:'休息',EAT:'食事',FORAGE:'食料確保',SLEEP:'睡眠'};
 let previousDetails='';
 let time=0,playing=true;const clock=new THREE.Clock();
 const toggle=document.querySelector<HTMLButtonElement>('#toggle')!;
 toggle.onclick=()=>{playing=!playing;toggle.textContent=playing?'停止':'再生'};
 document.querySelector<HTMLButtonElement>('#reset')!.onclick=()=>{time=0};
 const speed=document.querySelector<HTMLSelectElement>('#speed')!,npc=document.querySelector<HTMLSelectElement>('#npc')!;
 if(live){
  let busy=false;let queue=Promise.resolve();let pending=0;
  const error=document.createElement('div');document.querySelector('#panel')!.appendChild(error);
  const next=document.createElement('button');next.textContent='奈央を追加';document.querySelector('#panel')!.appendChild(next);
  const sync=async(cmd?:string)=>{
   busy=true;
   try{const res=await fetch(api(cmd?'control':'state'),cmd?{method:'POST',headers:{'X-DNMS-Control':'dnms-local'},body:cmd}:{cache:'no-store'});
    if(!res.ok)throw Error(String(res.status));current=await res.json();playing=!!current.running;toggle.textContent=playing?'停止':'再生';speed.value=String(current.speed);
   error.textContent='';}catch(e){error.textContent='通信エラー: '+String(e)}finally{busy=false}
  };
  const send=(cmd:string)=>{pending++;queue=queue.then(()=>sync(cmd)).finally(()=>{pending--});};
  const panel=document.querySelector('#panel')!;
  const history=document.createElement('select');panel.appendChild(history);
  const refresh=async()=>{try{const res=await fetch(api('checkpoints'));if(!res.ok)throw Error(String(res.status));const ids:string[]=await res.json();history.replaceChildren();ids.sort((a,b)=>Number(b.split('-')[0])-Number(a.split('-')[0])).forEach(id=>{const o=document.createElement('option');o.value=id;o.textContent='tick '+id.split('-')[0]+' / '+id.slice(-6);history.appendChild(o)});}catch(e){error.textContent=String(e)}};
  const button=(label:string,action:()=>void)=>{const b=document.createElement('button');b.textContent=label;b.onclick=action;panel.appendChild(b)};
  button('保存',()=>{send('checkpoint');void queue.then(refresh)});
  button('履歴更新',()=>{void refresh()});
  button('巻き戻し',()=>{if(history.value&&confirm('世界全体を巻き戻しますか？'))send('rewind:'+history.value)});
  void refresh();
  next.onclick=()=>send('next');
  toggle.onclick=()=>send(playing?'pause':'resume');
  document.querySelector<HTMLButtonElement>('#reset')!.onclick=()=>send('reset');
  speed.onchange=()=>send(speed.value);
  setInterval(()=>{if(!busy&&pending===0){queue=queue.then(()=>sync());}},100);
 }
 npc.onchange=()=>{const id=Number(npc.value);if(id>=0){const p=actors[id].avatar.position;camera.position.copy(p).add(new THREE.Vector3(8,8,8));controls.target.copy(p)}else{camera.position.set(95,110,110);controls.target.set(94,0,10)}controls.update()};
 addEventListener('resize',()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight)});
 renderer.setAnimationLoop(()=>{const dt=Math.min(clock.getDelta(),.1);if(playing)time=Math.min(frames.length-1,time+dt*20*Number(speed.value));const frame=live?current:frames[Math.floor(time)];
 while(actors.length>frame.naos.length){const a=actors.pop()!;scene.remove(a.avatar,a.plot,...a.logs,...a.posts);a.plot.geometry.dispose();a.avatar.geometry.dispose();a.option.remove();}
 while(actors.length<frame.naos.length)actors.push(createActor(frame.naos[actors.length]));
 if(Number(npc.value)>=actors.length||npc.value==='')npc.value='-1';
 frame.naos.forEach(([id,x,y,z,goal,collected,built])=>{const a=actors[id];a.avatar.position.set(x,y+.4,z);a.logs.forEach((m,j)=>m.visible=j>=collected);a.posts.forEach((m,j)=>m.visible=j<built)});
 const selected=Number(npc.value);if(selected>=0){const p=actors[selected].avatar.position;camera.position.add(p.clone().sub(controls.target));controls.target.copy(p)}
 const n=selected>=0?frame.naos[selected]:undefined;
 const details=n&&n[7]!==undefined?
  '奈央 '+(n[0]+1)+' | '+(n[7]<=0?'死亡':n[11]?'睡眠中':labels[n[4]]??n[4])+'\n体力 '+n[7].toFixed(1)+'/20 | 満腹度 '+Math.round((n[8]??0)/240)+'%\n疲労度 '+Math.round((n[10]??0)/240)+'% | 食料 '+n[9]+'食':
  live?'個体を選択すると身体状態を表示します':'記録再生：身体状態データなし';
 if(details!==previousDetails){physiology.textContent=details;previousDetails=details;}
 status.textContent=(live?'ライブ | tick ':'記録再生 | tick ')+frame.tick+' | 完成 '+frame.naos.filter(n=>n[6]===8).length+'/'+actors.length+(selected>=0?' | '+frame.naos[selected][4]:'');controls.update();renderer.render(scene,camera)});
}
start().catch(e=>status.textContent=String(e));
