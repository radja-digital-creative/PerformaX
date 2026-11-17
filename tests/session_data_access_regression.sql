-- Isolated staging only, after account_access.sql and session_data_access.sql.
-- Uses real authenticated role permissions (not a superuser RLS simulation).
-- Every fixture row, profile change, helper grant and session is rolled back.
\set ON_ERROR_STOP on
begin;
set local search_path = public, pg_temp;
select set_config('request.jwt.claims','{"role":"service_role"}',true);

create temporary table account_session_fixture(user_id uuid, session_id uuid);
grant select on account_session_fixture to authenticated;
insert into account_session_fixture(user_id,session_id)
  select id,gen_random_uuid() from public.profiles order by id limit 1;
do $$ begin
  if not exists(select 1 from account_session_fixture) then raise exception 'Requires a cloned profile'; end if;
end $$;
update public.profiles set is_active=true where id=(select user_id from account_session_fixture);
insert into auth.sessions(id,user_id,created_at,updated_at)
  select session_id,user_id,now(),now() from account_session_fixture;

-- Capture a visible location row if available. No device/location is fabricated.
create temporary table account_session_counts(profile_count bigint,location_count bigint);
grant select,insert on account_session_counts to authenticated;
select set_config('request.jwt.claims',jsonb_build_object('role','authenticated','sub',user_id,'session_id',session_id)::text,true)
  from account_session_fixture;
set local role authenticated;
insert into account_session_counts
  select (select count(*) from public.profiles where id=(select user_id from account_session_fixture)),
         (select count(*) from public.user_locations);
do $$ begin
  if (select profile_count from account_session_counts)<>1 then raise exception 'Valid session lost its own profile access'; end if;
  if (select location_count from account_session_counts)=0 then raise exception 'Fixture requires an existing visible cloned user_locations row'; end if;
  if not (public.current_account_access()->>'active')::boolean then raise exception 'Valid session is inactive'; end if;
end $$;
reset role;

-- Revocation must take effect on subsequent table queries with the same JWT.
select set_config('request.jwt.claims','{"role":"service_role"}',true);
delete from auth.sessions where id=(select session_id from account_session_fixture);
select set_config('request.jwt.claims',jsonb_build_object('role','authenticated','sub',user_id,'session_id',session_id)::text,true)
  from account_session_fixture;
set local role authenticated;
do $$ begin
  if exists(select 1 from public.profiles) then raise exception 'Revoked session can read profiles'; end if;
  if exists(select 1 from public.user_locations) then raise exception 'Revoked session can read user_locations'; end if;
  if (public.current_account_access()->>'active')::boolean then raise exception 'Revoked session remains active'; end if;
end $$;
reset role;

-- An inactive account also fails even when its auth.sessions row exists.
select set_config('request.jwt.claims','{"role":"service_role"}',true);
insert into auth.sessions(id,user_id,created_at,updated_at)
  select session_id,user_id,now(),now() from account_session_fixture;
update public.profiles set is_active=false where id=(select user_id from account_session_fixture);
select set_config('request.jwt.claims',jsonb_build_object('role','authenticated','sub',user_id,'session_id',session_id)::text,true)
  from account_session_fixture;
set local role authenticated;
do $$ begin
  if exists(select 1 from public.profiles) then raise exception 'Inactive account can read profiles'; end if;
  if exists(select 1 from public.user_locations) then raise exception 'Inactive account can read user_locations'; end if;
end $$;
reset role;

-- Ensure no existing table was accidentally omitted from the session guard.
do $$ begin
  if exists(
    select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace
    where c.relkind in ('r','p') and
      (n.nspname='public' or (n.nspname='storage' and c.relname in ('objects','buckets')))
      and (not c.relrowsecurity or not exists(select 1 from pg_policy p where p.polrelid=c.oid
        and p.polname='account_live_session_required' and not p.polpermissive))
  ) then raise exception 'Table is missing authenticated session RLS'; end if;
  raise notice 'Authenticated role session RLS checks passed';
end $$;
rollback;
