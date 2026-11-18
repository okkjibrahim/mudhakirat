-- مذاكرتي: تخزين توكنات FCM (شغّله مرة واحدة في Supabase > SQL Editor). آمن لإعادة التشغيل.

create table if not exists public.fcm_tokens (
  token text primary key,
  uid uuid not null references auth.users(id) on delete cascade,
  updated_at timestamptz not null default now()
);
create index if not exists fcm_tokens_uid on public.fcm_tokens (uid);

-- لا سياسات: الجدول لا يُقرأ ولا يُكتب إلا عبر الدوال أدناه أو بمفتاح service_role (الـ Edge Function)
alter table public.fcm_tokens enable row level security;

create or replace function public.save_fcm(p_token text)
returns void language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid();
begin
  if me is null then raise exception 'not_authenticated'; end if;
  if p_token is null or length(p_token) < 20 or length(p_token) > 4096 then raise exception 'bad_token'; end if;
  -- نفس الجهاز لو سجّل دخول بحساب ثاني ينتقل التوكن للحساب الجديد
  insert into fcm_tokens(token, uid) values (p_token, me)
  on conflict (token) do update set uid = excluded.uid, updated_at = now();
end $$;

create or replace function public.delete_fcm(p_token text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  delete from fcm_tokens where token = p_token and uid = auth.uid();
end $$;

revoke all on function public.save_fcm(text), public.delete_fcm(text) from public, anon;
grant execute on function public.save_fcm(text), public.delete_fcm(text) to authenticated;
