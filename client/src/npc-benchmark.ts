import * as THREE from 'three';

// Synthetic rendering benchmark, not the live NPC client.
export async function benchmarkNpcRendering(renderer: THREE.WebGLRenderer) {
 const scene = new THREE.Scene();
 const camera = new THREE.PerspectiveCamera(60, 1, .1, 500);
 camera.position.set(20, 30, 45); camera.lookAt(0, 0, 0);
 const geometry = new THREE.BoxGeometry(.6, 1.8, .6);
 const material = new THREE.MeshBasicMaterial({color: 0xe8a4bb});
 const matrix = new THREE.Matrix4();
 const results = [];
 try {
  for (const count of [24, 64, 128, 256]) {
   const mesh = new THREE.InstancedMesh(geometry, material, count);
   mesh.frustumCulled = false;
   scene.add(mesh);
   const frames: number[] = [];
   let previous = 0;
   try {
    for (let frame = 0; frame < 150; frame++) {
     const now = await new Promise<number>(resolve => requestAnimationFrame(resolve));
     for (let i = 0; i < count; i++)
      mesh.setMatrixAt(i, matrix.makeTranslation(i % 16 - 8 + Math.sin(frame / 20), 1, Math.floor(i / 16) - 8));
     mesh.instanceMatrix.needsUpdate = true;
     renderer.render(scene, camera);
     if (frame >= 30 && previous) frames.push(now - previous);
     previous = now;
    }
    frames.sort((a,b) => a-b);
    results.push({count, frames: frames.length, p95FrameMs: frames[Math.ceil(frames.length*.95)-1],
     meanFps: 1000 / (frames.reduce((a,b)=>a+b,0)/frames.length), drawCalls: renderer.info.render.calls});
   } finally { scene.remove(mesh); mesh.dispose(); }
  }
  return results;
 } finally { geometry.dispose(); material.dispose(); }
}
