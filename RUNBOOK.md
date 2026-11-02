# ران‌بوک کالابان (Kalaban)

سند زندهٔ محصول و پیاده‌سازی. این فایل باید **پس از هر تغییر اعمال‌شده** در پروژه به‌روز شود.

**وضعیت فاز:** فاز یک تکمیل‌شده. **نسخهٔ ۳ قابل بیلد/نصب** است: تست واحد ۳۵/۳۵، `lintDebug` سبز، `assembleDebug`/`assembleRelease` موفق، راهنمای ۱۶ مقاله‌ای FA/EN، و روی Redmi Note 8 Pro با `adb install -r` نصب شده (مهاجرت ۲→۳ روی دستگاه انجام شده؛ دیتابیس فعلی user_version=۳).

> جزئیات باقی‌ماندهٔ QA دستی با پین و تاریخچهٔ کار: [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md)  
> گزارش محصول: [`PRODUCT_REPORT.md`](PRODUCT_REPORT.md)

### تصمیم طراحی UI (گزینه ۳ + اصلاحات نسخهٔ ۳)

- صفحهٔ پیش‌فرض فرود: **پنل نمایش (Display)**؛ ورود به ادمین با پین (چندکاربره) / اثر انگشت (همیشه به‌عنوان مالک)
- **Bottom nav ادمین (۵ تب، بر اساس مجوز):** داشبورد | کالاها | فروش‌ها | گزارش‌ها | مدیریت
- انتخاب تب: آیکون+برچسب رنگی با خط شاخص مسی (بدون مربع فیروزه‌ای)
- پالت: جوهر سربی `#2F3E46` + مسی `#C17F4E` + کرم `#F7F1E8` (بدون تیل/سیان)
- تم: سیستم / روشن / تاریک بدون ری‌استارت
- پین: تنظیم یک‌بارهٔ مالک؛ کارکنان با پین جدا؛ قفل موقت بعد از تلاش ناموفق
- تاریخچه دلار: جلالی در فارسی، میلادی در انگلیسی
- RTL فارسی حفظ شده

---

## ۱. مفهوم اپ

کالابان یک اپ مدیریت فروشگاه **آفلاین** است با دو سطح اصلی و در نسخهٔ ۳ امکان چندکاربرهٔ اختیاری:

| بخش | نقش | وضعیت |
|-----|-----|--------|
| **پنل ادمین** | مدیریت کامل (با مجوز) | ✓ نسخهٔ ۳ |
| **پنل نمایش** | مشاهده/جستجوی قیمت + تصویر | ✓ |

بدون ساختن کاربر اضافه، تجربه همان تک‌کاربرهٔ مالک است.

---

## ۲. مدیریت کالا ✓ نسخهٔ ۳

- عنوان، SKU، بارکد، برند، تأمین‌کننده، توضیح، یادداشت، برچسب، وضعیت (فعال/آرشیو)
- هزینه: خرید، نرخ دلار خرید، حمل/بسته‌بندی/سایر، وابستگی به دلار
- قیمت‌گذاری: AUTO / FORMULA / MARKUP / MARGIN / FIXED_PROFIT / MANUAL + حد پایین/بالا
- تصویر: دوربین + گالری (`TakePicture` / `GetContent` + FileProvider) با کپی پایدار
- واحدها، دسته‌ها، ویژگی‌های سفارشی (`attr_<key>` در فرمول)
- عملیات گروهی: آرشیو، بازیابی، جابه‌جایی دسته، حذف امن (کالای فروخته‌شده مسدود)

---

## ۳. تنظیمات و قوانین قیمت ✓

نرخ دلار، مالیات، سود، درصد تغییر دلار، درصد مدیر، اختلاف اقساطی، سربار، گرد کردن، اعشار و جداکنندهٔ هزارگان، ارز نمایشی، زبان، تم، اثر انگشت، یادآور پشتیبان، حالت نمایش قیمت در پنل مشتری.

**رفتار مهم نسخهٔ ۳:** ذخیرهٔ تنظیمات دیگر بی‌صدا همهٔ قیمت‌ها را عوض نمی‌کند؛ با تغییر نرخ دلار/درصد مدیر پیشنهاد بازبینی محاسبهٔ مجدد می‌آید (پیش‌نمایش → اعمال دسته‌ای، قابل بازگردانی).

### ۳.۱ کلیدهای `AppConfig` (`ConfigKeys`)

