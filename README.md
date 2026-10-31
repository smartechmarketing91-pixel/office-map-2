# خريطة نطاق المكتب – تطبيق أندرويد

## الطريقة 1: بناء APK بدون أي برامج (GitHub)
1. أنشئ حساباً مجانياً في github.com ثم مستودعاً جديداً (New repository).
2. ارفع كل محتويات هذا المجلد إلى المستودع (مع مجلد .github).
3. افتح تبويب Actions وانتظر انتهاء «Build APK» (دقيقتان تقريباً)، أو اضغط Run workflow.
4. افتح التشغيلة الناجحة ونزّل office-map-apk من قسم Artifacts، فك الضغط، وثبّت app-release.apk على هاتفك.

## الطريقة 2: Android Studio
1. نزّل leaflet.js (الإصدار 1.9.4) من cdnjs وضعه في app/src/main/assets/
2. افتح هذا المجلد في Android Studio ثم Build > Build APK(s).

ملاحظة: الملف officemap.jks هو مفتاح التوقيع. احتفظ به لتتمكن من تحديث التطبيق لاحقاً فوق النسخة المثبّتة دون فقدان بياناتك.
