-- Pending exceptions grant action access; only approved exceptions waive deductions.
-- Apply transactionally after account_access and employee_attendance_preferences.
BEGIN;

CREATE OR REPLACE FUNCTION public.attendance_exception_access_internal(
    p_user_id uuid, p_schedule_id uuid
) RETURNS jsonb
LANGUAGE plpgsql VOLATILE SECURITY INVOKER SET search_path = public
AS $function$
DECLARE
    v_work_date date;
    v_offsite text;
    v_late text;
    v_early text;
    v_has_typed_offsite boolean;
    v_has_typed_shift boolean;
BEGIN
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
REVOKE ALL ON FUNCTION public.attendance_exception_access_internal(uuid,uuid) FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.get_attendance_exception_access(p_user_id uuid,p_schedule_id uuid)
RETURNS jsonb LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=public AS $function$
BEGIN
    IF auth.uid() IS NULL OR auth.uid() IS DISTINCT FROM p_user_id THEN
        RAISE EXCEPTION 'Sesi absensi tidak sesuai karyawan.';
    END IF;
    RETURN public.attendance_exception_access_internal(p_user_id,p_schedule_id);
END $function$;
REVOKE ALL ON FUNCTION public.get_attendance_exception_access(uuid,uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_attendance_exception_access(uuid,uuid) TO authenticated;

-- Normalize CRLF/indentation only, then require unique reviewed function anchors.
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
    original := regexp_replace(original, E'\n[ \t]+', E'\n ', 'g');
    IF strpos(original,'v_exception_access := public.get_attendance_exception_access')=0 THEN
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
    patched := replace(patched, E'BEGIN\n', E'BEGIN\n PERFORM pg_advisory_xact_lock(hashtextextended(p_user_id::text,7425));\n');
    ELSE
        patched := original;
    END IF;
    -- Authoritative direction-specific early checkout admission, including shifts
    -- whose end falls after midnight. Account unrestricted-time preference wins.
    IF strpos(patched,'Izin pulang cepat diperlukan sebelum akhir shift.')=0 THEN
        IF (length(patched)-length(replace(patched,' ELSIF p_attendance_type = ''check_out'' THEN','')))/length(' ELSIF p_attendance_type = ''check_out'' THEN')<>1 THEN
            RAISE EXCEPTION 'Unexpected checkout admission anchor';
        END IF;
        patched := replace(patched,' ELSIF p_attendance_type = ''check_out'' THEN',$early$
 ELSIF p_attendance_type = 'check_out' THEN
 IF NOT v_allow_any_time
 AND (now() AT TIME ZONE 'Asia/Jakarta') < (v_attendance_date + v_shift_end + CASE WHEN v_shift_end <= v_shift_start THEN interval '1 day' ELSE interval '0' END)
 AND NOT coalesce(v_exception_access->>'early_status' IN ('pending','approved'),false) THEN
 RAISE EXCEPTION 'Izin pulang cepat diperlukan sebelum akhir shift.';
 END IF;
$early$);
    END IF;
    EXECUTE patched;
END $migration$;

-- RLS replaces the historical unrestricted leave mutation policies.
CREATE OR REPLACE FUNCTION public.can_review_attendance_exception(p_target uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public AS $function$
 SELECT EXISTS(SELECT 1 FROM public.profiles actor JOIN public.roles ar ON ar.id=actor.role_id
   JOIN public.profiles target ON target.id=p_target JOIN public.roles tr ON tr.id=target.role_id
   WHERE actor.id=auth.uid() AND actor.is_active AND
    (lower(ar.role_name)='owner' OR (lower(ar.role_name)='manager' AND lower(tr.role_name) IN ('staff','employee')
      AND EXISTS(SELECT 1 FROM public.user_stores a JOIN public.user_stores t ON a.store_id=t.store_id
                 WHERE a.user_id=actor.id AND t.user_id=target.id))));
$function$;
REVOKE ALL ON FUNCTION public.can_review_attendance_exception(uuid) FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.can_review_attendance_exception(uuid) TO authenticated;
DO $policies$
DECLARE r record;
BEGIN
 FOR r IN SELECT policyname,tablename FROM pg_policies WHERE schemaname='public'
 AND tablename IN ('leave_requests','offsite_attendance_requests','shift_bypass_requests')
 LOOP EXECUTE format('DROP POLICY %I ON public.%I',r.policyname,r.tablename); END LOOP;
END $policies$;
ALTER TABLE public.leave_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY leave_read ON public.leave_requests FOR SELECT TO authenticated
 USING(user_id=auth.uid() OR public.can_review_attendance_exception(user_id));
CREATE POLICY leave_submit ON public.leave_requests FOR INSERT TO authenticated
 WITH CHECK(user_id=auth.uid() AND status::text='pending' AND approved_by IS NULL AND approval_notes IS NULL);
CREATE POLICY leave_review ON public.leave_requests FOR UPDATE TO authenticated
 USING(public.can_review_attendance_exception(user_id))
 WITH CHECK(public.can_review_attendance_exception(user_id));
CREATE POLICY leave_cancel ON public.leave_requests FOR DELETE TO authenticated
 USING(user_id=auth.uid() AND status::text='pending');
-- Legacy approved permissions remain readable; clients cannot manufacture them.
ALTER TABLE public.offsite_attendance_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shift_bypass_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY offsite_read ON public.offsite_attendance_requests FOR SELECT TO authenticated
 USING(user_id=auth.uid() OR public.can_review_attendance_exception(user_id));
CREATE POLICY bypass_read ON public.shift_bypass_requests FOR SELECT TO authenticated
 USING(user_id=auth.uid() OR public.can_review_attendance_exception(user_id));
-- A client must not relabel a leave category to convert a pending request into
-- another category or turn ordinary leave into a time waiver.
REVOKE INSERT,UPDATE,DELETE ON public.leave_types FROM PUBLIC,anon,authenticated;
CREATE OR REPLACE FUNCTION public.guard_attendance_exception_review()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $function$
BEGIN
 IF auth.role()='authenticated' THEN
  IF (NEW.user_id,NEW.leave_type_id,NEW.start_date,NEW.end_date,NEW.reason,NEW.attachment_url)
    IS DISTINCT FROM (OLD.user_id,OLD.leave_type_id,OLD.start_date,OLD.end_date,OLD.reason,OLD.attachment_url) THEN
    RAISE EXCEPTION 'Persetujuan tidak boleh mengubah rincian permohonan.';
  END IF;
  IF NOT public.can_review_attendance_exception(OLD.user_id) THEN
    RAISE EXCEPTION 'Hanya Manager/Owner berwenang yang boleh memutuskan izin.';
  END IF;
  IF NEW.status::text NOT IN ('approved','rejected') THEN
    RAISE EXCEPTION 'Keputusan izin tidak valid.';
  END IF;
  NEW.approved_by := auth.uid();
 END IF;
 RETURN NEW;
END $function$;
REVOKE ALL ON FUNCTION public.guard_attendance_exception_review() FROM PUBLIC,anon,authenticated;
DROP TRIGGER IF EXISTS attendance_exception_review_guard ON public.leave_requests;
CREATE TRIGGER attendance_exception_review_guard BEFORE UPDATE ON public.leave_requests
FOR EACH ROW EXECUTE FUNCTION public.guard_attendance_exception_review();

-- Serialize request decisions and attendance recording for each employee. These
-- locks also cover simultaneous check-in/out and approval requests.
CREATE OR REPLACE FUNCTION public.lock_attendance_exception_employee()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $function$
DECLARE v_user uuid;
BEGIN
    FOR v_user IN SELECT DISTINCT u FROM unnest(ARRAY[
        CASE WHEN TG_OP <> 'INSERT' THEN OLD.user_id END,
        CASE WHEN TG_OP <> 'DELETE' THEN NEW.user_id END]) u WHERE u IS NOT NULL ORDER BY u
    LOOP
        PERFORM pg_advisory_xact_lock(hashtextextended(v_user::text,7425));
    END LOOP;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $function$;
REVOKE ALL ON FUNCTION public.lock_attendance_exception_employee() FROM PUBLIC,anon,authenticated;
DROP TRIGGER IF EXISTS attendance_exception_employee_lock ON public.leave_requests;
CREATE TRIGGER attendance_exception_employee_lock BEFORE INSERT OR UPDATE OR DELETE
ON public.leave_requests FOR EACH ROW EXECUTE FUNCTION public.lock_attendance_exception_employee();

-- Recompute the two KPI components from recorded timestamps. No pending request
-- changes either status to on-time. Overnight shift end belongs to the next day.
CREATE OR REPLACE FUNCTION public.recalculate_attendance_exception_points()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $function$
DECLARE
    v_start timestamp; v_end timestamp; v_in timestamp; v_out timestamp;
    v_late integer; v_overtime integer; v_access jsonb;
    v_in_code text; v_out_code text; v_in_points integer; v_out_points integer;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.user_id::text,7425));
    SELECT s.work_date + sh.start_time,
           s.work_date + sh.end_time + CASE WHEN sh.end_time <= sh.start_time THEN interval '1 day' ELSE interval '0' END,
           coalesce(a.late_tolerance_minutes,15), coalesce(a.overtime_threshold_minutes,60)
    INTO v_start,v_end,v_late,v_overtime
    FROM public.user_schedules s JOIN public.shifts sh ON sh.id=s.shift_id
    LEFT JOIN public.attendance_settings a ON a.store_id=s.store_id
    WHERE s.id=NEW.schedule_id AND s.user_id=NEW.user_id;
    IF NOT FOUND OR NEW.check_in_time IS NULL THEN RETURN NEW; END IF;
    v_access := public.attendance_exception_access_internal(NEW.user_id,NEW.schedule_id);
    v_in := NEW.check_in_time AT TIME ZONE 'Asia/Jakarta';
    v_out := NEW.check_out_time AT TIME ZONE 'Asia/Jakarta';
    IF v_in < v_start THEN
        NEW.check_in_status := 'Lebih Awal'; v_in_code := 'IN_EARLY';
    ELSIF v_in <= v_start + make_interval(mins=>v_late) OR v_access->>'late_status'='approved' THEN
        NEW.check_in_status := 'Tepat Waktu'; v_in_code := 'IN_ON_TIME';
    ELSE
        NEW.check_in_status := 'Terlambat'; v_in_code := 'IN_LATE';
    END IF;
    IF v_out IS NULL THEN
        NEW.check_out_status := 'Belum Absen'; v_out_code := NULL;
    ELSIF v_out >= v_end + make_interval(mins=>v_overtime) THEN
        NEW.check_out_status := 'Lembur'; v_out_code := 'OUT_OVERTIME';
    ELSIF v_out >= v_end OR v_access->>'early_status'='approved' THEN
        NEW.check_out_status := 'Tepat Waktu'; v_out_code := 'OUT_ON_TIME';
    ELSE
        NEW.check_out_status := 'Lebih Awal'; v_out_code := 'OUT_EARLY';
    END IF;
    SELECT max(point_value) FILTER(WHERE rule_code=v_in_code),
           max(point_value) FILTER(WHERE rule_code=v_out_code)
    INTO v_in_points,v_out_points FROM public.kpi_point_rules WHERE rule_code IN(v_in_code,v_out_code);
    IF v_in_points IS NULL OR (v_out_code IS NOT NULL AND v_out_points IS NULL) THEN
        RAISE EXCEPTION 'Aturan poin KPI absensi tidak lengkap.';
    END IF;
    NEW.earned_points := v_in_points + coalesce(v_out_points,0);
    RETURN NEW;
