-- مذاكرتي: تفعيل الأصدقاء + الرسائل (شغّله مرة واحدة في Supabase > SQL Editor)
-- آمن لإعادة التشغيل أكثر من مرة.

-- 0) حذف النسخ القديمة من الدوال (اللي فيها p_email) حتى ما يصير تعارض
do $$
declare r record;
begin
  for r in
    select oid::regprocedure as sig
    from pg_proc
    where pronamespace = 'public'::regnamespace
      and (
        (proname = 'add_friend') or
        (proname = 'get_friends'   and pronargs <> 0) or
        (proname = 'remove_friend' and pronargs <> 1) or
        (proname = 'set_status'    and pronargs <> 1) or
        (proname = 'sync_profile'  and pronargs <> 2)
      )
  loop
    execute 'drop function ' || r.sig;
  end loop;
end $$;

create table if not exists public.fr_profiles (
  uid uuid primary key references auth.users(id) on delete cascade,
  code text unique not null,
  name text not null default '',
  days jsonb not null default '[]'::jsonb,
  now_text text,
  now_at timestamptz,
  updated_at timestamptz not null default now()
);

create table if not exists public.fr_friends (
  a uuid not null references auth.users(id) on delete cascade,
  b uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (a, b)
);

create table if not exists public.fr_blocks (
  blocker uuid not null references auth.users(id) on delete cascade,
  blocked uuid not null references auth.users(id) on delete cascade,
  primary key (blocker, blocked)
);

create table if not exists public.fr_messages (
  id bigserial primary key,
  from_uid uuid not null references auth.users(id) on delete cascade,
  to_uid uuid not null references auth.users(id) on delete cascade,
  kind text not null default 'text',
  body text not null,
  at timestamptz not null default now()
);
create index if not exists fr_messages_pair on public.fr_messages (from_uid, to_uid, id);
create index if not exists fr_messages_at on public.fr_messages (at);

create table if not exists public.fr_reads (
  reader uuid not null references auth.users(id) on delete cascade,
  peer uuid not null references auth.users(id) on delete cascade,
  upto bigint not null default 0,
  primary key (reader, peer)
);

-- صورة الحساب المصغّرة (JPEG صغير base64) تظهر في بطاقة طلب الصداقة
alter table public.fr_profiles add column if not exists photo text;

-- طلبات الصداقة: pending = بانتظار الرد، rejected = المستلم رفض (المرسل ما يعرف)
create table if not exists public.fr_requests (
  from_uid uuid not null references auth.users(id) on delete cascade,
  to_uid uuid not null references auth.users(id) on delete cascade,
  status text not null default 'pending',
  created_at timestamptz not null default now(),
  primary key (from_uid, to_uid)
);
create index if not exists fr_requests_to on public.fr_requests (to_uid, status);

-- سجل محاولات إرسال الطلبات (للحد من تخمين الأكواد بالقوة)
create table if not exists public.fr_attempts (
  uid uuid not null references auth.users(id) on delete cascade,
  at timestamptz not null default now()
);
create index if not exists fr_attempts_uid on public.fr_attempts (uid, at);

-- الجداول مقفولة؛ الوصول فقط عبر الدوال أدناه
alter table public.fr_requests enable row level security;
alter table public.fr_attempts enable row level security;
alter table public.fr_profiles enable row level security;
alter table public.fr_friends  enable row level security;
alter table public.fr_blocks   enable row level security;
alter table public.fr_messages enable row level security;
alter table public.fr_reads    enable row level security;

-- ========== sync_profile ==========
create or replace function public.sync_profile(p_name text, p_days jsonb)
returns text language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  c text;
  d jsonb;
  n int := 0;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  d := case when jsonb_typeof(coalesce(p_days,'[]'::jsonb)) = 'array' then p_days else '[]'::jsonb end;
  select code into c from fr_profiles where uid = me;
  if c is null then
    loop
      c := upper(substr(md5(random()::text || clock_timestamp()::text || me::text || n::text), 1, 8));
      exit when not exists (select 1 from fr_profiles where code = c);
      n := n + 1;
      if n > 50 then raise exception 'code_failed'; end if;
    end loop;
    insert into fr_profiles(uid, code, name, days)
    values (me, c, left(coalesce(p_name,''),40), d);
  else
    update fr_profiles
       set name = left(coalesce(p_name,''),40), days = d, updated_at = now()
     where uid = me;
  end if;
  return c;
