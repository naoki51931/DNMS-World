const CACHE='dnms-preview-v1';
self.addEventListener('install',e=>e.waitUntil(caches.open(CACHE).then(c=>c.addAll(['./','./manifest.webmanifest','./icon.svg']))));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('dnms-preview-')&&k!==CACHE).map(k=>caches.delete(k))))));
self.addEventListener('fetch',e=>{
 const r=e.request,u=new URL(r.url);
 if(r.method!=='GET'||u.origin!==self.location.origin||!u.href.startsWith(self.registration.scope)||r.headers.has('authorization'))return;
 const relative=u.href.slice(self.registration.scope.length).split('?')[0];
 if(!(r.mode==='navigate'&&(relative===''||relative==='index.html')||relative.startsWith('assets/')||['terrain.json','manifest.webmanifest','icon.svg'].includes(relative)))return;
 e.respondWith(fetch(r).then(async response=>{if(response.ok){const c=await caches.open(CACHE);await c.put(r,response.clone());}return response;}).catch(async()=>{const hit=await caches.match(r);if(hit)return hit;if(r.mode==='navigate'){const home=await caches.match(new URL('./',self.registration.scope));if(home)return home;}return new Response('Offline',{status:503});}));
});