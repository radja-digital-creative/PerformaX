-- Apply as a single transaction. Existing employee defaults are unchanged until saved.
BEGIN;
CREATE TABLE public.employee_attendance_preferences (
 user_id uuid PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
 require_photo boolean NOT NULL DEFAULT true,
 allow_any_time boolean NOT NULL DEFAULT false,
 max_check_in_time time,
 default_store_id uuid REFERENCES public.stores(id),
 default_shift_id uuid REFERENCES public.shifts(id),
 work_days integer[] NOT NULL DEFAULT ARRAY[1,2,3,4,5,6],
 CONSTRAINT valid_work_days CHECK (work_days <@ ARRAY[1,2,3,4,5,6,7] AND array_position(work_days,NULL) IS NULL),
 CONSTRAINT complete_default_shift CHECK ((default_store_id IS NULL) = (default_shift_id IS NULL)),
 CONSTRAINT unrestricted_has_no_cutoff CHECK (NOT allow_any_time OR max_check_in_time IS NULL)
);
ALTER TABLE public.employee_attendance_preferences ENABLE ROW LEVEL SECURITY;
GRANT SELECT,INSERT,UPDATE ON public.employee_attendance_preferences TO authenticated;
REVOKE ALL ON public.employee_attendance_preferences FROM anon;
CREATE POLICY preferences_read ON public.employee_attendance_preferences FOR SELECT TO authenticated
 USING (user_id = (SELECT auth.uid()) OR EXISTS (SELECT 1 FROM public.profiles p JOIN public.roles r ON r.id=p.role_id WHERE p.id=(SELECT auth.uid()) AND lower(r.role_name) IN ('owner','admin')));
CREATE POLICY preferences_insert ON public.employee_attendance_preferences FOR INSERT TO authenticated
 WITH CHECK (EXISTS (SELECT 1 FROM public.profiles p JOIN public.roles r ON r.id=p.role_id WHERE p.id=(SELECT auth.uid()) AND lower(r.role_name) IN ('owner','admin')));
CREATE POLICY preferences_update ON public.employee_attendance_preferences FOR UPDATE TO authenticated
 USING (EXISTS (SELECT 1 FROM public.profiles p JOIN public.roles r ON r.id=p.role_id WHERE p.id=(SELECT auth.uid()) AND lower(r.role_name) IN ('owner','admin')))
 WITH CHECK (EXISTS (SELECT 1 FROM public.profiles p JOIN public.roles r ON r.id=p.role_id WHERE p.id=(SELECT auth.uid()) AND lower(r.role_name) IN ('owner','admin')));

CREATE OR REPLACE FUNCTION public.fn_auto_scheduler() RETURNS void LANGUAGE plpgsql SECURITY INVOKER SET search_path=public AS $$
DECLARE
 start_range date := (now() AT TIME ZONE 'Asia/Jakarta')::date;
 end_range date := (date_trunc('month', (now() AT TIME ZONE 'Asia/Jakarta') + interval '2 month') - interval '1 day')::date;
BEGIN
 INSERT INTO public.user_schedules(user_id,store_id,shift_id,work_date,notes)
 SELECT p.id, office.store_id, shift.id, d::date, 'Automatic employee defaults'
 FROM public.profiles p JOIN public.roles r ON r.id=p.role_id
 LEFT JOIN public.employee_attendance_preferences pref ON pref.user_id=p.id
 JOIN LATERAL (
   SELECT us.store_id FROM public.user_stores us JOIN public.stores st ON st.id=us.store_id AND st.is_active
   WHERE us.user_id=p.id AND (pref.default_store_id IS NULL OR us.store_id=pref.default_store_id)
   ORDER BY us.is_primary DESC NULLS LAST, us.created_at, us.id LIMIT 1
 ) office ON true
 JOIN LATERAL (
   SELECT sh.id FROM public.shifts sh WHERE sh.store_id=office.store_id AND sh.is_active
   AND (pref.default_shift_id IS NULL OR sh.id=pref.default_shift_id) ORDER BY sh.start_time,sh.id LIMIT 1
 ) shift ON true
 CROSS JOIN generate_series(start_range,end_range,interval '1 day') d
 WHERE p.is_active AND lower(r.role_name) IN ('employee','manager','admin')
 AND extract(isodow FROM d)=ANY(coalesce(pref.work_days,ARRAY[1,2,3,4,5,6]))
 AND NOT EXISTS (SELECT 1 FROM public.user_schedules s WHERE s.user_id=p.id AND s.work_date=d::date)
 ON CONFLICT (user_id,store_id,work_date) DO NOTHING;
END $$;

