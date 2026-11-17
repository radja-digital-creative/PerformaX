-- Run ONLY in the isolated restored staging database after account_access.sql.
-- Reuses cloned rows; every modification and helper is rolled back.
\set ON_ERROR_STOP on
begin;
set local search_path = public, pg_temp;
select set_config('request.jwt.claims','{"role":"service_role"}',true);
create function pg_temp.must_deny(statement text) returns void language plpgsql as $$
begin
  begin
    execute statement;
  exception when insufficient_privilege then return;
  end;
  raise exception 'Expected permission denial: %',statement;
end;
$$;
create function pg_temp.definer_role_attack(target uuid, new_role integer) returns void
language sql security definer as $$ update public.profiles set role_id=new_role where id=target $$;

do $$
declare
  ids uuid[];
  owner_id uuid;
  manager_id uuid;
  staff_id uuid;
  owner_role integer;
  manager_role integer;
  staff_role integer;
  fixture_store uuid;
  other_store uuid;
  session_id uuid := gen_random_uuid();
  access jsonb;
  fn regprocedure;
begin
  select array_agg(id order by id) into ids from (select id from public.profiles order by id limit 3) p;
  if cardinality(ids)<3 then raise exception 'Fixture requires at least three cloned profiles'; end if;
  owner_id:=ids[1]; manager_id:=ids[2]; staff_id:=ids[3];
  select id into owner_role from public.roles where lower(role_name)='owner' limit 1;
  select id into manager_role from public.roles where lower(role_name)='manager' limit 1;
  select id into staff_role from public.roles where lower(role_name) in ('staff','employee') limit 1;
  if owner_role is null or manager_role is null or staff_role is null then raise exception 'Missing expected roles'; end if;
  select id into fixture_store from public.stores order by id limit 1;
  select id into other_store from public.stores where id<>fixture_store order by id limit 1;
  if other_store is null then raise exception 'Fixture requires two cloned stores'; end if;
  update public.profiles set role_id=owner_role,is_active=true where id=owner_id;
  update public.profiles set role_id=manager_role,is_active=true where id=manager_id;
  update public.profiles set role_id=staff_role,is_active=true where id=staff_id;
  delete from public.user_stores where user_id in(manager_id,staff_id);
  insert into public.user_stores(user_id,store_id,is_primary) values(manager_id,fixture_store,true),(staff_id,fixture_store,true);
  insert into auth.sessions(id,user_id,created_at,updated_at) values(session_id,staff_id,now(),now());

  perform set_config('request.jwt.claims',jsonb_build_object('role','authenticated','sub',staff_id,'session_id',session_id)::text,true);
  select public.current_account_access() into access;
  if access->>'active'<>'true' then raise exception 'Valid session rejected'; end if;
  perform public.require_active_account();
  perform pg_temp.must_deny(format('update public.profiles set role_id=%s where id=%L',owner_role,staff_id));
  perform pg_temp.must_deny(format('select pg_temp.definer_role_attack(%L,%s)',staff_id,owner_role));
  perform pg_temp.must_deny(format('update public.profiles set is_active=false where id=%L',staff_id));
  perform pg_temp.must_deny(format('delete from public.profiles where id=%L',staff_id));
  perform pg_temp.must_deny(format('update public.roles set role_name=''owner'' where id=%s',staff_role));
  perform pg_temp.must_deny(format('insert into public.user_stores(user_id,store_id) values(%L,%L)',staff_id,other_store));
  perform pg_temp.must_deny(format('select public.disable_employee_account(%L,%L)',owner_id,staff_id));
  perform pg_temp.must_deny(format('select public.update_employee_account(%L,%L,''{}'')',owner_id,staff_id));

  perform set_config('request.jwt.claims','{"role":"service_role"}',true);
  perform public.update_employee_account(owner_id,staff_id,jsonb_build_object('p_role_id',owner_role));
  perform public.update_employee_account(owner_id,staff_id,jsonb_build_object('p_role_id',staff_role));
  perform public.update_employee_account(manager_id,staff_id,jsonb_build_object('p_full_name','Account regression fixture'));
  perform pg_temp.must_deny(format('select public.update_employee_account(%L,%L,%L::jsonb)',manager_id,staff_id,jsonb_build_object('p_role_id',owner_role)));
  perform pg_temp.must_deny(format('select public.update_employee_account(%L,%L,%L::jsonb)',manager_id,staff_id,jsonb_build_object('p_stores',jsonb_build_array(jsonb_build_object('store_id',other_store)))));
  perform pg_temp.must_deny(format('select public.update_employee_account(%L,%L,%L::jsonb)',staff_id,staff_id,jsonb_build_object('p_basic_salary',999999)));
  perform public.update_employee_account(staff_id,staff_id,jsonb_build_object('p_full_name','Self fixture'));
  perform pg_temp.must_deny(format('select public.disable_employee_account(%L,%L)',owner_id,owner_id));
  perform pg_temp.must_deny(format('select public.disable_employee_account(%L,%L)',manager_id,owner_id));
  delete from public.user_stores where user_id=staff_id;
  insert into public.user_stores(user_id,store_id,is_primary) values(staff_id,other_store,true);
  perform pg_temp.must_deny(format('select public.disable_employee_account(%L,%L)',manager_id,staff_id));
  delete from public.user_stores where user_id=staff_id;
  insert into public.user_stores(user_id,store_id,is_primary) values(staff_id,fixture_store,true);
  perform public.disable_employee_account(manager_id,staff_id);
  if exists(select 1 from auth.sessions where user_id=staff_id) then raise exception 'Sessions remain after disable'; end if;
  if exists(select 1 from auth.refresh_tokens where user_id::text=staff_id::text and revoked=false) then raise exception 'Refresh tokens remain'; end if;
  if exists(select 1 from public.profiles where id=staff_id and is_active=true) then raise exception 'Profile remains active'; end if;
  -- Reactivating the profile alone must never resurrect its old JWT session.
  update public.profiles set is_active=true where id=staff_id;
  perform set_config('request.jwt.claims',jsonb_build_object('role','authenticated','sub',staff_id,'session_id',session_id)::text,true);
  select public.current_account_access() into access;
  if access->>'active'<>'false' then raise exception 'Revoked JWT accepted'; end if;
  perform pg_temp.must_deny('select public.require_active_account()');
  for fn in select p.oid::regprocedure from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public' and p.proname in('register_new_employee','update_employee_full','update_employee','fn_update_user_admin','disable_employee_account','update_employee_account')
  loop
    if has_function_privilege('authenticated',fn,'execute') or has_function_privilege('anon',fn,'execute') then
      raise exception 'Legacy privilege bypass remains: %',fn;
    end if;
  end loop;
  raise notice 'Account SQL scope, direct/definer bypass, disable and revoked JWT regression cases passed';
end;
$$;
rollback;
