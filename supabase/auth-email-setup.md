# إعداد إيميلات الرموز (إنشاء الحساب + نسيت كلمة السر)

التطبيق يرسل **رمز من 6 أرقام** في الحالتين. لازم 3 أشياء في Supabase:

## 1) SMTP مخصّص (الأهم)
الخدمة المدمجة في Supabase للتجربة فقط:
- ترسل فقط لإيميلات أعضاء فريق مشروعك (غيرهم يطلع خطأ `Email address not authorized`).
- حدّها 2 إيميل بالساعة لكل المشروع (يتشارك فيها التسجيل والنسيان والإعادة).

الحل: Authentication → Emails → SMTP Settings → Enable custom SMTP، وتربطه بأي مزوّد (Brevo / Resend / Gmail App Password …).
بعدها ارفع الحد من Authentication → Rate Limits.

## 2) قالب Confirm signup
Authentication → Emails → Templates → **Confirm signup**:

```html
<h2>مذاكرتي</h2>
<p>رمز تأكيد حسابك:</p>
<h1 style="letter-spacing:6px">{{ .Token }}</h1>
<p>ينتهي خلال ساعة. إذا ما طلبته تجاهل الرسالة.</p>
```

## 3) قالب Reset Password
نفس المكان → **Reset Password**:

```html
<h2>مذاكرتي</h2>
<p>رمز إعادة تعيين كلمة السر:</p>
<h1 style="letter-spacing:6px">{{ .Token }}</h1>
<p>ينتهي خلال ساعة. إذا ما طلبته تجاهل الرسالة.</p>
```

ملاحظة: لو القالب فيه `{{ .ConfirmationURL }}` فقط (الافتراضي) يوصل رابط بدل رمز، والرمز ما يظهر.