end $$;

-- ========== set_status ==========
create or replace function public.set_status(p_text text)
returns text language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); t text := nullif(btrim(coalesce(p_text,'')),'');
begin
  if me is null then raise exception 'not_authenticated'; end if;
  update fr_profiles
     set now_text = left(t,80), now_at = case when t is null then null else now() end
   where uid = me;
  return 'ok';
end $$;

-- ========== set_photo ==========
create or replace function public.set_photo(p_photo text)
returns text language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); p text := nullif(btrim(coalesce(p_photo,'')),'');
begin
  if me is null then raise exception 'not_authenticated'; end if;
  if p is not null and (length(p) > 24000 or p !~ '^data:image/jpeg;base64,[A-Za-z0-9+/=]+$') then
    raise exception 'bad_photo';
  end if;
  update fr_profiles set photo = p where uid = me;
  return 'ok';
end $$;

-- ========== send_friend_request ==========
-- النتائج: sent | accepted (هو كان مرسل لك) | pending (أرسلت له قبل) | already | self | none
create or replace function public.send_friend_request(p_code text)
returns text language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); o uuid;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  -- تحديد المحاولات: كل محاولة (ناجحة أو فاشلة) تنحسب. أكثر من 15 خلال 10 دقائق = رفض
  delete from fr_attempts where at < now() - interval '1 day';
  if (select count(*) from fr_attempts where uid = me and at > now() - interval '10 minutes') >= 15 then
    raise exception 'rate_limited';
  end if;
  insert into fr_attempts(uid) values (me);

  select uid into o from fr_profiles where code = upper(btrim(coalesce(p_code,'')));
  if o is null then return 'none'; end if;
  if o = me then return 'self'; end if;
  if exists (select 1 from fr_blocks where (blocker = me and blocked = o) or (blocker = o and blocked = me)) then
    return 'none';
  end if;
  if exists (select 1 from fr_friends where a = me and b = o) then return 'already'; end if;

  -- لو هو أصلاً أرسل لي طلب، ارسالي له = موافقة
  if exists (select 1 from fr_requests where from_uid = o and to_uid = me) then
    insert into fr_friends(a,b) values (me,o) on conflict do nothing;
    insert into fr_friends(a,b) values (o,me) on conflict do nothing;
    delete from fr_requests where (from_uid = o and to_uid = me) or (from_uid = me and to_uid = o);
    return 'accepted';
  end if;
  -- أرسلت له قبل (حتى لو رفض، ما نزعجه ونرجع نفس الرد)
  if exists (select 1 from fr_requests where from_uid = me and to_uid = o) then return 'pending'; end if;
  if (select count(*) from fr_requests where from_uid = me) >= 30 then raise exception 'too_many_requests'; end if;

  insert into fr_requests(from_uid, to_uid) values (me, o);
  return 'sent';
end $$;

-- ========== get_requests ==========
create or replace function public.get_requests()
returns jsonb language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); i jsonb; o jsonb;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  select coalesce(jsonb_agg(jsonb_build_object(
           'code', p.code, 'name', p.name, 'photo', p.photo, 'at', r.created_at
         ) order by r.created_at desc), '[]'::jsonb)
    into i
    from fr_requests r join fr_profiles p on p.uid = r.from_uid
   where r.to_uid = me and r.status = 'pending'
     and not exists (select 1 from fr_blocks bl
                      where (bl.blocker = me and bl.blocked = r.from_uid) or (bl.blocker = r.from_uid and bl.blocked = me));
  -- المرفوضة تظهر للمرسل كأنها لسه معلّقة
  select coalesce(jsonb_agg(jsonb_build_object(
           'code', p.code, 'name', p.name, 'at', r.created_at
         ) order by r.created_at desc), '[]'::jsonb)
    into o
    from fr_requests r join fr_profiles p on p.uid = r.to_uid
   where r.from_uid = me
     and not exists (select 1 from fr_blocks bl
                      where (bl.blocker = me and bl.blocked = r.to_uid) or (bl.blocker = r.to_uid and bl.blocked = me));
  return jsonb_build_object('incoming', i, 'outgoing', o);
