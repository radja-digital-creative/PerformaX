-- DRAFT ONLY — NOT DEPLOYED. Review against the installed function and RLS first.
-- This draft separates permission to record attendance from approval/KPI exemption.
-- Prerequisite: employee_attendance_preferences migration already installed.
-- REQUIRED BEFORE RELEASE:
-- 1. Review policies: employee may only submit pending requests for themselves;
--    only authorized manager/owner may approve/reject. Never trust client status.
-- 2. Implement transactional approval/rejection reconciliation for existing attendance,
--    KPI and payroll rows. The existing AFTER UPDATE approval trigger only inserts
--    legacy access rows; it does NOT recalculate past attendance or revoke access.
--    This draft does not solve retrospective deduction removal after approval.
-- 3. Verify pending/approved/rejected, cancellation, overlap, dates, shift direction,
--    opening/closing, overnight shifts and existing legacy requests in an isolated DB.
-- 4. Review concurrent approval/rejection versus attendance transactions.
BEGIN;

CREATE OR REPLACE FUNCTION public.get_attendance_exception_access(
    p_user_id uuid, p_schedule_id uuid
) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY INVOKER SET search_path = public
AS $function$
DECLARE
    v_work_date date;
    v_offsite text;
    v_late text;
    v_early text;
    v_has_typed_offsite boolean;
    v_has_typed_shift boolean;
BEGIN
    IF auth.uid() IS NULL OR auth.uid() IS DISTINCT FROM p_user_id THEN
        RAISE EXCEPTION 'Sesi absensi tidak sesuai karyawan.';
    END IF;
    SELECT s.work_date INTO STRICT v_work_date
    FROM public.user_schedules s
    WHERE s.id = p_schedule_id AND s.user_id = p_user_id;

    -- Keep rejected rows in the category-existence checks: they must suppress
    -- stale legacy rows that an earlier approval trigger may have left behind.
    WITH dated AS (
        SELECT l.status::text AS status, t.name, coalesce(t.is_offsite, false) AS offsite,
               coalesce(t.bypass_shift_hours, false) AS shift_hours
        FROM public.leave_requests l
        JOIN public.leave_types t ON t.id = l.leave_type_id
        WHERE l.user_id = p_user_id
          AND v_work_date BETWEEN l.start_date AND l.end_date
    )
    SELECT
        CASE max(CASE WHEN offsite AND status='approved' THEN 2 WHEN offsite AND status='pending' THEN 1 ELSE 0 END)
            WHEN 2 THEN 'approved' WHEN 1 THEN 'pending' END,
        CASE max(CASE WHEN shift_hours AND name='Izin Masuk Terlambat' AND status='approved' THEN 2
                      WHEN shift_hours AND name='Izin Masuk Terlambat' AND status='pending' THEN 1 ELSE 0 END)
            WHEN 2 THEN 'approved' WHEN 1 THEN 'pending' END,
        CASE max(CASE WHEN shift_hours AND name='Izin Pulang Cepat' AND status='approved' THEN 2
                      WHEN shift_hours AND name='Izin Pulang Cepat' AND status='pending' THEN 1 ELSE 0 END)
            WHEN 2 THEN 'approved' WHEN 1 THEN 'pending' END,
        coalesce(bool_or(offsite), false),
        coalesce(bool_or(shift_hours), false)
    INTO v_offsite, v_late, v_early, v_has_typed_offsite, v_has_typed_shift FROM dated;

    IF NOT v_has_typed_offsite AND EXISTS (
        SELECT 1 FROM public.offsite_attendance_requests r
        WHERE r.user_id=p_user_id AND r.schedule_id=p_schedule_id AND r.status::text='approved'
    ) THEN v_offsite := 'approved'; END IF;
    -- Historical untyped approved bypass remains broad only when no dated typed
    -- shift request exists. A late-only request must never grant early departure.
    IF NOT v_has_typed_shift AND EXISTS (
        SELECT 1 FROM public.shift_bypass_requests r
        WHERE r.user_id=p_user_id AND r.schedule_id=p_schedule_id AND r.status::text='approved'
    ) THEN v_late := 'approved'; v_early := 'approved'; END IF;

    RETURN jsonb_build_object('offsite_status', v_offsite, 'late_status', v_late, 'early_status', v_early);