END $function$;
REVOKE ALL ON FUNCTION public.recalculate_attendance_exception_points() FROM PUBLIC,anon,authenticated;
DROP TRIGGER IF EXISTS attendance_exception_points ON public.attendances;
CREATE TRIGGER attendance_exception_points BEFORE INSERT OR UPDATE OF check_in_time,check_out_time,check_in_status,check_out_status,earned_points
ON public.attendances FOR EACH ROW EXECUTE FUNCTION public.recalculate_attendance_exception_points();
DROP TRIGGER IF EXISTS attendance_exception_points ON public.manual_attendances;
CREATE TRIGGER attendance_exception_points BEFORE INSERT OR UPDATE OF check_in_time,check_out_time,check_in_status,check_out_status,earned_points
ON public.manual_attendances FOR EACH ROW EXECUTE FUNCTION public.recalculate_attendance_exception_points();

-- Preserve the original monetary amount while an approved exception invalidates
-- an aggregate late/early violation. Rejection restores that amount exactly.
ALTER TABLE public.employee_violation_penalties ADD COLUMN IF NOT EXISTS attendance_exception_original_amount numeric;
DROP FUNCTION IF EXISTS public.reconcile_attendance_exception_payroll(uuid);
CREATE OR REPLACE FUNCTION public.reconcile_attendance_exception_payroll(p_user uuid,p_start date,p_end date,p_penalty uuid DEFAULT NULL)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $function$
DECLARE p record; v_count integer; v_valid boolean; v_has_waiver boolean; v_first date;
BEGIN
 FOR p IN SELECT evp.id,evp.penalty_amount,evp.attendance_exception_original_amount,
   l.store_id,l.violation_date,r.trigger_type,r.trigger_mode,r.trigger_threshold
 FROM public.employee_violation_penalties evp
 JOIN public.employee_violation_logs l ON l.id=evp.violation_log_id
 JOIN public.employee_rules r ON r.id=l.rule_id
 WHERE evp.user_id=p_user AND (p_penalty IS NULL OR evp.id=p_penalty) AND r.trigger_type IN ('late','early_leave')
   AND r.trigger_mode IN ('consecutive','accumulative_monthly')
 LOOP
  -- Only the changed request's dates can invalidate this historical rule window.
  IF p.trigger_mode='accumulative_monthly' THEN v_first:=date_trunc('month',p.violation_date::timestamp)::date;
  ELSE
   SELECT min(work_date) INTO v_first FROM (SELECT s.work_date FROM public.user_schedules s
    WHERE s.user_id=p_user AND s.store_id=p.store_id AND s.work_date<=p.violation_date
    ORDER BY s.work_date DESC,s.id LIMIT p.trigger_threshold) recent;
  END IF;
  IF p_start IS NOT NULL AND (p_start>p.violation_date OR p_end<v_first) THEN CONTINUE; END IF;
  WITH candidate AS (
   SELECT s.id,s.work_date FROM public.user_schedules s
   WHERE s.user_id=p_user AND s.store_id=p.store_id AND s.work_date<=p.violation_date
    AND (p.trigger_mode='consecutive' OR date_trunc('month',s.work_date::timestamp)=date_trunc('month',p.violation_date::timestamp))
   ORDER BY s.work_date DESC,s.id
   LIMIT CASE WHEN p.trigger_mode='consecutive' THEN p.trigger_threshold ELSE NULL END
  )
  SELECT count(*) INTO v_count FROM candidate c
  WHERE EXISTS(SELECT 1 FROM public.attendances a WHERE a.schedule_id=c.id
   AND CASE WHEN p.trigger_type='late' THEN a.check_in_status::text='Terlambat' ELSE a.check_out_status::text='Lebih Awal' END)
   AND NOT EXISTS(SELECT 1 FROM public.leave_requests lr JOIN public.leave_types lt ON lt.id=lr.leave_type_id
    WHERE lr.user_id=p_user AND lr.status::text='approved' AND c.work_date BETWEEN lr.start_date AND lr.end_date
      AND NOT coalesce(lt.is_offsite,false) AND NOT coalesce(lt.bypass_shift_hours,false));
  v_valid := v_count>=p.trigger_threshold;
  IF p.trigger_mode='accumulative_monthly' THEN v_first:=date_trunc('month',p.violation_date::timestamp)::date;
  ELSE
   SELECT min(work_date) INTO v_first FROM (SELECT s.work_date FROM public.user_schedules s
    WHERE s.user_id=p_user AND s.store_id=p.store_id AND s.work_date<=p.violation_date
    ORDER BY s.work_date DESC,s.id LIMIT p.trigger_threshold) recent;
  END IF;
  SELECT EXISTS(SELECT 1 FROM public.leave_requests lr JOIN public.leave_types lt ON lt.id=lr.leave_type_id
   WHERE lr.user_id=p_user AND lr.status::text='approved' AND lr.start_date<=p.violation_date AND lr.end_date>=v_first
    AND lt.bypass_shift_hours AND lt.name=CASE WHEN p.trigger_type='late' THEN 'Izin Masuk Terlambat' ELSE 'Izin Pulang Cepat' END)
  INTO v_has_waiver;
  IF NOT v_valid AND v_has_waiver AND p.attendance_exception_original_amount IS NULL THEN
   UPDATE public.employee_violation_penalties SET attendance_exception_original_amount=penalty_amount,penalty_amount=0 WHERE id=p.id;
  ELSIF NOT v_valid AND v_has_waiver AND p.penalty_amount<>0 THEN
   UPDATE public.employee_violation_penalties SET penalty_amount=0 WHERE id=p.id;
  ELSIF (v_valid OR NOT v_has_waiver) AND p.attendance_exception_original_amount IS NOT NULL THEN
   UPDATE public.employee_violation_penalties SET penalty_amount=attendance_exception_original_amount,attendance_exception_original_amount=NULL WHERE id=p.id;
  END IF;
 END LOOP;