| کلید | معنی |
|------|------|
| `business.name/phone/address/note` | پروفایل فروشگاه / متن نمایش |
| `format.moneyDecimals` / `format.grouping` | اعشار و جداکنندهٔ هزارگان |
| `pricing.roundingMode` / `pricing.roundingStep` | گرد کردن |
| `pricing.overheadPct` / `pricing.minMarginPct` | سربار و آستانهٔ حاشیهٔ کم |
| `pricing.recalcPrompt` | پیشنهاد بازبینی بعد از تغییر دلار/مدیر |
| `display.priceMode` | `BASE` یا `CASH` روی پنل مشتری |
| `display.showStock` / `display.showImages` | نمایش موجودی و تصویر |
| `notify.*` | کمبود، سود منفی، حاشیه کم، نسیه، زمان‌بندی قیمت |
| `backup.reminderDays` / `backup.lastAt` | یادآور و زمان آخرین پشتیبان |
| `csv.delimiter` / `import.mapping` / `report.columns` | CSV و ستون‌ها |
| `search.recent` / `seed.formulas` | جستجوهای اخیر / بذر فرمول |

---

## ۴. موتور قیمت و فرمول ✓

- هسته: `PriceCalculator` + `RoundingPolicy` + `Money`/`Numbers` (BigDecimal)
- بدون فرمول فعال، خروجی با قاعدهٔ قدیمی سازگار است
- فرمول امن (بدون اجرای کد): متغیرها، توابع، شرط، اولویت، دامنه، نسخه و بازگردانی
- تغییر گروهی: درصد / مبلغ / ضریب / تنظیم مستقیم / محاسبهٔ مجدد — با پیش‌نمایش، تعارض، زمان‌بندی، تاریخچه و rollback
- تخفیف پلکانی تعدادی در مسیر فروش
- `PricingEngine.purchaseRateAfterEdit` فقط برای حفظ نرخ خرید هنگام ویرایش کالا مانده است

### ۴.۱ مرجع کوتاه فرمول

- **متغیرها:** `cost`، `full_cost`، `purchase_price`، `purchase_rate`، `rate`، `ratio`، `transport`، `packaging`، `other_cost`، `extra_cost`، `overhead`، `overhead_pct`، `manager_pct`، `profit_pct`، `tax_pct`، `dollar_change_pct`، `installment_pct`، `value`، `current_price`، `min_price`، `max_price`، `stock`، `pieces`، `dollar_linked`، `category_id`، `attr_<key>`
- **توابع:** `min` `max` `abs` `round` `ceil` `floor` `roundto/ceilto/floorto` `clamp` `pct` `addpct` `subpct` `margin` `if`
- **عملگرها:** `+ - * / ^`، مقایسه‌ها، `and/or/not`؛ ارقام فارسی و `×` `÷` پذیرفته می‌شوند

---

## ۵. تراکنش‌ها ✓

- نقد/نسیه، نام خریدار، تاریخ جلالی، سبد چندقلم
- هر قلم فروش: بهای تمام‌شده، مالیات و تخفیف همان لحظه ذخیره می‌شود
- نسیه: مانده، پرداخت جزئی، صفر با دلیل + اعلان یادآور (WorkManager)
- مجوز ویرایش قیمت فروش در نقش‌ها قابل کنترل است

---

## ۶. گزارش‌ها و داشبورد ✓

- داشبورد: کارت‌های امروز/ماه، هشدارها، اقدام سریع
- گزارش فروش، تحلیل قیمت (پرچم‌ها، حاشیه، ارزش موجودی)، تغییرات قیمت
- خروجی CSV گزارش‌ها و لاگ ممیزی

---

## ۷. داده و عملیات ✓

- پشتیبان/بازیابی `.kalb` (داخلی سریع + SAF) با دیالوگ تأیید بازیابی
- ورود CSV با نگاشت ستون، پیش‌نمایش، سیاست تکراری، خروجی خطا
- خروجی CSV کالاها و قالب
- تاریخچه نرخ دلار، کمبود موجودی، آفلاین کامل

---

## ۸. پنل نمایش ✓

جستجو + قیمت (پایه یا نقد) + تصویر اختیاری + موجودی اختیاری + نام فروشگاه.

---

## ۹. امنیت، کاربران و مجوزها ✓

- پین با نمک + SHA-256 کشیده‌شده (سازگاری با هش قدیمی)
- کاربران، نقش‌های پیش‌فرض ADMIN / MANAGER / CASHIER / VIEWER، قفل تصاعدی بعد از ۵ تلاش
- لاگ ممیزی فقط‌افزودنی
- fa/en + RTL/LTR
- راهنمای درون‌برنامه: `assets/help/help_fa.json` و `help_en.json` (۱۶ مقاله)

### ۹.۱ مجوزها (`Permission`)

`VIEW_PRODUCTS`، `EDIT_PRODUCTS`، `DELETE_PRODUCTS`، `VIEW_COSTS`، `MANAGE_PRICES`، `MANAGE_FORMULAS`، `MANAGE_CATALOG`، `RECORD_SALES`، `EDIT_SALE_PRICE`، `MANAGE_CREDITS`، `VIEW_REPORTS`، `EXPORT_DATA`، `IMPORT_DATA`، `MANAGE_SETTINGS`، `MANAGE_USERS`، `VIEW_AUDIT`

