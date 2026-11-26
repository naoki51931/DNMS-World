import * as THREE from 'three';
import {OrbitControls} from 'three/addons/controls/OrbitControls.js';
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
 status.textContent='DNMS World | '+blocks.length+' blocks | ドラッグで回転・ピンチで拡大';
 addEventListener('resize',()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight)});
 renderer.setAnimationLoop(()=>{controls.update();renderer.render(scene,camera)});
}
start().catch(e=>{status.textContent='起動失敗: '+String(e)});
