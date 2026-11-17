// Shared runtime gate for legacy Edge functions as well as account endpoints.
export async function requireLiveSession(req: Request): Promise<Response | null> {
  if (req.method === 'OPTIONS') return null;
  const authorization = req.headers.get('authorization') ?? '';
  const service = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');
  if (service && authorization === `Bearer ${service}`) return null;
  const response = (status: number, error: string) => new Response(JSON.stringify({ error }), {
    status, headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' },
  });
  if (!/^Bearer\s+\S+$/i.test(authorization)) return response(401, 'Silakan masuk kembali.');
  try {
    const result = await fetch(`${Deno.env.get('SUPABASE_URL')}/rest/v1/rpc/current_account_access`, {
      method: 'POST', headers: {
        authorization, apikey: Deno.env.get('SUPABASE_ANON_KEY') ?? '',
        'Content-Type': 'application/json',
      }, body: '{}', signal: AbortSignal.timeout(8000),
    });
    if (result.status === 401 || result.status === 403) return response(403, 'Sesi tidak aktif.');
    if (!result.ok) return response(503, 'Pemeriksaan sesi tidak tersedia.');
    return (await result.json()).active === true ? null : response(403, 'Sesi tidak aktif.');
  } catch { return response(503, 'Pemeriksaan sesi tidak tersedia.'); }
}
