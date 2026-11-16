// Deployment draft. See database/account_access.sql for mandatory rollout gates.
// Secrets are read ONLY in the Edge runtime; never bundle them in the Android app.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.8";

const url = Deno.env.get("SUPABASE_URL")!;
const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const admin = createClient(url, key, { auth: { persistSession: false, autoRefreshToken: false } });
const headers = { "Content-Type": "application/json", "Cache-Control": "no-store" };
const respond = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers });
const uuid = (v: unknown): v is string => typeof v === "string" && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(v);
class HttpError extends Error { constructor(public status: number, message: string) { super(message); } }
const fail = (status: number, message: string): never => { throw new HttpError(status, message); };

async function profile(id: string) {
  const { data, error } = await admin.from("profiles").select("id, role_id, is_active").eq("id", id).maybeSingle();
  if (error) fail(503, "Pemeriksaan akun tidak tersedia.");
  return data;
}
async function role(id: number) {
  const { data, error } = await admin.from("roles").select("role_name").eq("id", id).maybeSingle();
  if (error || !data) fail(400, "Peran tidak valid.");
  return String(data.role_name).toLowerCase();
}
async function storeIds(id: string): Promise<string[]> {
  const { data, error } = await admin.from("user_stores").select("store_id").eq("user_id", id);
  if (error) fail(503, "Pemeriksaan toko tidak tersedia.");
  return (data ?? []).map(row => String(row.store_id));
}