end $$;

-- ========== respond_request (قبول / رفض) ==========
create or replace function public.respond_request(p_code text, p_accept boolean)
returns text language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); o uuid;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  select uid into o from fr_profiles where code = upper(btrim(coalesce(p_code,'')));
  if o is null or not exists (select 1 from fr_requests where from_uid = o and to_uid = me) then
    return 'none';
  end if;
  if exists (select 1 from fr_blocks where (blocker = me and blocked = o) or (blocker = o and blocked = me)) then
    delete from fr_requests where (from_uid = o and to_uid = me) or (from_uid = me and to_uid = o);
    return 'none';
  end if;
  if coalesce(p_accept,false) then
    insert into fr_friends(a,b) values (me,o) on conflict do nothing;
    insert into fr_friends(a,b) values (o,me) on conflict do nothing;
    delete from fr_requests where (from_uid = o and to_uid = me) or (from_uid = me and to_uid = o);
  else
    update fr_requests set status = 'rejected' where from_uid = o and to_uid = me;
  end if;
  return 'ok';
end $$;

-- ========== cancel_request (المرسل يلغي طلبه) ==========
create or replace function public.cancel_request(p_code text)
returns text language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); o uuid;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  select uid into o from fr_profiles where code = upper(btrim(coalesce(p_code,'')));
  if o is not null then delete from fr_requests where from_uid = me and to_uid = o; end if;
  return 'ok';
end $$;

-- ========== remove_friend ==========
create or replace function public.remove_friend(p_code text)
returns text language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); o uuid;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  select uid into o from fr_profiles where code = upper(btrim(coalesce(p_code,'')));
  if o is null then return 'ok'; end if;
  delete from fr_friends where (a = me and b = o) or (a = o and b = me);
  delete from fr_messages where (from_uid = me and to_uid = o) or (from_uid = o and to_uid = me);
  delete from fr_reads where (reader = me and peer = o) or (reader = o and peer = me);
  return 'ok';
end $$;

-- ========== block_friend ==========
create or replace function public.block_friend(p_code text)
returns text language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); o uuid;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  select uid into o from fr_profiles where code = upper(btrim(coalesce(p_code,'')));
  if o is null or o = me then return 'ok'; end if;
  insert into fr_blocks(blocker, blocked) values (me, o) on conflict do nothing;
  delete from fr_requests where (from_uid = me and to_uid = o) or (from_uid = o and to_uid = me);
  delete from fr_friends where (a = me and b = o) or (a = o and b = me);
  delete from fr_messages where (from_uid = me and to_uid = o) or (from_uid = o and to_uid = me);
  delete from fr_reads where (reader = me and peer = o) or (reader = o and peer = me);
  return 'ok';
end $$;

-- ========== get_friends ==========
create or replace function public.get_friends()
returns jsonb language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); r jsonb;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  select coalesce(jsonb_agg(x order by x->>'name'), '[]'::jsonb) into r from (
    select jsonb_build_object(
      'code', p.code,
      'name', p.name,
      'photo', p.photo,
      'days', p.days,
      'now_text', p.now_text,
      'now_at', p.now_at,
      'unread', (select count(*) from fr_messages m
                  where m.from_uid = p.uid and m.to_uid = me
                    and m.id > coalesce((select upto from fr_reads where reader = me and peer = p.uid), 0)),
      'last', (select m.body from fr_messages m
                where m.from_uid = p.uid and m.to_uid = me
                  and m.id > coalesce((select upto from fr_reads where reader = me and peer = p.uid), 0)
                order by m.id desc limit 1)
    ) as x
    from fr_friends f join fr_profiles p on p.uid = f.b
    where f.a = me
  ) s;
  return r;
end $$;