نام enum در دیتابیس نقش‌ها ذخیره می‌شود؛ تغییر نام = شکستن داده.

---

## ۱۰. معماری و مدل داده نسخهٔ ۳

```
core/            Numbers, Money, PricingEngine (compat),
                 pricing/PriceCalculator, RoundingPolicy,
                 formula/FormulaEngine, csv/Csv
data/local/      Room v3 (exportSchema) + MIGRATION_2_3
data/repository/ Shop, Product, Pricing (+ BulkRequestCodec),
                 Config, Audit, User, Report, ImportExport
data/help/       HelpContent از assets/help/help_<lang>.json
domain/          Permission, Session
notifications/   ReminderNotifications, ShopWorkers, BootReceiver
ui/              Nav (۵ تب) + ViewModelها + screens/components
tools/           verify_room_migration.py
app/schemas/     Room schema نسخهٔ ۳
```

### ۱۰.۱ جدول‌های مهم Room v3

- پایه: `items` (ستون‌های هزینه/قیمت‌گذاری گسترش‌یافته)، `sales` (+`createdBy`)، `sale_lines` (cost/tax/discount)، دسته‌ها، واحدها، تنظیمات قدیمی، تاریخچه دلار
- قیمت: `pricing_formulas`، `formula_versions`، `price_batches`، `price_history`، `discount_rules`، `attribute_definitions`، `item_attribute_values`
- ادمین: `audit_log`، `app_config`، `roles`، `users`

مهاجرت ۲→۳ فقط‌افزودنی است. ابزار Python روی کپی **نسخهٔ ۲** اجرا می‌شود؛ بعد از نصب موفق روی گوشی، `user_version` برابر ۳ است.

---

## ۱۱. ساخت، تست و نصب

```bash
export JAVA_HOME=~/.jdks/current
export ANDROID_HOME=~/Apps/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.kalaban.farshidb2011/.MainActivity
```

**هشدارها:**

- قبل از نصب روی گوشی با دادهٔ واقعی، پشتیبان بگیرید.
- هرگز `connectedAndroidTest` را روی گوشی کاربر اجرا نکنید (اپ را uninstall می‌کند).
- نصب فقط با `adb install -r`؛ در صورت `INSTALL_FAILED_UPDATE_INCOMPATIBLE` اپ را حذف نکنید.

پشتیبان دستگاه:

```bash
BK="$HOME/kalaban-device-backups/$(date +%Y%m%d-%H%M%S)" && mkdir -p "$BK" \
  && adb shell am force-stop com.kalaban.farshidb2011 \
  && adb exec-out "run-as com.kalaban.farshidb2011 tar -cf - databases files cache 2>/dev/null" > "$BK/app-data.tar"
```

راستی‌آزمایی مهاجرت (روی دیتابیس نسخهٔ ۲):

```bash
python3 tools/verify_room_migration.py /path/to/kalaban.db
```

آخرین پشتیبان‌های شناخته‌شده:

- `~/kalaban-device-backups/20261102-010322/` (قبل از موج v3؛ برای تست مهاجرت)
- `~/kalaban-device-backups/20261103-002208/` (بعد از نصب دیباگ فعلی + اسکرین QA)

---

## ۱۲. تصمیم‌های رفتاری نسخهٔ ۳

1. تغییر دلار/درصد مدیر → پیشنهاد بازبینی، نه بازنویسی بی‌صدا.
2. ضریب قیمت دسته‌ای داخل «تغییر گروهی» است.
3. سود دقیق‌تر برای فروش‌های جدید (با unitCost).
4. جداکنندهٔ هزارگان و اعشار قابل تنظیم؛ ارقام فارسی پذیرفته می‌شوند.
5. بازیابی پشتیبان دیالوگ تأیید دارد.
6. ورود چندکاربره + قفل تصاعدی؛ اثر انگشت = مالک.
7. پنل نمایش: قیمت پایه یا نقد؛ مخفی‌سازی موجودی/تصویر.

## ۱۳. محدودیت‌ها

- بدون XLSX/PDF (فقط CSV با BOM)
- بدون approval گردش‌کار قیمت و بدون اسکن بارکد دوربین
- WorkManager ممکن است روی OEMها تأخیر داشته باشد
- QA سناریوهای نیازمند پین مالک هنوز باید توسط کاربر نهایی تأیید شود

## ۱۴. فاز یک — مرجع تاریخی

فاز یک حداقلی قبلاً بسته شده بود. نسخهٔ ۳ روی همان پایه گسترش یافته است.

## ۱۵. به‌روزرسانی سند

با هر تغییر اعمال‌شده، این فایل، در صورت نیاز `IMPLEMENTATION_STATUS.md` و برای تحویل‌ها `PRODUCT_REPORT.md` را هم‌زمان به‌روز کنید.