END $function$;
REVOKE ALL ON FUNCTION public.get_attendance_exception_access(uuid,uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_attendance_exception_access(uuid,uuid) TO authenticated;

-- The guarded process_attendance patch follows after confirming exact installed anchors.
DO $migration$
DECLARE
    original text;
    patched text;
    old_access text := $anchor$ SELECT EXISTS (
 SELECT 1 FROM public.shift_bypass_requests
 WHERE schedule_id = p_schedule_id AND user_id = p_user_id AND status = 'approved'
 ) INTO v_has_shift_bypass;

 SELECT EXISTS (
 SELECT 1 FROM public.offsite_attendance_requests
 WHERE schedule_id = p_schedule_id AND user_id = p_user_id AND status = 'approved'
 ) INTO v_has_offsite;$anchor$;
    old_opening text := $anchor$ IF NOT v_allow_any_time AND NOT v_has_shift_bypass AND 
 (now() AT TIME ZONE 'Asia/Jakarta') < (v_attendance_date + v_shift_start - (v_open_before || ' minutes')::interval) 
 THEN$anchor$;
    old_closing text := ' IF NOT v_allow_any_time AND v_current_time > v_close_time THEN';
    old_maximum text := $anchor$ IF NOT v_allow_any_time AND NOT v_has_shift_bypass AND v_max_check_in_time IS NOT NULL AND v_current_time > v_max_check_in_time THEN RAISE EXCEPTION 'Batas maksimal absen masuk sudah lewat.'; END IF;$anchor$;
BEGIN
    SELECT pg_get_functiondef(p.oid) INTO STRICT original
    FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
    WHERE n.nspname='public' AND p.proname='process_attendance';
    original := replace(original, E'\r\n', E'\n');
    -- Exact, unique anchors deliberately fail closed on a different deployed body.
    IF (length(original)-length(replace(original,old_access,'')))/length(old_access) <> 1
       OR (length(original)-length(replace(original,old_opening,'')))/length(old_opening) <> 1
       OR (length(original)-length(replace(original,old_closing,'')))/length(old_closing) <> 1
       OR (length(original)-length(replace(original,old_maximum,'')))/length(old_maximum) <> 1
       OR (length(original)-length(replace(original,' v_has_shift_bypass boolean := FALSE;','')))/length(' v_has_shift_bypass boolean := FALSE;') <> 1
       OR (length(original)-length(replace(original,' IF v_has_shift_bypass THEN','')))/length(' IF v_has_shift_bypass THEN') <> 2
    THEN RAISE EXCEPTION 'Attendance function differs from reviewed anchors; review before applying pending exceptions'; END IF;

    patched := replace(original, ' v_has_shift_bypass boolean := FALSE;',
        ' v_has_shift_bypass boolean := FALSE; v_exception_access jsonb; v_time_access boolean := FALSE;');
    patched := replace(patched, old_access, $patch$
 v_exception_access := public.get_attendance_exception_access(p_user_id,p_schedule_id);
 v_has_offsite := coalesce(v_exception_access->>'offsite_status' IN ('pending','approved'),false);
 -- Only approved, direction-matching requests waive KPI deductions.
 v_has_shift_bypass := coalesce(CASE p_attendance_type
   WHEN 'check_in' THEN v_exception_access->>'late_status' = 'approved'
   WHEN 'check_out' THEN v_exception_access->>'early_status' = 'approved'
   ELSE false END,false);
 -- Permission to record is distinct from the approved-only KPI flag above.
 v_time_access := coalesce(CASE p_attendance_type
   WHEN 'check_in' THEN v_exception_access->>'late_status' IN ('pending','approved')
   WHEN 'check_out' THEN v_exception_access->>'early_status' IN ('pending','approved')
       AND v_current_time < v_shift_end
   ELSE false END,false);
$patch$);
    -- A late-arrival exception must not allow arrival before the opening window.
    patched := replace(patched, old_opening, $patch$ IF NOT v_allow_any_time AND 
 (now() AT TIME ZONE 'Asia/Jakarta') < (v_attendance_date + v_shift_start - (v_open_before || ' minutes')::interval) 
 THEN$patch$);
    patched := replace(patched, old_maximum, replace(old_maximum,'NOT v_has_shift_bypass','NOT v_time_access'));
    -- Only late check-in access bypasses daily closing; early permission cannot
    -- enable a late checkout. Preserve all existing checkout admission behavior.
    patched := replace(patched, old_closing,
        ' IF NOT v_allow_any_time AND NOT (p_attendance_type = ''check_in'' AND v_time_access) AND v_current_time > v_close_time THEN');
    -- Guard against a newly introduced gate silently retaining approval-only access.
    IF strpos(patched,'NOT v_has_shift_bypass') > 0 THEN
        RAISE EXCEPTION 'Unreviewed approval-only attendance gate remains';
    END IF;
    IF (length(patched)-length(replace(patched,' IF v_has_shift_bypass THEN','')))/length(' IF v_has_shift_bypass THEN') <> 2 THEN
        RAISE EXCEPTION 'KPI exemption blocks unexpectedly changed';
    END IF;
    EXECUTE patched;
END $migration$;

-- Existing server has no early-checkout admission gate; this draft preserves that
-- behavior. Android restricts early access; authoritative early/overnight enforcement
-- needs a separate reviewed server change before treating that client gate as security.
NOTIFY pgrst, 'reload schema';
COMMIT;