END $function$;
REVOKE ALL ON FUNCTION public.reconcile_attendance_exception_payroll(uuid,date,date,uuid) FROM PUBLIC,anon,authenticated;
CREATE OR REPLACE FUNCTION public.reconcile_new_attendance_penalty()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $function$
BEGIN
 IF pg_trigger_depth()=1 THEN PERFORM public.reconcile_attendance_exception_payroll(NEW.user_id,NULL,NULL,NEW.id); END IF;
 RETURN NEW;
END $function$;
REVOKE ALL ON FUNCTION public.reconcile_new_attendance_penalty() FROM PUBLIC,anon,authenticated;
DROP TRIGGER IF EXISTS attendance_exception_payroll ON public.employee_violation_penalties;
CREATE TRIGGER attendance_exception_payroll AFTER INSERT OR UPDATE OF penalty_amount ON public.employee_violation_penalties
FOR EACH ROW EXECUTE FUNCTION public.reconcile_new_attendance_penalty();

-- A typed late/early/offsite approval must not excuse the opposite direction.
-- The matching late/early exemption has already updated attendance status.
DO $payroll_patch$
DECLARE src text; anchor text := 'AND lr.status = ''approved''';
BEGIN
 SELECT replace(pg_get_functiondef('public.process_daily_violations()'::regprocedure),E'\r\n',E'\n') INTO src;
 IF strpos(src,'lt.id=lr.leave_type_id AND NOT coalesce(lt.is_offsite,false)')>0 THEN RETURN; END IF;
 IF (length(src)-length(replace(src,anchor,'')))/length(anchor)<>2 THEN
  RAISE EXCEPTION 'Unexpected daily violation approval anchors';
 END IF;
 src:=replace(src,anchor,anchor || E'\n AND EXISTS(SELECT 1 FROM public.leave_types lt WHERE lt.id=lr.leave_type_id AND NOT coalesce(lt.is_offsite,false) AND NOT coalesce(lt.bypass_shift_hours,false))');
 EXECUTE src;
