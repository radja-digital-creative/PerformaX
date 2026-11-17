-- Run with psql -v ON_ERROR_STOP=1 on an isolated clone AFTER the migration.
-- Everything, including fixtures, is rolled back. No production test data.
BEGIN;
DO $test$
DECLARE
 u uuid; st uuid; sh uuid; sid uuid:=gen_random_uuid(); aid uuid:=gen_random_uuid();
 late_id uuid; early_id uuid; offsite_id uuid; lr uuid; er uuid; off uuid;
 d date:='2098-06-10'; s time; e time; a record; j jsonb;
 late_points integer; in_points integer; early_points integer; out_points integer;
 gate_sid uuid:=gen_random_uuid(); gate_error boolean:=false; lat double precision; lon double precision;
 rule_id uuid:=gen_random_uuid(); log_id uuid:=gen_random_uuid(); penalty_id uuid:=gen_random_uuid(); amount numeric;
BEGIN
 SELECT us.user_id,us.store_id,us.shift_id,h.start_time,h.end_time
 INTO STRICT u,st,sh,s,e FROM public.user_schedules us JOIN public.shifts h ON h.id=us.shift_id
 WHERE h.end_time>h.start_time+interval '4 hours' LIMIT 1;
 SELECT id INTO STRICT late_id FROM public.leave_types WHERE name='Izin Masuk Terlambat' AND bypass_shift_hours LIMIT 1;
 SELECT id INTO STRICT early_id FROM public.leave_types WHERE name='Izin Pulang Cepat' AND bypass_shift_hours LIMIT 1;
 SELECT id INTO STRICT offsite_id FROM public.leave_types WHERE is_offsite LIMIT 1;
 SELECT max(point_value) FILTER(WHERE rule_code='IN_LATE'),max(point_value) FILTER(WHERE rule_code='IN_ON_TIME'),
 max(point_value) FILTER(WHERE rule_code='OUT_EARLY'),max(point_value) FILTER(WHERE rule_code='OUT_ON_TIME')
 INTO late_points,in_points,early_points,out_points FROM public.kpi_point_rules;
 PERFORM set_config('request.jwt.claims',jsonb_build_object('sub',u,'role','service_role')::text,true);
 PERFORM set_config('request.jwt.claim.sub',u::text,true);
 INSERT INTO public.user_schedules(id,user_id,store_id,shift_id,work_date) VALUES(sid,u,st,sh,d);
 INSERT INTO public.leave_requests(user_id,leave_type_id,start_date,end_date,reason,status)
 VALUES(u,late_id,d,d,'ROLLBACK TEST late','pending') RETURNING id INTO lr;
 j:=public.get_attendance_exception_access(u,sid);
 IF j->>'late_status' IS DISTINCT FROM 'pending' OR j->>'early_status' IS NOT NULL THEN
 RAISE EXCEPTION 'Pending late request grants incorrect direction: %',j; END IF;
 INSERT INTO public.attendances(id,user_id,schedule_id,attendance_date,check_in_time,check_out_time,check_in_status,check_out_status,earned_points)
 VALUES(aid,u,sid,d,(d+s+interval '2 hours') AT TIME ZONE 'Asia/Jakarta',
 (d+e-interval '1 hour') AT TIME ZONE 'Asia/Jakarta','Tepat Waktu','Tepat Waktu',999);
 SELECT * INTO a FROM public.attendances WHERE id=aid;
 IF a.check_in_status::text<>'Terlambat' OR a.check_out_status::text<>'Lebih Awal' OR a.earned_points<>late_points+early_points THEN
 RAISE EXCEPTION 'Pending incorrectly waived deductions: %',row_to_json(a); END IF;
 INSERT INTO public.employee_rules SELECT (jsonb_populate_record(NULL::public.employee_rules,
 to_jsonb(r)||jsonb_build_object('id',rule_id,'trigger_type','late','trigger_mode','consecutive','trigger_threshold',1,'penalty_type','deduction_fixed','penalty_fixed_amount',100))).*
 FROM public.employee_rules r LIMIT 1;
 IF NOT FOUND THEN RAISE EXCEPTION 'Test requires one existing rule as schema fixture'; END IF;
 INSERT INTO public.employee_violation_logs(id,user_id,user_name,store_id,rule_id,violation_date,applied_penalty,violation_notes,is_processed_to_payroll)
 VALUES(log_id,u,'ROLLBACK TEST',st,rule_id,d,'100','ROLLBACK TEST',false);
 INSERT INTO public.employee_violation_penalties(id,violation_log_id,user_id,penalty_amount,penalty_type)
 VALUES(penalty_id,log_id,u,100,'DEDUCTION_FIXED');
 SELECT penalty_amount INTO amount FROM public.employee_violation_penalties WHERE id=penalty_id;
 IF amount<>100 THEN RAISE EXCEPTION 'Pending waived payroll penalty'; END IF;
 UPDATE public.leave_requests SET status='approved' WHERE id=lr;
 SELECT penalty_amount INTO amount FROM public.employee_violation_penalties WHERE id=penalty_id;
 IF amount<>0 THEN RAISE EXCEPTION 'Approval did not waive payroll penalty'; END IF;
 SELECT * INTO a FROM public.attendances WHERE id=aid;
 IF a.check_in_status::text<>'Tepat Waktu' OR a.check_out_status::text<>'Lebih Awal' OR a.earned_points<>in_points+early_points THEN
 RAISE EXCEPTION 'Late approval did not reconcile only check-in'; END IF;
 INSERT INTO public.leave_requests(user_id,leave_type_id,start_date,end_date,reason,status)
 VALUES(u,early_id,d,d,'ROLLBACK TEST early','pending') RETURNING id INTO er;
 SELECT * INTO a FROM public.attendances WHERE id=aid;
 IF a.check_out_status::text<>'Lebih Awal' THEN RAISE EXCEPTION 'Pending early waived deduction'; END IF;
 UPDATE public.leave_requests SET status='approved' WHERE id=er;
 SELECT * INTO a FROM public.attendances WHERE id=aid;
 IF a.check_out_status::text<>'Tepat Waktu' OR a.earned_points<>in_points+out_points THEN RAISE EXCEPTION 'Early approval did not reconcile'; END IF;
 UPDATE public.leave_requests SET status='rejected' WHERE id=lr;
 SELECT penalty_amount INTO amount FROM public.employee_violation_penalties WHERE id=penalty_id;
 IF amount<>100 THEN RAISE EXCEPTION 'Rejection did not restore payroll penalty'; END IF;
 SELECT * INTO a FROM public.attendances WHERE id=aid;
 IF a.check_in_status::text<>'Terlambat' OR a.earned_points<>late_points+out_points THEN RAISE EXCEPTION 'Rejection did not restore deduction'; END IF;
 DELETE FROM public.leave_requests WHERE id=er;
 SELECT * INTO a FROM public.attendances WHERE id=aid;
 IF a.check_out_status::text<>'Lebih Awal' OR a.earned_points<>late_points+early_points THEN RAISE EXCEPTION 'Deletion retained early waiver'; END IF;
 INSERT INTO public.leave_requests(user_id,leave_type_id,start_date,end_date,reason,status)
 VALUES(u,offsite_id,d,d,'ROLLBACK TEST offsite','pending') RETURNING id INTO off;
 j:=public.get_attendance_exception_access(u,sid);
 IF j->>'offsite_status' IS DISTINCT FROM 'pending' OR j->>'late_status' IS NOT NULL OR j->>'early_status' IS NOT NULL THEN RAISE EXCEPTION 'Offsite grants time waiver: %',j; END IF;
 UPDATE public.leave_requests SET status='rejected' WHERE id=off;
 j:=public.get_attendance_exception_access(u,sid);
 IF j->>'offsite_status' IS NOT NULL THEN RAISE EXCEPTION 'Rejected offsite retained access'; END IF;
 IF a.check_in_time IS DISTINCT FROM ((d+s+interval '2 hours') AT TIME ZONE 'Asia/Jakarta') OR
 a.check_out_time IS DISTINCT FROM ((d+e-interval '1 hour') AT TIME ZONE 'Asia/Jakarta') THEN RAISE EXCEPTION 'Reconciliation changed timestamps'; END IF;
 -- RPC admission: no early permission is denied, pending early permits recording.
 SELECT latitude,longitude INTO lat,lon FROM public.stores WHERE id=st;
 UPDATE public.attendance_settings SET daily_close_time='23:59:59' WHERE store_id=st;
 INSERT INTO public.employee_attendance_preferences(user_id,require_photo,allow_any_time)
 VALUES(u,false,false) ON CONFLICT(user_id) DO UPDATE SET require_photo=false,allow_any_time=false;
 INSERT INTO public.user_schedules(id,user_id,store_id,shift_id,work_date) VALUES(gate_sid,u,st,sh,d+1);
 INSERT INTO public.attendances(user_id,schedule_id,attendance_date,check_in_time,check_in_status,earned_points)
 VALUES(u,gate_sid,d+1,now()-interval '1 hour','Lebih Awal',0);
 BEGIN
  PERFORM public.process_attendance(u,gate_sid,'check_out',coalesce(lat,0),coalesce(lon,0),NULL,false);
 EXCEPTION WHEN OTHERS THEN
  IF SQLERRM NOT LIKE '%Izin pulang cepat diperlukan%' THEN RAISE; END IF;
  gate_error:=true;
 END;
 IF NOT gate_error THEN RAISE EXCEPTION 'RPC allowed early checkout without permission'; END IF;
 INSERT INTO public.leave_requests(user_id,leave_type_id,start_date,end_date,reason,status)
 VALUES(u,early_id,d+1,d+1,'ROLLBACK TEST early gate','pending');
 PERFORM public.process_attendance(u,gate_sid,'check_out',coalesce(lat,0),coalesce(lon,0),NULL,false);
 IF NOT EXISTS(SELECT 1 FROM public.attendances WHERE schedule_id=gate_sid AND check_out_time IS NOT NULL AND check_out_status::text='Lebih Awal') THEN
 RAISE EXCEPTION 'Pending early RPC failed or waived deduction'; END IF;
 RAISE NOTICE 'PASS: pending directions, approved-only waivers, rejection/deletion restoration, offsite and timestamps';
END $test$;
ROLLBACK;
