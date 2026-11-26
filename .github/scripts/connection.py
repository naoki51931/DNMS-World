import json, os, sys, urllib.request, urllib.error, uuid
base = 'http://127.0.0.1:8080'
auth = {'Authorization': 'Bearer '+os.environ['DNMS_WORLD_TOKEN']}
def req(path='/api/v1/npcs', body=None, headers=auth, status=200):
    r=urllib.request.Request(base+path, data=None if body is None else body.encode(), headers=headers)
    try:
        with urllib.request.urlopen(r, timeout=10) as response:
            code, data=response.status,response.read()
    except urllib.error.HTTPError as e:
        code,data=e.code,e.read()
    assert code==status,(path,code,status,data)
    return json.loads(data)
world_id=req('/api/v1/world')['worldId']
base_auth=dict(auth)
req(headers=base_auth,status=409)
req(headers={**base_auth,'X-DNMS-World':str(uuid.UUID(int=0))},status=409)
auth['X-DNMS-World']=world_id
def spawn(i, status=201, pos=None):
    ident=str(uuid.UUID(int=i+1000))
    x,y,z=pos or ((i%16)*12+4,1,(i//16)*12+4)
    return req(body=f'{ident},{x},{y},{z}',status=status)
if sys.argv[1]=='fill':
    req(headers={},status=401)
    req(headers={'Authorization':'Bearer wrong'},status=401)
    ns=req()['npcs']
    assert len(ns)==25 and ns[24]['id']=='00000000-0000-4000-8000-000000000024'
    req('/api/control','pause',{'X-DNMS-Control':'dnms-local'})
    for cmd in ('next','reset'):
        req('/api/control',cmd,{'X-DNMS-Control':'dnms-local'},400)
    assert spawn(25,400,(0,0,0))['result']=='INVALID_POSITION'
    assert spawn(25)['result']=='CREATED'
    assert spawn(25,409)['result']=='ALREADY_EXISTS'
    for i in range(26,256): assert spawn(i)['result']=='CREATED'
    assert spawn(256,409)['result']=='CAPACITY'
    ns=req()['npcs']
    assert len(ns)==256 and len({n['id'] for n in ns})==256
    with open('../expected-ids.json','w') as f: json.dump(sorted(n['id'] for n in ns),f)
    req('/api/control','checkpoint',{'X-DNMS-Control':'dnms-local'})
else:
    with open('../expected-ids.json') as f: expected=json.load(f)
    assert sorted(n['id'] for n in req()['npcs'])==expected
print('PASS',sys.argv[1])
