-- REVIEWED DEPLOYMENT DRAFT — not applied by the Android change.
-- Prerequisites before production:
-- 1. Back up and test on a staging clone, including Auth table/version compatibility.
-- 2. Deploy employee-account with server-only SUPABASE_SERVICE_ROLE_KEY.
-- 3. REMOVE/secure legacy create_user_admin and update_user_admin BEFORE enabling
--    this client. Their service-role calls bypass grants/RLS and may permit role,
--    email, password or activation changes. New endpoints alone do not close this.
-- 4. Review existing profile/role/store mutation RPCs and grants: an active user
--    must not elevate their role or change their own store membership. Audit all
--    SECURITY DEFINER functions and exposed views; this is not a full RLS audit.
-- 5. Merge the pre-request hook below with any existing hook; do not replace one
--    silently. For self-hosted PostgREST use PGRST_DB_PRE_REQUEST if DB config is
--    disabled. Confirm stale JWT is denied on BOTH tables and SECURITY DEFINER RPCs.
-- 6. The hook covers PostgREST only. Storage/Realtime/other Edge functions must
--    enforce profiles.is_active separately; existing access JWTs remain signed.
--    Auth ban/session revocation prevents refresh but is not JWT invalidation.
-- 7. Exercise Owner/Manager/Staff matrix, cross-store attacks, parallel disable,
--    orphan Auth cleanup, ban retries, and credential changes before rollout.
-- 8. Verify auth.sessions(id uuid, user_id uuid) and JWT session_id are present
--    in this Auth version. Legacy JWTs without session_id fail closed and need
--    a fresh sign-in. Verify auth.refresh_tokens.user_id varchar compatibility.

begin;

create or replace function public.current_account_access()
returns jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object('active', exists (
    select 1 from public.profiles p where p.id = auth.uid() and p.is_active = true
    and exists (select 1 from auth.sessions s where s.user_id = p.id
      and s.id::text = auth.jwt()->>'session_id')
  ));
$$;
revoke all on function public.current_account_access() from public, anon;
grant execute on function public.current_account_access() to authenticated;

create or replace function public.require_active_account()
returns void language plpgsql security definer set search_path = '' as $$
declare
  claims jsonb := coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb;
  request_path text := current_setting('request.path', true);
begin
  -- service-role requests are backend-only and must authorize their own caller.
  if claims->>'role' = 'service_role' then return; end if;
  -- Anonymous/public endpoints retain their existing grants/RLS.
  if claims->>'role' is distinct from 'authenticated' then return; end if;
  if request_path = '/rpc/current_account_access' then return; end if;
  if not exists(select 1 from public.profiles p where p.id = auth.uid() and p.is_active = true
    and exists(select 1 from auth.sessions s where s.user_id = p.id
      and s.id::text = claims->>'session_id')) then
    raise sqlstate '42501' using message = 'ACCOUNT_INACTIVE';
  end if;
end;
$$;
revoke all on function public.require_active_account() from public;
grant execute on function public.require_active_account() to anon, authenticated, service_role;

-- Deliberately explicit rollout command: review existing authenticator settings
-- first; uncomment only after preserving any existing pre-request validation.
-- alter role authenticator set pgrst.db_pre_request = 'public.require_active_account';
-- notify pgrst, 'reload config';

create or replace function public.disable_employee_account(p_actor uuid, p_target uuid)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
  actor_role text;
  target_role text;
begin
  -- This function is service-role-only; caller UUID comes from Auth getUser in
  -- the Edge function. Never grant it directly to authenticated users.
  if coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb->>'role' is distinct from 'service_role' then
    raise sqlstate '42501' using message = 'SERVER_ONLY';
  end if;
  if p_actor = p_target then raise sqlstate '42501' using message = 'CANNOT_DISABLE_SELF'; end if;
  -- Serialize the owner-count check and disable transaction across this entry.
  perform pg_advisory_xact_lock(1936028271, 1);
  perform 1 from public.profiles where id in (p_actor, p_target) order by id for update;
  select lower(r.role_name) into actor_role from public.profiles p
    join public.roles r on r.id = p.role_id where p.id = p_actor and p.is_active = true;
  select lower(r.role_name) into target_role from public.profiles p
    join public.roles r on r.id = p.role_id where p.id = p_target;
  if target_role is null then raise exception 'ACCOUNT_NOT_FOUND'; end if;
  if actor_role = 'owner' then
    if target_role = 'owner' and not exists (
      select 1 from public.profiles p join public.roles r on r.id = p.role_id
      where p.is_active = true and lower(r.role_name) = 'owner' and p.id <> p_target
    ) then raise sqlstate '42501' using message = 'LAST_OWNER'; end if;
  elsif actor_role = 'manager' and target_role in ('staff', 'employee') and exists (
    select 1 from public.user_stores a join public.user_stores t on t.store_id = a.store_id
    where a.user_id = p_actor and t.user_id = p_target
  ) then null;
  else raise sqlstate '42501' using message = 'DISABLE_FORBIDDEN';
  end if;
  update public.profiles set is_active = false where id = p_target;
  -- auth.refresh_tokens.user_id is varchar in the standard Supabase Auth schema.
  update auth.refresh_tokens set revoked = true where user_id = p_target::text;
  delete from auth.sessions where user_id = p_target;
  return true;
end;
$$;
revoke all on function public.disable_employee_account(uuid, uuid) from public, anon, authenticated;
grant execute on function public.disable_employee_account(uuid, uuid) to service_role;

-- Both legacy and newer register overloads lack caller authorization. Remove
-- direct client access; the reviewed Edge function invokes via service_role.
do $$
declare fn regprocedure;
begin
  for fn in select p.oid::regprocedure from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = 'register_new_employee'
  loop
    execute format('revoke execute on function %s from public, anon, authenticated', fn);
    execute format('grant execute on function %s to service_role', fn);
  end loop;
end;
$$;

notify pgrst, 'reload schema';
commit;