Deno.serve(async req => {
  if (req.method !== "POST") return respond(405, { error: "Gunakan POST." });
  try {
    const token = req.headers.get("Authorization")?.match(/^Bearer\s+(\S+)$/i)?.[1];
    if (!token) fail(401, "Silakan masuk kembali.");
    const { data: auth, error: authError } = await admin.auth.getUser(token);
    if (authError || !auth.user) fail(401, "Sesi tidak valid.");
    const actor = await profile(auth.user.id);
    if (!actor?.is_active) fail(403, "Akun tidak aktif.");
    const actorRole = await role(actor.role_id);
    const raw = await req.text();
    if (raw.length > 65536) fail(413, "Permintaan terlalu besar.");
    let body: Record<string, any>;
    try { body = JSON.parse(raw); } catch { fail(400, "JSON tidak valid."); }
    if (!body || Array.isArray(body) || typeof body !== "object") fail(400, "Permintaan tidak valid.");

    if (body.action === "create") {
      if (!["owner", "manager"].includes(actorRole)) fail(403, "Tidak diizinkan membuat akun.");
      const payload = body;
      if (!payload || !Number.isInteger(payload.p_role_id)) fail(400, "Data karyawan tidak valid.");
      const requestedRole = await role(payload.p_role_id);
      if (actorRole === "manager") {
        if (!["staff", "employee"].includes(requestedRole)) fail(403, "Manager hanya dapat membuat Staff.");
        const permitted = new Set(await storeIds(actor.id));
        if (!Array.isArray(payload.p_stores) || !payload.p_stores.length || payload.p_stores.some((s: any) => !permitted.has(s.store_id))) fail(403, "Toko di luar akses Manager.");
        if (!Array.isArray(payload.p_schedules ?? []) || (payload.p_schedules ?? []).some((s: any) => !payload.p_stores.some((p: any) => p.store_id === s.store_id))) fail(400, "Jadwal harus berada di toko karyawan.");
      }
      if (typeof payload.email !== "string" || payload.email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email.trim())) fail(400, "Email tidak valid.");
      if (typeof payload.password !== "string" || payload.password.length < 8 || payload.password.length > 128) fail(400, "Password harus 8–128 karakter.");
      const { data: created, error: createError } = await admin.auth.admin.createUser({
        email: payload.email.trim(), password: payload.password, email_confirm: true,
        // Keep the Auth account unusable until its business rows exist.
        ban_duration: "876000h",
      });
      if (createError || !created.user) fail(400, "Akun gagal dibuat. Periksa email dan kebijakan password.");
      const fields = ["p_role_id", "p_full_name", "p_gender", "p_address", "p_phone_number", "p_id_card_url", "p_face_reference_url", "p_stores", "p_schedules", "p_pay_per_day", "p_basic_salary", "p_transport_allowance", "p_meal_allowance", "p_position_allowance", "p_deduction_ids"];
      const registration: Record<string, unknown> = { p_auth_uid: created.user.id, p_payroll_type: payload.p_payroll_type ?? "monthly" };
      for (const field of fields) if (payload[field] !== undefined) registration[field] = payload[field];
      const { data: registered, error: registerError } = await admin.rpc("register_new_employee", registration);
      if (registerError || registered !== true) {
        // Best-effort compensation; if it fails the orphan remains banned and has
        // no active profile. Operations must reconcile it before retrying email.
        const { error: cleanupError } = await admin.auth.admin.deleteUser(created.user.id);
        if (cleanupError) console.error("employee-account registration cleanup required", created.user.id);
        fail(409, "Data karyawan gagal disimpan; hubungi administrator jika email tidak dapat digunakan kembali.");
      }
      const { error: unbanError } = await admin.auth.admin.updateUserById(created.user.id, { ban_duration: "none" });
      if (unbanError) fail(503, "Data karyawan tersimpan, tetapi akun belum dapat digunakan. Administrator perlu menyelesaikan aktivasi; jangan ulangi pembuatan akun.");
      return respond(200, { success: true, user_id: created.user.id });
    }

    if (!uuid(body.user_id)) fail(400, "ID pengguna tidak valid.");
    const target = await profile(body.user_id);
    if (!target) fail(404, "Akun tidak ditemukan.");
    if (body.action === "get_email" || body.action === "update_credentials") {
      if (actor.id !== target.id && actorRole !== "owner") fail(403, "Hanya pemilik akun atau Owner yang diizinkan.");
      if (body.action === "get_email") {
        const { data, error } = await admin.auth.admin.getUserById(target.id);
        if (error || !data.user) fail(503, "Email belum dapat diambil.");
        return respond(200, { email: data.user.email ?? null });
      }
      const changes: { email?: string; password?: string } = {};
      if (body.email !== undefined) {
        if (typeof body.email !== "string" || body.email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(body.email.trim())) fail(400, "Email tidak valid.");
        changes.email = body.email.trim();
      }
      if (body.password !== undefined && body.password !== "") {
        if (typeof body.password !== "string" || body.password.length < 8 || body.password.length > 128) fail(400, "Password baru harus 8–128 karakter.");
        changes.password = body.password;
      }
      if (!Object.keys(changes).length) fail(400, "Tidak ada perubahan kredensial.");
      const { error } = await admin.auth.admin.updateUserById(target.id, changes);
      if (error) fail(400, "Kredensial gagal diperbarui. Periksa email dan kebijakan password.");
      return respond(200, { success: true });
    }
    if (body.action === "disable") {
      // Authorization, target role/store checks, owner count and session revocation
      // run atomically in SQL; p_actor is sourced from verified getUser, not JSON.
      const { error } = await admin.rpc("disable_employee_account", { p_actor: actor.id, p_target: target.id });
      if (error) fail(error.code === "42501" ? 403 : 409, "Akun gagal dinonaktifkan. Periksa hak akses dan status akun.");
      const { error: banError } = await admin.auth.admin.updateUserById(target.id, { ban_duration: "876000h" });
      if (banError) fail(503, "Profil sudah nonaktif dan sesi dicabut, tetapi pemblokiran login belum selesai. Administrator perlu mengulangi penonaktifan akun.");
      return respond(200, { success: true, active: false });
    }
    fail(400, "Aksi tidak dikenali.");
  } catch (error) {
    // Never log request bodies, tokens, passwords or Auth response details.
    return respond(error instanceof HttpError ? error.status : 500, { error: error instanceof HttpError ? error.message : "Layanan akun sementara tidak tersedia." });
  }
});
