// Supabase Edge Function: يرسل إشعار FCM للمستلم عند إضافة رسالة جديدة في fr_messages.
// الأسرار المطلوبة (supabase secrets set):
//   FCM_SERVICE_ACCOUNT  = محتوى ملف JSON الخاص بالـ Service Account كاملاً
//   WEBHOOK_SECRET       = كلمة سر عشوائية (نفسها تحطها في هيدر الـ Webhook)
// SUPABASE_URL و SUPABASE_SERVICE_ROLE_KEY موجودين تلقائياً.
// يحتاج Webhook (Insert) على جدولين، بنفس الـ URL ونفس هيدر x-webhook-secret:
//   1) fr_messages  → إشعار رسالة جديدة
//   2) fr_requests  → إشعار طلب صداقة جديد
import { createClient } from "npm:@supabase/supabase-js@2";

const sa = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT") ?? "{}");
const db = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

const b64url = (b: ArrayBuffer | string) => {
  const bytes = typeof b === "string" ? new TextEncoder().encode(b) : new Uint8Array(b);
  let s = "";
  for (const c of bytes) s += String.fromCharCode(c);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
};

let cached: { token: string; exp: number } | null = null;
async function accessToken(): Promise<string> {
  if (cached && cached.exp > Date.now() + 60_000) return cached.token;
  const now = Math.floor(Date.now() / 1000);
  const head = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const pem = String(sa.private_key).replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${head}.${claim}`));
  const r = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${head}.${claim}.${b64url(sig)}`,
    }),
  });
  const j = await r.json();
  if (!j.access_token) throw new Error("oauth_failed");
  cached = { token: j.access_token, exp: Date.now() + (j.expires_in ?? 3600) * 1000 };
  return cached.token;
}

Deno.serve(async (req) => {
  if (req.headers.get("x-webhook-secret") !== Deno.env.get("WEBHOOK_SECRET")) {
    return new Response("forbidden", { status: 403 });
  }
  const rec = (await req.json())?.record;
  if (!rec?.to_uid || !rec?.from_uid) return new Response("ignored");
  // سجل fr_requests عنده status ومافي body؛ سجل fr_messages العكس
  const isReq = rec.status !== undefined && rec.body === undefined;
  if (isReq && rec.status !== "pending") return new Response("ignored");

  const { data: from } = await db.from("fr_profiles").select("name").eq("uid", rec.from_uid).maybeSingle();
  const { data: rows } = await db.from("fcm_tokens").select("token").eq("uid", rec.to_uid);
  if (!rows?.length) return new Response("no tokens");

  const name = from?.name || "صديق";
  const body = isReq
    ? `${name} أرسل لك طلب صداقة 👋`
    : rec.kind === "cheer" ? `${name} شجّعك 💪` : `${name}: ${String(rec.body).slice(0, 100)}`;
  const at = await accessToken();

  for (const { token } of rows) {
    const r = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${at}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token,
          data: { title: "مذاكرتي", body, tag: `${isReq ? "rq" : "msg"}-${rec.from_uid}` },
          android: { priority: "HIGH" },
        },
      }),
    });
    if (r.status === 404 || r.status === 400) {
      const t = await r.text();
      if (t.includes("UNREGISTERED") || t.includes("INVALID_ARGUMENT")) {
        await db.from("fcm_tokens").delete().eq("token", token); // توكن ميت
      }
    }
  }
  return new Response("ok");
});
