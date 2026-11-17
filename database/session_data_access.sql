-- Session revocation enforcement for authenticated table access, including
-- Realtime's RLS checks. Apply after account_access.sql, as the database owner.
-- This adds only a session requirement: it does not replace existing business
-- authorization policies or grant access to any role/table.
-- Views and SECURITY DEFINER RPCs can bypass table RLS; retain the PostgREST
-- require_active_account hook and checks in every externally callable Edge path.
-- Signed Storage URLs remain usable until their expiry; RLS cannot revoke them.

begin;

-- current_account_access must safely bypass profile RLS to avoid recursion.
do $$
declare helper_owner oid;
begin
  select proowner into helper_owner from pg_proc
    where oid='public.current_account_access()'::regprocedure and prosecdef;
  if helper_owner is null or not exists (
    select 1 from pg_roles r, pg_class c
    where r.oid=helper_owner and c.oid='public.profiles'::regclass
      and (r.rolsuper or r.rolbypassrls or (c.relowner=r.oid and not c.relforcerowsecurity))
  ) then raise exception 'current_account_access owner must bypass profiles RLS'; end if;
end;
$$;

do $$
declare t record;
begin
  for t in
    select c.oid,n.nspname,c.relname,c.relrowsecurity
    from pg_class c join pg_namespace n on n.oid=c.relnamespace
    where c.relkind in ('r','p')
      and (n.nspname='public' or (n.nspname='storage' and c.relname in ('objects','buckets')))
    order by n.nspname,c.relname
  loop
    if not t.relrowsecurity then
      -- Dormant restrictive policies would become effective as a side effect.
      -- Refuse this case rather than silently alter active-user permissions.
      if exists(select 1 from pg_policy p where p.polrelid=t.oid and not p.polpermissive
        and p.polname<>'account_live_session_required') then
        raise exception 'Review dormant restrictive policies before enabling RLS on %.%',t.nspname,t.relname;
      end if;
      -- RLS previously allowed every row subject to SQL grants. Preserve that
      -- baseline for all roles; this policy conveys no new SQL privileges.
      execute format('drop policy if exists account_prior_unrestricted_access on %I.%I',t.nspname,t.relname);
      execute format('create policy account_prior_unrestricted_access on %I.%I as permissive for all to public using (true) with check (true)',t.nspname,t.relname);
    end if;
    execute format('drop policy if exists account_live_session_required on %I.%I',t.nspname,t.relname);
    execute format($policy$
      create policy account_live_session_required on %I.%I
      as restrictive for all to authenticated
      using ((select coalesce((public.current_account_access()->>'active')::boolean,false)))
      with check ((select coalesce((public.current_account_access()->>'active')::boolean,false)))
    $policy$,t.nspname,t.relname);
    -- Storage may be owned by supabase_storage_admin. Policy management is
    -- delegated separately; already-enabled RLS needs no owner-only ALTER.
    if not t.relrowsecurity then
      execute format('alter table %I.%I enable row level security',t.nspname,t.relname);
    end if;
  end loop;
end;
$$;

notify pgrst, 'reload schema';
commit;