CREATE FUNCTION public.sync_employee_attendance_preferences() RETURNS trigger LANGUAGE plpgsql SECURITY INVOKER SET search_path=public AS $$
BEGIN
 IF NEW.default_store_id IS NOT NULL THEN
   IF NOT EXISTS (SELECT 1 FROM public.user_stores WHERE user_id=NEW.user_id AND store_id=NEW.default_store_id) THEN
     RAISE EXCEPTION 'Kantor default harus termasuk penempatan karyawan.';
   END IF;
   IF NOT EXISTS (SELECT 1 FROM public.shifts WHERE id=NEW.default_shift_id AND store_id=NEW.default_store_id AND is_active) THEN
     RAISE EXCEPTION 'Shift default harus aktif dan berada di kantor yang dipilih.';
   END IF;
 END IF;
 -- Photo/time changes alone never rewrite schedules, including the first save.
 IF TG_OP='INSERT' AND NEW.default_store_id IS NULL AND NEW.default_shift_id IS NULL AND NEW.work_days=ARRAY[1,2,3,4,5,6] THEN RETURN NEW; END IF;
 IF TG_OP='UPDATE' THEN
   IF NEW.default_store_id IS NOT DISTINCT FROM OLD.default_store_id AND NEW.default_shift_id IS NOT DISTINCT FROM OLD.default_shift_id AND NEW.work_days IS NOT DISTINCT FROM OLD.work_days THEN RETURN NEW; END IF;
 END IF;
 IF NEW.default_store_id IS NOT NULL THEN
   UPDATE public.user_stores SET is_primary=(store_id=NEW.default_store_id) WHERE user_id=NEW.user_id;
 END IF;
 -- Only future automatically generated rows without attendance/requests are regenerated.
 -- Today's schedule, history, bulk/manual overrides and linked requests are preserved.
 DELETE FROM public.user_schedules s WHERE s.user_id=NEW.user_id
 AND s.work_date > (now() AT TIME ZONE 'Asia/Jakarta')::date
 AND (s.notes IS NULL OR s.notes='Automatic employee defaults')
 AND NOT EXISTS (SELECT 1 FROM public.attendances a WHERE a.schedule_id=s.id)
 AND NOT EXISTS (SELECT 1 FROM public.manual_attendances a WHERE a.schedule_id=s.id)
 AND NOT EXISTS (SELECT 1 FROM public.shift_bypass_requests a WHERE a.schedule_id=s.id)
 AND NOT EXISTS (SELECT 1 FROM public.offsite_attendance_requests a WHERE a.schedule_id=s.id);
 PERFORM public.fn_auto_scheduler();
 RETURN NEW;
END $$;
CREATE TRIGGER sync_employee_attendance_preferences AFTER INSERT OR UPDATE ON public.employee_attendance_preferences FOR EACH ROW EXECUTE FUNCTION public.sync_employee_attendance_preferences();

-- Preserve the installed attendance/KPI/payroll implementation, changing only admission checks.
DO $$
DECLARE original text; patched text;
BEGIN
 SELECT pg_get_functiondef(p.oid) INTO STRICT original FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public' AND p.proname='process_attendance';
 IF strpos(original,'IF v_current_time > v_close_time THEN')=0 OR strpos(original,'IF NOT v_has_shift_bypass AND')=0 THEN RAISE EXCEPTION 'Unexpected attendance function; review before upgrading'; END IF;
 patched := replace(original, 'v_close_time time;', 'v_close_time time; v_allow_any_time boolean := false; v_require_photo boolean := true; v_max_check_in_time time;');
 IF patched=original THEN RAISE EXCEPTION 'Attendance declaration mismatch'; END IF;
 patched := replace(patched, '-- 3. AMBIL ATURAN POIN KPI DARI TABEL', $patch$
 SELECT require_photo,allow_any_time,max_check_in_time INTO v_require_photo,v_allow_any_time,v_max_check_in_time FROM public.employee_attendance_preferences WHERE user_id=p_user_id;
 v_require_photo := coalesce(v_require_photo,true);
 v_allow_any_time := coalesce(v_allow_any_time,false);
 IF auth.uid() IS DISTINCT FROM p_user_id THEN RAISE EXCEPTION 'Sesi absensi tidak sesuai karyawan.'; END IF;
 IF v_require_photo AND nullif(trim(p_image_url),'') IS NULL THEN RAISE EXCEPTION 'Foto wajib untuk karyawan ini.'; END IF;
 -- 3. AMBIL ATURAN POIN KPI DARI TABEL$patch$);
 IF strpos(patched,'Sesi absensi')=0 THEN RAISE EXCEPTION 'Attendance settings insertion mismatch'; END IF;
 patched := replace(patched,'IF v_current_time > v_close_time THEN','IF NOT v_allow_any_time AND v_current_time > v_close_time THEN');
 patched := replace(patched,'IF NOT v_has_shift_bypass AND','IF NOT v_allow_any_time AND NOT v_has_shift_bypass AND');
 patched := replace(patched,'-- Cek Tabel Berdasarkan Flag Manual', $patch$
 IF NOT v_allow_any_time AND NOT v_has_shift_bypass AND v_max_check_in_time IS NOT NULL AND v_current_time > v_max_check_in_time THEN RAISE EXCEPTION 'Batas maksimal absen masuk sudah lewat.'; END IF;
 -- Cek Tabel Berdasarkan Flag Manual$patch$);
 EXECUTE patched;
END $$;
NOTIFY pgrst, 'reload schema';
COMMIT;
