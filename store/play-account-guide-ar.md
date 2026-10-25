# دليل نشر «نِداء» على Google Play — خطوة بخطوة

## أولاً: إنشاء حساب مطوّر جديد (25 دولار مرة وحدة)
1. افتح https://play.google.com/console/signup من الكمبيوتر وادخل بحساب Gmail (يفضّل حساب خاص بالتطبيقات).
2. اختر نوع الحساب: **«لنفسك» (Personal / Yourself)**.
3. اكتب اسم المطوّر اللي يطلع في المتجر، مثلاً: **وطن النهار** أو **Watan Al-Nahar**.
4. اكتب إيميل التواصل: aaaaaaaasa2112@gmail.com ورقم تلفونك (يرسلون لك رمز تحقق).
5. جاوب على أسئلة الخبرة (عدد التطبيقات، الخبرة… أي إجابة صادقة تمشي).
6. ادفع **25 دولار** ببطاقة (فيزا أو ماستركارد). هذي مرة وحدة بس.
7. **التحقق من الهوية:** يطلبون صورة البطاقة المدنية أو الجواز، والاسم والعنوان لازم يطابقون البطاقة. التحقق ياخذ من يوم إلى كم يوم.
8. لازم بعد تتحقق من جهاز أندرويد: نزّل تطبيق **Play Console** على جوالك وادخل بنفس الحساب لما يطلبونه.

## ثانياً: إنشاء التطبيق
1. في Play Console اضغط **Create app**.
2. App name: `نِداء: لاسلكي بدون إنترنت` · Default language: **Arabic – ar** · App · Free.
3. وافق على السياسات واضغط Create.

## ثالثاً: تعبئة «محتوى التطبيق» (App content)
كل الإجابات جاهزة في مجلد `store`:
- **Privacy policy:** `https://ahmadalfayed.github.io/nidaa/privacy.html`
- **Ads:** No · **App access:** كل الميزات متاحة بدون تسجيل دخول
- **Content rating:** من ملف `content-rating.md`
- **Target audience:** 13 سنة وفوق (لا تختار أقل من 13)
- **Data safety:** من ملف `data-safety.md`
- **Foreground service permissions:** من ملف `permissions.md` (يبون فيديو قصير يبين الإشعار والتطبيق شغّال والشاشة مطفية — صوّره بجوالك وارفعه يوتيوب «غير مدرج»)
- **Government / Financial / Health / News:** No

## رابعاً: بطاقة المتجر (Store listing)
- الاسم والوصف المختصر والكامل: انسخهم من `listing/ar.md`، وبعدين ضيف لغة **English (en-US)** وانسخ من `listing/en.md`.
- App icon: `graphics/icon-512.png`
- Feature graphic: `graphics/feature-graphic-1024x500.png`
- Phone screenshots: كل الصور في `screenshots/phone/` (7 صور).
- Category: **Communication** · Email: aaaaaaaasa2112@gmail.com

## خامساً: الاختبار المغلق (إجباري للحسابات الشخصية الجديدة)
Google تشترط: **12 مختبر على الأقل** يكونون مشتركين في الاختبار **14 يوم متواصلة** قبل لا تقدر تطلب النشر للكل.
1. Testing ← **Closed testing** ← Create track (أو استخدم Alpha).
2. **Testers:** سوّ قائمة إيميلات (Google Groups أو Email list) وحط فيها **12 إيميل Gmail أو أكثر** (يفضل 15 احتياط).
3. **Create release:** ارفع ملف **`Nidaa-1.3.0.aab`**. أول مرة يسألك عن **Play App Signing** — وافق (Use Google-generated key)، ومفتاحك الحالي يصير «مفتاح الرفع».
4. اكتب ملاحظة الإصدار: «أول إصدار: لاسلكي ومحادثة ومجموعات واستغاثة بدون إنترنت».
5. Review release ← Start rollout to Closed testing. المراجعة تاخذ من ساعات إلى كم يوم.
6. بعد الموافقة، انسخ **رابط الانضمام (opt-in link)** وأرسله للـ 12 شخص. كل واحد:
   - يفتح الرابط بنفس الإيميل اللي حطيته في القائمة ويضغط **Become a tester**.
   - ينزّل التطبيق من Google Play ويخليه في جواله **14 يوم بدون ما يطلع من الاختبار**.
   - يفتحه كم مرة ويجربه (Google تشوف إذا فعلاً استخدموه).
7. خلال الأيام هذي إذا صلّحت شي ارفع إصدار جديد (versionCode أكبر) — هذا شي زين يبين إنك تطوّر.
8. بعد **14 يوم** من ما صار عندك 12 مختبر، يطلع لك في Dashboard زر **Apply for production**. جاوب على الأسئلة (شلون جبت المختبرين، شنو تعلمت من ملاحظاتهم، ليش التطبيق جاهز). Google ترد خلال 7 أيام تقريباً.
9. بعد الموافقة: Production ← Create release ← ارفع نفس الـ AAB (أو أحدث) ← اختر الدول ← Start rollout. 🎉

> ملاحظة: أجهزة هواوي ما فيها Google Play، فالمختبرين لازم يكون عندهم جوالات فيها Google Play (سامسونج، شاومي، بكسل…).

## سادساً: متاجر ثانية (مجانية)
- **Huawei AppGallery:** https://developer.huawei.com/consumer/ar/ ← سجّل حساب مطور فردي (مجاني، يطلب هوية) ← AppGallery Connect ← My apps ← New app ← ارفع **`Nidaa-1.3.0.apk`** ← نفس الوصف والصور وسياسة الخصوصية. «نِداء» ما يحتاج خدمات Google، فيشتغل على هواوي ✅.
- **Samsung Galaxy Store:** https://seller.samsungapps.com ← حساب مجاني ← Add new application ← Android ← ارفع **`Nidaa-1.3.0.apk`** ونفس البيانات.
- في الاثنين استخدم نفس ملف الـ APK الموقّع بمفتاحك، ولا تغيّر المفتاح أبداً (لازم كل التحديثات تنوقّع بنفس المفتاح).

## مهم جداً
- **احفظ ملف المفتاح** (`nidaa-release.jks`) وكلمة السر في مكان آمن (مثلاً Google Drive خاص + نسخة على فلاش). إذا ضاع ما تقدر تحدّث التطبيق في هواوي وسامسونج أبد.