-- ========== send_message ==========
create or replace function public.send_message(p_code text, p_text text, p_kind text default 'text')
returns jsonb language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); o uuid; t text := left(btrim(coalesce(p_text,'')),120); k text; mid bigint; ts timestamptz;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  if t = '' then raise exception 'empty'; end if;
  k := case when p_kind = 'cheer' then 'cheer' else 'text' end;
  select uid into o from fr_profiles where code = upper(btrim(coalesce(p_code,'')));
  if o is null or not exists (select 1 from fr_friends where a = me and b = o) then
    raise exception 'not_friends';
  end if;
  if (select count(*) from fr_messages where from_uid = me and at > now() - interval '10 seconds') >= 5 then
    raise exception 'rate_limited';
  end if;
  delete from fr_messages where at < now() - interval '7 days';
  insert into fr_messages(from_uid, to_uid, kind, body) values (me, o, k, t)
  returning id, at into mid, ts;
  return jsonb_build_object('id', mid, 'at', ts);
end $$;

-- ========== get_messages ==========
create or replace function public.get_messages(p_code text, p_after bigint default 0)
returns jsonb language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); o uuid; msgs jsonb; mx bigint; ru bigint;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  select uid into o from fr_profiles where code = upper(btrim(coalesce(p_code,'')));
  if o is null or not exists (select 1 from fr_friends where a = me and b = o) then
    return jsonb_build_object('msgs','[]'::jsonb,'read_upto',0);
  end if;
  select coalesce(jsonb_agg(jsonb_build_object(
           'id', m.id, 'mine', (m.from_uid = me), 'kind', m.kind, 'text', m.body, 'at', m.at
         ) order by m.id), '[]'::jsonb)
    into msgs
    from fr_messages m
   where m.id > coalesce(p_after,0)
     and m.at > now() - interval '7 days'
     and ((m.from_uid = me and m.to_uid = o) or (m.from_uid = o and m.to_uid = me));
  select max(id) into mx from fr_messages where from_uid = o and to_uid = me;
  if mx is not null then
    insert into fr_reads(reader, peer, upto) values (me, o, mx)
    on conflict (reader, peer) do update set upto = greatest(fr_reads.upto, excluded.upto);
  end if;
  select coalesce(upto,0) into ru from fr_reads where reader = o and peer = me;
  return jsonb_build_object('msgs', msgs, 'read_upto', coalesce(ru,0));
end $$;

-- صلاحيات التنفيذ للمستخدمين المسجّلين فقط
revoke all on function public.sync_profile(text,jsonb) from public, anon;
revoke all on function public.set_status(text) from public, anon;
drop function if exists public.add_friend(text);  -- الإضافة المباشرة ممنوعة، لازم طلب وموافقة
revoke all on function public.set_photo(text) from public, anon;
revoke all on function public.send_friend_request(text) from public, anon;
revoke all on function public.get_requests() from public, anon;
revoke all on function public.respond_request(text,boolean) from public, anon;
revoke all on function public.cancel_request(text) from public, anon;
revoke all on function public.remove_friend(text) from public, anon;
revoke all on function public.block_friend(text) from public, anon;
revoke all on function public.get_friends() from public, anon;
revoke all on function public.send_message(text,text,text) from public, anon;
revoke all on function public.get_messages(text,bigint) from public, anon;

grant execute on function public.sync_profile(text,jsonb) to authenticated;
grant execute on function public.set_status(text) to authenticated;
grant execute on function public.set_photo(text) to authenticated;
grant execute on function public.send_friend_request(text) to authenticated;
grant execute on function public.get_requests() to authenticated;
grant execute on function public.respond_request(text,boolean) to authenticated;
grant execute on function public.cancel_request(text) to authenticated;
grant execute on function public.remove_friend(text) to authenticated;
grant execute on function public.block_friend(text) to authenticated;
grant execute on function public.get_friends() to authenticated;
grant execute on function public.send_message(text,text,text) to authenticated;
grant execute on function public.get_messages(text,bigint) to authenticated;

-- حتى يتحدّث كاش PostgREST فوراً
notify pgrst, 'reload schema';

-- حذف الرسائل الأقدم من 7 أيام تلقائياً كل يوم الساعة 3 فجراً UTC
create extension if not exists pg_cron;
do $$
begin
  if exists (select 1 from cron.job where jobname = 'delete-old-messages') then
    perform cron.unschedule('delete-old-messages');
  end if;
  perform cron.schedule('delete-old-messages', '0 3 * * *',
    $q$ delete from public.fr_messages where at < now() - interval '7 days'; delete from public.fr_requests where created_at < now() - interval '30 days' $q$);
end $$;
