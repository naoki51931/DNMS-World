import * as THREE from 'three';
import {benchmarkNpcRendering} from './npc-benchmark';
import {OrbitControls} from 'three/addons/controls/OrbitControls.js';
import {step, type Body} from './physics';
type Block=[number,number,number,string];
const status=document.getElementById('status')!;
async function start(){
 const response=await fetch(import.meta.env.BASE_URL+'terrain.json');
 if(!response.ok) throw Error('terrain.json: '+response.status);
 const blocks:Block[]=await response.json();
 const scene=new THREE.Scene();scene.background=new THREE.Color(0x9ac9ef);
 const camera=new THREE.PerspectiveCamera(60,innerWidth/innerHeight,.1,500);
 camera.position.set(48,35,48);
 const renderer=new THREE.WebGLRenderer({antialias:true});renderer.setPixelRatio(Math.min(devicePixelRatio,2));renderer.setSize(innerWidth,innerHeight);document.body.appendChild(renderer.domElement);
 if(new URLSearchParams(location.search).get('benchmark')==='npc'){
  status.textContent='NPC rendering benchmark running...';
  try{const results=await benchmarkNpcRendering(renderer);status.textContent=JSON.stringify(results);console.table(results);}
  finally{renderer.dispose();}
  return;
 }
 const controls=new OrbitControls(camera,renderer.domElement);controls.target.set(16,2,16);controls.enableDamping=true;controls.update();
 scene.add(new THREE.HemisphereLight(0xffffff,0x596344,2));
 const light=new THREE.DirectionalLight(0xffffff,2);light.position.set(20,40,10);scene.add(light);
 const colors:Record<string,number>={GRASS:0x6b9d43,DIRT:0x90613e,STONE:0x85898b,LOG:0x79502e};
 const geometry=new THREE.BoxGeometry(1,1,1);const matrix=new THREE.Matrix4();
 for(const [kind,color] of Object.entries(colors)){
  const group=blocks.filter(b=>b[3]===kind);if(!group.length)continue;
  const mesh=new THREE.InstancedMesh(geometry,new THREE.MeshStandardMaterial({color}),group.length);
  group.forEach(([x,y,z],i)=>mesh.setMatrixAt(i,matrix.makeTranslation(x,y,z)));mesh.instanceMatrix.needsUpdate=true;scene.add(mesh);
 }
 const blockKey=(x:number,y:number,z:number)=>[x,y,z].join(',');
 const occupied=new Set(blocks.map(([x,y,z])=>blockKey(x,y,z)));
 const solid=(x:number,y:number,z:number)=>x<0||z<0||x>=32||z>=32||y<0||occupied.has(blockKey(x,y,z));
 const spawn=Math.max(...blocks.filter(b=>b[0]===16&&b[2]===16).map(b=>b[1]))+.5;
 const body:Body={x:16,y:spawn,z:16,vy:0,grounded:false};
 const avatar=new THREE.Mesh(new THREE.BoxGeometry(.6,1.8,.6),new THREE.MeshStandardMaterial({color:0xe8a4bb}));scene.add(avatar);
 const keys=new Set<string>();const allowed=['KeyW','KeyA','KeyS','KeyD','Space'];
 addEventListener('keydown',e=>{if(allowed.includes(e.code)&&!(e.target instanceof HTMLInputElement)&&!(e.target instanceof HTMLButtonElement)){e.preventDefault();keys.add(e.code)}});
 addEventListener('keyup',e=>keys.delete(e.code));addEventListener('blur',()=>keys.clear());
 const pad=document.createElement('div');pad.style.cssText='position:fixed;bottom:max(12px,env(safe-area-inset-bottom));left:max(8px,env(safe-area-inset-left));display:flex;flex-wrap:wrap;max-width:95vw;gap:4px;touch-action:none';
 for(const [label,key] of [['↑','KeyW'],['←','KeyA'],['↓','KeyS'],['→','KeyD'],['Jump','Space']]){
 const button=document.createElement('button');button.textContent=label;button.style.cssText='padding:12px;min-width:44px;min-height:44px;font-size:18px;touch-action:none';
 button.onpointerdown=e=>{button.setPointerCapture(e.pointerId);keys.add(key)};
 button.onpointerup=button.onpointercancel=button.onlostpointercapture=()=>keys.delete(key);pad.appendChild(button);
 }document.body.appendChild(pad);

 // Lightweight local simulation; no backend requests.
 type LocalNpc={x:number;z:number;angle:number};
 const storageKey='dnms-local-v1';
 let npcs:LocalNpc[]=[], ticks=0;
 const heights=new Map<string,number>();
 for(const [x,y,z] of blocks){const k=x+','+z;heights.set(k,Math.max(heights.get(k)??-1,y));}
 const height=(x:number,z:number)=>(heights.get(Math.round(x)+','+Math.round(z))??-1)+.5;
 const spawnNpc=()=>({x:2+Math.random()*27,z:2+Math.random()*27,angle:Math.random()*Math.PI*2});
 try{const saved=JSON.parse(localStorage.getItem(storageKey)??'null');if(saved&&Array.isArray(saved.npcs)&&saved.npcs.length>=1&&saved.npcs.length<=256&&saved.npcs.every((n:LocalNpc)=>Number.isFinite(n.x)&&Number.isFinite(n.z)&&Number.isFinite(n.angle)&&n.x>=1&&n.x<=30&&n.z>=1&&n.z<=30)&&Number.isSafeInteger(saved.ticks)&&saved.ticks>=0){npcs=saved.npcs;ticks=saved.ticks;}}catch{}
 if(!npcs.length)npcs=Array.from({length:24},spawnNpc);
 const npcMesh=new THREE.InstancedMesh(new THREE.BoxGeometry(.55,1.6,.55),new THREE.MeshStandardMaterial({color:0x607fca}),256);
 npcMesh.frustumCulled=false;scene.add(npcMesh);

 const panel=document.createElement('section');panel.setAttribute('aria-label','NPC管理');
 panel.style.cssText='position:fixed;top:max(72px,env(safe-area-inset-top));right:max(8px,env(safe-area-inset-right));width:min(290px,calc(100vw - 24px));max-height:calc(100dvh - 160px);overflow:auto;box-sizing:border-box;background:#172331;color:white;padding:12px;border:1px solid #506174;border-radius:12px;z-index:10;font:14px system-ui,sans-serif;box-shadow:0 4px 16px #0005';
 const header=document.createElement('div');header.style.cssText='display:flex;align-items:center;justify-content:space-between;gap:8px';
 const heading=document.createElement('strong');heading.textContent='NPC管理 · Non-Server';header.appendChild(heading);panel.appendChild(header);
 const content=document.createElement('div');content.style.cssText='display:grid;gap:8px';panel.appendChild(content);
 const button=(parent:HTMLElement,label:string,fn:()=>void)=>{const b=document.createElement('button');b.type='button';b.textContent=label;b.style.cssText='min-height:44px;padding:6px 10px;border:1px solid #66798a;border-radius:7px;background:#30465a;color:white;font:inherit;cursor:pointer';b.onclick=fn;parent.appendChild(b);return b;};
 const toggle=button(header,'閉じる',()=>{content.hidden=!content.hidden;content.style.display=content.hidden?'none':'grid';toggle.textContent=content.hidden?'開く':'閉じる';toggle.setAttribute('aria-expanded',String(!content.hidden));});toggle.setAttribute('aria-expanded','true');
 const info=document.createElement('div');info.style.cssText='font-size:24px;font-weight:bold';content.appendChild(info);
 const meter=document.createElement('progress');meter.max=256;meter.style.width='100%';meter.setAttribute('aria-label','NPC人数');content.appendChild(meter);
 const detail=document.createElement('div');content.appendChild(detail);
 const label=document.createElement('label');label.textContent='追加する人数';label.htmlFor='npc-add-count';content.appendChild(label);
 const input=document.createElement('input');input.id='npc-add-count';input.type='number';input.inputMode='numeric';input.min='1';input.step='1';input.value='1';input.style.cssText='min-height:44px;width:100%;box-sizing:border-box;font-size:18px';content.appendChild(input);
 const presets=document.createElement('div');presets.style.cssText='display:grid;grid-template-columns:repeat(4,1fr);gap:4px';content.appendChild(presets);
 let saveMessage='',paused=false;
 const save=()=>{try{localStorage.setItem(storageKey,JSON.stringify({npcs,ticks}));saveMessage='保存済み';}catch{saveMessage='保存失敗';}};
 const message=document.createElement('div');message.setAttribute('role','status');
 const presetButtons=[1,8,24,64].map(n=>button(presets,String(n),()=>{input.value=String(Math.min(n,256-npcs.length));refresh();}));
 const add=button(content,'NPCを追加',()=>{const n=Number(input.value);if(!Number.isInteger(n)||n<1||n>256-npcs.length)return;for(let i=0;i<n;i++)npcs.push(spawnNpc());save();drawNpcs();message.textContent=n+'人追加しました';input.value='1';refresh();});add.style.background='#26734e';
 const full=button(content,'最大256人まで増やす',()=>{input.value=String(256-npcs.length);add.click();});
 const tools=document.createElement('div');tools.style.cssText='display:grid;grid-template-columns:1fr 1fr;gap:4px';content.appendChild(tools);
 const pause=button(tools,'一時停止',()=>{paused=!paused;pause.textContent=paused?'再開':'一時停止';refresh();});
 button(tools,'保存',()=>{save();refresh();});
 button(content,'新規世界（24人）',()=>{if(confirm('端末内のNPCと時刻を初期化しますか？')){npcs=Array.from({length:24},spawnNpc);ticks=0;input.value='1';save();drawNpcs();message.textContent='新規世界を作成しました';refresh();}});
 content.appendChild(message);
 const refresh=()=>{const remaining=256-npcs.length;info.textContent=npcs.length+' / 256 人';meter.value=npcs.length;detail.textContent='追加可能 '+remaining+'人 | Day '+(1+Math.floor(ticks/24000))+' '+Math.floor((ticks%24000)/1000)+':'+String(Math.floor((ticks%1000)*60/1000)).padStart(2,'0')+' | '+(paused?'停止中':'稼働中')+' '+saveMessage;input.max=String(remaining);input.disabled=remaining===0;const n=Number(input.value);add.disabled=!(Number.isInteger(n)&&n>=1&&n<=remaining);full.disabled=remaining===0;presetButtons.forEach(b=>b.disabled=remaining===0);add.textContent=remaining===0?'上限に達しました':add.disabled?'人数を確認してください':n+'人追加する';};
 input.addEventListener('input',refresh);document.body.appendChild(panel);refresh();
 const drawNpcs=()=>{npcMesh.count=npcs.length;npcs.forEach((n,i)=>npcMesh.setMatrixAt(i,matrix.makeTranslation(n.x,height(n.x,n.z)+.8,n.z)));npcMesh.instanceMatrix.needsUpdate=true;};
 drawNpcs();
 let elapsed=0;
 const advanceLocal=(dt:number)=>{if(paused||document.hidden)return;elapsed+=Math.min(dt,.25);while(elapsed>=.05){elapsed-=.05;ticks++;for(const n of npcs){if(Math.random()<.025)n.angle+=Math.random()*2-1;const x=n.x+Math.cos(n.angle)*.035,z=n.z+Math.sin(n.angle)*.035;if(x<1||x>30||z<1||z>30||Math.abs(height(x,z)-height(n.x,n.z))>1){n.angle+=Math.PI/2+Math.random();}else{n.x=x;n.z=z;}}if(ticks%200===0)save();}drawNpcs();refresh();};
 addEventListener('pagehide',save);document.addEventListener('visibilitychange',()=>{if(document.hidden)save();});

 const clock=new THREE.Clock();
 status.textContent='DNMS World | '+blocks.length+' blocks | WASD / 矢印ボタン・Spaceでジャンプ';
 addEventListener('resize',()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight)});
 renderer.setAnimationLoop(()=>{const dt=clock.getDelta(); advanceLocal(dt); const forward=new THREE.Vector3();camera.getWorldDirection(forward);forward.y=0;forward.normalize();const right=new THREE.Vector3(-forward.z,0,forward.x);const move=forward.multiplyScalar(Number(keys.has('KeyW'))-Number(keys.has('KeyS'))).addScaledVector(right,Number(keys.has('KeyD'))-Number(keys.has('KeyA'))).normalize().multiplyScalar(4);step(body,solid,move.x,move.z,keys.has('Space'),dt);avatar.position.set(body.x,body.y+.9,body.z);controls.update();renderer.render(scene,camera)});
}
start().catch(e=>{status.textContent='起動失敗: '+String(e)});

if(import.meta.env.PROD && 'serviceWorker' in navigator && window.isSecureContext){
 window.addEventListener('load',()=>navigator.serviceWorker.register(import.meta.env.BASE_URL+'sw.js').catch(console.warn));
}
