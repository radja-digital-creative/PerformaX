import fs from 'node:fs';
import assert from 'node:assert/strict';
import os from 'node:os';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
// Run with: node --experimental-transform-types tests/test_employee_account.mjs
const actor='11111111-1111-4111-8111-111111111111',target='22222222-2222-4222-8222-222222222222', store='33333333-3333-4333-8333-333333333333';
let state;
const ok=data=>({data,error:null});
globalThis.Deno={env:{get:n=>n}};
globalThis.mockCreateClient=(url,key,opts)=>({
 auth:{getUser:async()=>state.invalid?{data:{user:null},error:{}}:ok({user:{id:actor}}),admin:{
 createUser:async()=>{state.mutations++;return ok({user:{id:target}})},deleteUser:async()=>ok({}),
 updateUserById:async()=>{state.mutations++;return ok({})}, getUserById:async()=>ok({user:{email:'test@example.com'}})}},
 from:table=>({select:()=>({eq:(field,id)=>({maybeSingle:async()=>ok(table==='profiles'?{id,role_id:1,is_active:true}:{role_name:id===1?state.role:'staff'}),then:resolve=>resolve(ok([{store_id:store}]))})})}),
 rpc:async(name,args)=>{state.calls.push({name,args,auth:opts.global?.headers?.Authorization});if(name==='current_account_access')return state.rpcError?{data:null,error:{}}:ok({active:state.active});return ok(true)}
});
let source=fs.readFileSync(new URL('../supabase/functions/_shared/employee-account.ts',import.meta.url),'utf8').replace(/import \{ createClient \} from .*;/,'const createClient = globalThis.mockCreateClient;');
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'employee-account-test-'));
const modulePath=path.join(tmp,'handler.ts');
fs.writeFileSync(modulePath,source);
const {handleEmployeeAccount:run}=await import(pathToFileURL(modulePath).href);
fs.rmSync(tmp,{recursive:true,force:true});
function reset(role='owner'){state={role,active:true,mutations:0,calls:[]};}
async function request(body,action,token='valid'){return run(new Request('http://test',{method:'POST',headers:token?{Authorization:`Bearer ${token}`}:{},body:JSON.stringify(body)}),action);}
let cases=0;
reset();assert.equal((await request({action:'get_email',user_id:actor},undefined,'')).status,401);cases++;
reset();state.invalid=true;assert.equal((await request({action:'get_email',user_id:actor})).status,401);cases++;
reset();state.active=false;assert.equal((await request({action:'update_credentials',user_id:actor,password:'newpassword'})).status,401);assert.equal(state.mutations,0);cases++;
reset();state.rpcError=true;assert.equal((await request({action:'get_email',user_id:actor})).status,503);cases++;
reset('staff');assert.equal((await request({action:'get_email',user_id:actor})).status,200);assert.equal(state.calls[0].auth,'Bearer valid');cases++;
reset('manager');assert.equal((await request({action:'update_credentials',user_id:target,password:'newpassword'})).status,403);assert.equal(state.mutations,0);cases++;
reset('owner');assert.equal((await request({action:'update_credentials',user_id:target,password:'newpassword'})).status,200);cases++;
reset('staff');assert.equal((await request({action:'create',p_role_id:2})).status,403);cases++;
reset('manager');assert.equal((await request({action:'create',p_role_id:1,p_stores:[{store_id:store}]})).status,403);cases++;
reset('manager');assert.equal((await request({action:'create',p_role_id:2,p_stores:[null]})).status,400);cases++;
reset('manager');assert.equal((await request({action:'create',p_role_id:2,p_stores:[{store_id:target}]})).status,403);cases++;
reset('manager');assert.equal((await request({action:'create',p_role_id:2,p_stores:[{store_id:store}],email:'new@example.com',password:'newpassword'})).status,200);cases++;
reset('owner');assert.equal((await request({action:'update_credentials',p_role_id:1,p_stores:[],email:'owner@example.com',password:'newpassword'},'create')).status,200);assert(state.calls.some(c=>c.name==='register_new_employee'));cases++;
reset('manager');assert.equal((await request({p_user_id:target,email:'unauthorized@example.com'},'update_profile')).status,403);assert.equal(state.mutations,0);cases++;
reset('owner');assert.equal((await request({p_user_id:target,p_full_name:'Changed',email:'new@example.com'},'update_profile')).status,200);assert.deepEqual(state.calls.find(c=>c.name==='update_employee_account').args.p_changes,{p_full_name:'Changed'});cases++;
console.log(`${cases} mocked endpoint security/contract cases passed`);
