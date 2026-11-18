<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/aad8ec87-3ec6-457c-aa9f-037ae20866ed

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

---

# مذاكرتي — ملاحظات التحديث (Supabase + الأندرويد)

## ملفات الـ SQL (مجلد `supabase/`)
- `friends-setup.sql`: يفعّل الأصدقاء والرسائل والستريك المشترك، ويحذف الرسائل الأقدم من 7 أيام تلقائياً.
  شغّله مرة واحدة في Supabase > SQL Editor. آمن لإعادة التشغيل.
- الدوال تعتمد على `auth.uid()`، لذلك الأصدقاء يحتاج حساب مسجّل ومؤكَّد بالإيميل في Supabase.
  الدخول المحلي (زر «المتابعة بدون رمز») يشتغل للتطبيق نفسه، لكن الأصدقاء يعرض رسالة تطلب تسجيل الدخول بحساب مؤكَّد.
- إشعارات الـ Push اليومية تعتمد على جدول `push_subs` ودالتي `save_push` و`delete_push` من إعدادك السابق (`setup-ready.sql`)، وهي غير مضمّنة هنا.

## ما الذي تغيّر في `index.html`
- الأصدقاء يستخدم نسخة الويب الأحدث (الكود يتولّد على السيرفر ولا يُرسل الإيميل).
- محفوظ من نسخة الأندرويد: الإشعارات عبر `AndroidBridge`، وتخطي رمز التحقق، وتسجيل الدخول المحلي.
- رسالة جديدة تظهر بتبويب الأصدقاء إذا ما كان هناك حساب سحابي مؤكَّد.

## بناء الـ APK
1. افتح المشروع بـ Android Studio.
2. اضغط Build > Build APK(s).
3. عند أي تعديل على الموقع: انسخ `index.html` و`sw.js` الجديدين إلى `app/src/main/assets/` ثم أعد البناء.

## إشعارات الخلفية (أندرويد)
- إشعار انتهاء المؤقّت والتذكير اليومي يصلان حتى لو التطبيق بالخلفية أو مغلق، عبر منبّهات النظام (`AlarmManager`).
- الملفات الجديدة: `AlarmScheduler.kt` و`AlarmReceiver.kt` و`BootReceiver.kt` و`Notifier.kt`.
- بعد إعادة تشغيل الجهاز أو تحديث التطبيق، المنبّهات المحفوظة ترجع تلقائياً.
- ما يوصل إشعار مكرر: لما يكون التطبيق مفتوحاً تتولّى الصفحة التنبيه بنفسها.
- تنبيهات الأصدقاء («خلّص صديقك وقته» والرسائل) تحتاج التطبيق مفتوحاً أو بالخلفية القريبة، لأنها تعتمد على فحص دوري. للوصول الكامل وهو مغلق يلزم Firebase Cloud Messaging.
- ارتفع رقم الإصدار إلى 1.1 (versionCode 2) حتى يُثبَّت فوق النسخة القديمة.

---

# إشعارات الأصدقاء عبر FCM (بدون OneSignal)
1. `app/google-services.json` موجود (مشروع Firebase: `mudhakirati-814db`).
2. شغّل `supabase/fcm-setup.sql` في Supabase > SQL Editor.
3. انشر الدالة: `supabase functions deploy send-push --no-verify-jwt`
4. الأسرار: `supabase secrets set FCM_SERVICE_ACCOUNT="$(cat service-account.json)" WEBHOOK_SECRET=<كلمة-سر-عشوائية>`
   (لا تضع ملف الـ Service Account داخل المشروع ولا ترفعه).
5. Supabase > Database > Webhooks: جدول `fr_messages`، حدث Insert، النوع Supabase Edge Functions، الدالة `send-push`،
   وأضف هيدر `x-webhook-secret` بنفس قيمة WEBHOOK_SECRET.

---

# خط الإيموجي (Twemoji)
- الملف: `app/src/main/assets/fonts/Twemoji.Mozilla.ttf`، ومربوط في `index.html` بـ `@font-face` باسم `TwemojiMz` مع `unicode-range` للإيموجي فقط (الأرقام والنصوص تبقى بخطها).
- الترخيص: رسومات Twemoji تحت CC-BY 4.0 وتحتاج ذكر المصدر. التفاصيل في `assets/fonts/NOTICE.txt`.
- إشعارات النظام تعرض الإيموجي بخط الجوال ولا تتأثر.

---

# بناء الـ APK على GitHub (بدون كمبيوتر)
1. ارفع كل ملفات المشروع لمستودع **خاص (Private)** (المفتاح `debug.keystore` موجود عمداً حتى يبقى توقيع التطبيق ثابت وتتحدّث النسخ فوق بعض).
2. تبويب **Actions** ← **Build APK** ← **Run workflow** (يشتغل تلقائياً مع كل رفع أيضاً).
3. بعد ~5-10 دقائق: تبويب **Releases** ← آخر Build ← نزّل `mudhakirati.apk` وثبّته.
4. لو عندك نسخة قديمة من AI Studio احذفها أول (التوقيع يختلف).
