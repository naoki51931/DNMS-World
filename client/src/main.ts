import * as THREE from 'three';
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
 const occupied=new Set(blocks.map(([x,y,z])=>x+','+y+','+z'));
 const solid=(x:number,y:number,z:number)=>x<0||z<0||x>=32||z>=32||y<0||occupied.has(x+','+y+','+z');
 const spawn=Math.max(...blocks.filter(b=>b[0]===16&&b[2]===16).map(b=>b[1]))+.5;
 const body:Body={x:16,y:spawn,z:16,vy:0,grounded:false};
 const avatar=new THREE.Mesh(new THREE.BoxGeometry(.6,1.8,.6),new THREE.MeshStandardMaterial({color:0xe8a4bb}));scene.add(avatar);
 const keys=new Set<string>();const allowed=['KeyW','KeyA','KeyS','KeyD','Space'];
 addEventListener('keydown',e=>{if(allowed.includes(e.code)){e.preventDefault();keys.add(e.code)}});
 addEventListener('keyup',e=>keys.delete(e.code));addEventListener('blur',()=>keys.clear());
 const pad=document.createElement('div');pad.style.cssText='position:fixed;bottom:20px;left:20px;display:flex;gap:8px;touch-action:none';
 for(const [label,key] of [['↑','KeyW'],['←','KeyA'],['↓','KeyS'],['→','KeyD'],['Jump','Space']]){
 const button=document.createElement('button');button.textContent=label;button.style.cssText='padding:16px;font-size:20px;touch-action:none';
 button.onpointerdown=e=>{button.setPointerCapture(e.pointerId);keys.add(key)};
 button.onpointerup=button.onpointercancel=button.onlostpointercapture=()=>keys.delete(key);pad.appendChild(button);
 }document.body.appendChild(pad);
 const clock=new THREE.Clock();
 status.textContent='DNMS World | '+blocks.length+' blocks | WASD / 矢印ボタン・Spaceでジャンプ';
 addEventListener('resize',()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight)});
 renderer.setAnimationLoop(()=>{const dt=clock.getDelta(); const forward=new THREE.Vector3();camera.getWorldDirection(forward);forward.y=0;forward.normalize();const right=new THREE.Vector3(-forward.z,0,forward.x);const move=forward.multiplyScalar(Number(keys.has('KeyW'))-Number(keys.has('KeyS'))).addScaledVector(right,Number(keys.has('KeyD'))-Number(keys.has('KeyA'))).normalize().multiplyScalar(4);step(body,solid,move.x,move.z,keys.has('Space'),dt);avatar.position.set(body.x,body.y+.9,body.z);controls.update();renderer.render(scene,camera)});
}
start().catch(e=>{status.textContent='起動失敗: '+String(e)});