END $payroll_patch$;

-- Replace the legacy broad-access side effect with reversible reconciliation.
-- Deleting/moving/changing a typed exception revokes any synthesized legacy row.
CREATE OR REPLACE FUNCTION public.handle_approved_leave_automation()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $function$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT DISTINCT s.id,s.user_id,t.is_offsite,t.bypass_shift_hours
        FROM public.user_schedules s JOIN public.leave_types t ON
          (TG_OP <> 'INSERT' AND t.id=OLD.leave_type_id AND s.user_id=OLD.user_id AND s.work_date BETWEEN OLD.start_date AND OLD.end_date)
          OR (TG_OP <> 'DELETE' AND t.id=NEW.leave_type_id AND s.user_id=NEW.user_id AND s.work_date BETWEEN NEW.start_date AND NEW.end_date)
        WHERE coalesce(t.is_offsite,false) OR coalesce(t.bypass_shift_hours,false)
    LOOP
        IF r.is_offsite THEN DELETE FROM public.offsite_attendance_requests WHERE user_id=r.user_id AND schedule_id=r.id; END IF;
        IF r.bypass_shift_hours THEN DELETE FROM public.shift_bypass_requests WHERE user_id=r.user_id AND schedule_id=r.id; END IF;
        -- Manual sync runs first, then normal attendance receives the same result.
        UPDATE public.manual_attendances SET earned_points=earned_points WHERE schedule_id=r.id AND user_id=r.user_id;
        UPDATE public.attendances SET earned_points=earned_points WHERE schedule_id=r.id AND user_id=r.user_id;
    END LOOP;
    IF TG_OP<>'INSERT' THEN PERFORM public.reconcile_attendance_exception_payroll(OLD.user_id,OLD.start_date,OLD.end_date); END IF;
    IF TG_OP<>'DELETE' THEN PERFORM public.reconcile_attendance_exception_payroll(NEW.user_id,NEW.start_date,NEW.end_date); END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $function$;
REVOKE ALL ON FUNCTION public.handle_approved_leave_automation() FROM PUBLIC,anon,authenticated;
DROP TRIGGER IF EXISTS trg_auto_handle_approved_leave ON public.leave_requests;
CREATE TRIGGER trg_auto_handle_approved_leave AFTER INSERT OR UPDATE OR DELETE
ON public.leave_requests FOR EACH ROW EXECUTE FUNCTION public.handle_approved_leave_automation();

-- No deployment-time accounting backfill: historical attendance and payroll are
-- reconciled only when a request affecting their dates is subsequently decided,
-- changed or removed. Existing approved requests retain their stored accounting.
NOTIFY pgrst, 'reload schema';
COMMIT;
