# وضعیت پیاده‌سازی نسخهٔ ۳ کالابان و کارهای باقی‌مانده

> **آخرین به‌روزرسانی:** ۱۴۰۵/۰۸/۱۲ (2026-11-03)
>
> **وضعیت:** کارهای برنامه‌ریزی‌شدهٔ بیلد/محتوا/جمع‌وجوری/تست/اسناد **انجام شد**. بیلد دیباگ و ریلیز سبز؛ lint سبز؛ ۳۵ تست واحد پاس؛ راهنما ۱۶ مقاله FA/EN؛ متن‌های UI بازبینی‌شده؛ نصب روی گوشی با مهاجرت موفق به `user_version=3`. 
>
> **ماندهٔ آگاهانه:** سناریوهای QA دستی که **پین مالک** می‌خواهند (تم کامل، دلار→rollback، فرمول روی کالای واقعی، نقش فروشنده، ۵ پین اشتباه) و سؤال‌های باز مالک در بخش ۶. موارد خارج از محدوده (XLSX/PDF، approval، اسکن دوربین) عمداً پیاده نشده‌اند.
>
> گزارش محصول: [`PRODUCT_REPORT.md`](PRODUCT_REPORT.md) · ران‌بوک: [`RUNBOOK.md`](RUNBOOK.md)

این سند برای ادامهٔ کار در یک نشست جدید نوشته شده: چه چیزی انجام شده، دقیقاً چه چیزی مانده، با چه دستوری بررسی شود و چه نکات ایمنی‌ای باید رعایت شود.

---

## ۰. خلاصهٔ پیشرفت (وضعیت جاری)

| حوزه | وضعیت | توضیح |
|------|--------|--------|
| هسته + تست واحد | ✅ | ۳۵ تست پاس (شامل DomainConfigTest) |
| Room v3 + مهاجرت | ✅ | ابزار Python روی v2 OK؛ روی دستگاه فعلاً v3 |
| UI/ViewModel صفحات | ✅ | کامپایل و نصب‌شده |
| سبز کردن بیلد | ✅ | رشته‌ها، آیکون‌ها، import، Switch، lint permission |
| نصب دیباگ | ✅ | 20261103-002208؛ لانچ بدون کرش بافر crash |
| کیفیت متن UI | ✅ | پولیش گستردهٔ FA + کلیدهای مهم EN |
| راهنمای درون‌برنامه | ✅ | ۱۶ مقاله در help_fa/en.json |
| جمع‌وجوری / dead code | ✅ | AutoMirrored، LocalResources، PricingEngine، BulkRequestCodec، حذف UI مرده |
| lint + release + تست اضافه | ✅ | lintDebug / assembleRelease / تست‌های خالص |
| QA دستی با پین مالک | 🟡 | نصب/لانچ/مهاجرت OK؛ سناریوهای پین‌دار با کاربر |
| README + گزارش نهایی | ✅ | README و PRODUCT_REPORT |

برآورد کلی: کد و محتوای نسخهٔ ۳ برای استفادهٔ عملی آماده است (~۹۰٪). باقی عمدتاً تأیید دستی مالک و تصمیم‌های باز بخش ۶ است.

---
## ۱. نکات ایمنی (قبل از هر کاری بخوانید)

1. **دادهٔ واقعی روی گوشی است.** اولین اجرای نسخهٔ ۳ مهاجرت دیتابیس ۲→۳ را اجرا می‌کند. قبل از هر نصب دوباره، حتماً یک پشتیبان تازه بگیرید (پشتیبان قبلی مال ۱۴۰۵/۰۸/۱۱ ساعت ۰۱:۰۳ است و ممکن است داده بعد از آن تغییر کرده باشد):

   ```bash
   BK="$HOME/kalaban-device-backups/$(date +%Y%m%d-%H%M%S)" && mkdir -p "$BK" \
     && adb shell am force-stop com.kalaban.farshidb2011 \
     && adb exec-out "run-as com.kalaban.farshidb2011 tar -cf - databases files cache 2>/dev/null" > "$BK/app-data.tar" \
     && tar -tvf "$BK/app-data.tar"
   ```

   پشتیبان‌ها: `~/kalaban-device-backups/20261102-010322/app-data.tar` (v2 برای تست مهاجرت) · `~/kalaban-device-backups/20261103-002208/` (بعد از نصب فعلی + اسکرین)

2. **هرگز `connectedAndroidTest` را روی گوشی کاربر اجرا نکنید.** این تسک در پایان، اپ را حذف می‌کند و همهٔ داده‌های فروشگاه از بین می‌رود.
3. نصب فقط با `adb install -r` باشد (حفظ داده). اگر خطای امضا (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) آمد، **اپ را حذف نکنید** و از کاربر بپرسید.
4. **مهاجرت دیتابیس:** هر تغییری در Entityها باید هم در `MIGRATION_2_3_STATEMENTS` (فایل `KalabanDatabase.kt`) اعمال شود و هم با ابزار زیر روی کپی دیتابیس واقعی تأیید شود:

   ```bash
   mkdir -p /tmp/kalaban-dbcheck && tar -xf ~/kalaban-device-backups/<آخرین>/app-data.tar -C /tmp/kalaban-dbcheck databases/
   python3 tools/verify_room_migration.py /tmp/kalaban-dbcheck/databases/kalaban.db
   ```

   این ابزار عین دستورهای مهاجرت را از کد Kotlin می‌خواند و نتیجه را با `app/schemas/.../3.json` (همان چیزی که Room موقع اجرا اعتبارسنجی می‌کند) مقایسه می‌کند. آخرین اجرا روی دادهٔ واقعی: **OK** (۴۸ دستور، ۱۹ جدول، بدون نقض کلید خارجی، دادهٔ قبلی دست‌نخورده). یک تست منفی هم نشان داد که ابزار خرابی را می‌گیرد.

---

## ۲. کارهای انجام‌شده

### هستهٔ محاسباتی (بدون وابستگی به اندروید، با تست واحد)

| فایل | محتوا |
|------|--------|
| `core/Numbers.kt` | پارس اعداد با ارقام فارسی/عربی و جداکنندهٔ هزارگان. قبلاً تایپ ارقام فارسی بی‌صدا ۰ ذخیره می‌شد. `String.toAmountOrNull()` |
| `core/Money.kt` | محاسبهٔ پول با `BigDecimal` (`MathContext.DECIMAL128`)، تعداد اعشار و جداکنندهٔ هزارگان قابل تنظیم، `Money.input()` برای فیلدهای ویرایشی، حد ذخیره‌سازی امن (`maxStorable`) |
| `core/pricing/RoundingPolicy.kt` | گرد کردن قیمت: نزدیک‌ترین / بانکی / رو به بالا / رو به پایین، با گام دلخواه (مثلاً ۱۰۰۰ ریال) |
| `core/pricing/PriceCalculator.kt` | موتور قیمت v2: بهای تمام‌شده (خرید × نسبت دلار + حمل + بسته‌بندی + سایر)، سربار، روش‌های AUTO / FORMULA / MARKUP / MARGIN / FIXED_PROFIT / MANUAL، انتخاب فرمول بر اساس اولویت و دامنه و شرط، حد پایین و بالای قیمت، قیمت پیشخوان (نقد و نسیه)، تحلیل سود (حاشیه، مارک‌آپ، نقطهٔ سربه‌سر)، تخفیف پلکانی، تعدیل گروهی. **بدون فرمول، خروجی دقیقاً برابر قاعدهٔ قدیمی است.** |
| `core/formula/FormulaEngine.kt` | زبان فرمول امن: lexer و parser و بررسی نوع و ارزیابی با `BigDecimal`. اجرای کد ممنوع است. محدودیت طول (۲۰۰۰) و عمق (۶۴) و مقدار (1e18) و توان (عدد صحیح ±۱۰)، خطا با موقعیت |
| `core/csv/Csv.kt` | خواندن و نوشتن CSV مطابق RFC 4180، تشخیص جداکننده، BOM برای اکسل، محافظت در برابر فرمول‌های مخرب در اکسل (`Csv.text`) |

تست‌ها: `FormulaEngineTest` (۷)، `PriceCalculatorTest` (۱۳)، `NumbersAndCsvTest` (۵) و `ExampleUnitTest` (۱۰). **هر ۳۵ تست در آخرین اجرا پاس شدند.** این اجرا قبل از شروع کار UI بود؛ الان چون main کامپایل نمی‌شود، تست‌ها هم اجرا نمی‌شوند.

### لایهٔ داده (Room نسخهٔ ۳)

- `data/local/entity/Entities.kt`:
  - `items` ستون‌های جدید گرفت: barcode (یکتا)، brand، supplier، description، notes، tags، status، transportCost، packagingCost، otherCost، dollarLinked، pricingMode، pricingValue، formulaId، minPrice، maxPrice، maxStock، createdAtEpochMs.
  - `sales.createdBy` اضافه شد.
  - `sale_lines` ستون‌های unitCost، taxAmount و discountAmount گرفت.
- `PricingEntities.kt`: جدول‌های pricing_formulas، formula_versions، price_batches، price_history، discount_rules، attribute_definitions و item_attribute_values.
- `AdminEntities.kt`: جدول‌های audit_log (فقط افزودنی)، app_config (کلید-مقدار)، roles و users.
- DAOها در `Daos.kt` (تغییر)، `PricingDaos.kt` و `AdminDaos.kt`.
  - `ItemDao.upsert` (که REPLACE بود و روی کالای فروخته‌شده کرش می‌داد) به `insert` تبدیل شد.
- `KalabanDatabase.kt`:
  - نسخهٔ ۳ با `exportSchema = true` (آرگومان KSP در `app/build.gradle.kts`).
  - `MIGRATION_2_3` فقط افزودنی است.
  - داده‌های اولیه: ۴ نقش پیش‌فرض و ۳ فرمول نمونهٔ غیرفعال (یک‌بار ساخته می‌شوند).
- Repositoryها:
  - `ConfigRepository`: تنظیمات تایپ‌شده با `AppConfig`، کلیدها در `ConfigKeys`.
  - `AuditRepository`: لاگ ممیزی (کدهای اقدام در `AuditAction`).
  - `PricingRepository`: CRUD فرمول با نسخه‌گذاری و بازگردانی، محاسبهٔ قیمت، پیش‌نمایش و اعمال تغییر گروهی در تراکنش با تشخیص تعارض، تاریخچه، بازگردانی (rollback) و زمان‌بندی.
  - `ProductRepository`: ذخیرهٔ کالا با اعتبارسنجی کامل، تکثیر، آرشیو، حذف امن و تغییر گروهی دسته.
  - `UserRepository`: ورود چندکاربره با قفل موقت بعد از ۵ تلاش ناموفق، کاربران، نقش‌ها و تغییر پین مالک.
  - `ReportRepository`: تحلیل قیمت، خلاصهٔ فروش، تغییرات قیمت و داشبورد.
  - `ImportExportRepository`: ورود CSV با نگاشت ستون‌ها و پیش‌نمایش و تکراری‌ها، خروجی کالاها و قالب.
  - `ShopRepository` (تغییر):
    - محاسبهٔ مجدد بی‌صدای همهٔ قیمت‌ها حذف شد و جایش `SettingsChange` آمد.
    - هر قلم فروش، بهای تمام‌شده و مالیات و تخفیف همان لحظه را ذخیره می‌کند.
    - پشتیبان نسخهٔ ۳ همهٔ جدول‌های جدید را دارد. پشتیبان‌های قدیمی هم با `InstanceCreator` درست بازیابی می‌شوند؛ بدون آن، `dollarLinked` به `false` تبدیل می‌شد.
- `data/help/HelpContent.kt`: بارگذاری و جستجوی راهنما از `assets/help/help_<lang>.json`.
- `domain/Permission.kt`: ۱۶ مجوز و نقش‌های پیش‌فرض (ADMIN، MANAGER، CASHIER، VIEWER).
- `domain/Session.kt`: `Actor`، `SessionUser` و `SessionManager` (قفل موقت تصاعدی).
- `data/prefs/SecurePrefs.kt`: `changePin` و `newSalt`.

### پس‌زمینه و راه‌اندازی

- `notifications/ShopWorkers.kt`: بررسی روزانهٔ وضعیت فروشگاه حوالی ساعت ۹ (کمبود موجودی، سود منفی، حاشیهٔ کم، پشتیبان عقب‌افتاده) و اجرای تغییرات قیمت زمان‌بندی‌شده.
- `ReminderNotifications.kt`: کارگر زمان‌بندی مجدد، کارهای زمان‌بندی‌شدهٔ قیمت را هم بعد از بوت دوباره ثبت می‌کند.
- `KalabanApp.kt`: ساختن و اتصال همهٔ Repositoryها و `appScope`.
- `app/proguard-rules.pro`: keep برای `BulkRequestDto` و `data.help.**` (به‌خاطر Gson).

### لایهٔ UI

- `ui/UiSupport.kt`:
  - `UiMessages` (پیام‌های Snackbar).
  - `GuardedViewModel`: بررسی مجوز در لایهٔ کسب‌وکار، و تبدیل خطاهای غیرمنتظره به پیام به‌جای کرش.
  - `appViewModelFactory`.
- ViewModelها:
  - `AppViewModel`: بازنویسی کامل با مجوزها، ممیزی، نشست، موتور قیمت اعشاری برای فروش (`quote`)، و `pendingRecalc` بعد از تغییر نرخ دلار.
  - `ProductsViewModel`، `PricingViewModel`، `AdminViewModel`، `ReportsViewModel` و `ImportViewModel`.
- `ui/theme/Theme.kt`: نقش‌های رنگی `SemanticColors` (success، warning، info) برای روشن و تاریک، از طریق `MaterialTheme.semantic`.
- `ui/components/Kit.kt`:
  - توکن‌های فاصله (`Spacing`)، `Tone` و `MetricCard`.
  - `StatusChip`، `InfoBanner`، `ConfirmDialog` و `MoneyText`.
  - `KeyValueRow`، نمودار میله‌ای (`BarChart`)، `SearchField` و `ExpandableSection`.
  - `NumberField`، `DropdownField` و `ChoiceChips`.
  - `JalaliDateField` و `JalaliRangePicker` (با کتابخانهٔ shamsi-picker که قبلاً بی‌استفاده بود).
- `RetailUi.kt`: `RetailTextField` حالا `isError` و `supportingText` دارد، و تب پایین برای ۵ تب جمع‌وجور شد.
- صفحه‌های جدید:
  - `DashboardScreen.kt`.
  - `ProductScreens.kt`: فهرست کالا با جستجو و فیلتر و انتخاب گروهی، جزئیات کالا با شکستِ قدم‌به‌قدم قیمت و تاریخچه، ویرایشگر با پیش‌نمایش زندهٔ قیمت.
  - `PricingScreens.kt`: فرمول‌ها، سازندهٔ فرمول، تغییر گروهی، تاریخچه و بازگردانی، تخفیف پلکانی.
  - `ReportScreens.kt`: سه تب فروش، تحلیل قیمت و تغییرات قیمت.
  - `AdminScreens.kt`: هاب مدیریت، قوانین قیمت، تنظیمات عمومی، امنیت، اعلان‌ها، کاربران، نقش‌ها، لاگ ممیزی، ویژگی‌های سفارشی.
  - `ImportScreens.kt` و `HelpScreens.kt` (راهنما و درباره).
  - `Labels.kt`: برچسب‌های مشترک.
- `Screens.kt`:
  - حذف شدند: `HomeScreen` (بی‌استفاده بود)، `AdminHub`، `ItemsScreen`، `SettingsScreen` و `ReportsScreen`، که همه با صفحه‌های جدید جایگزین شده‌اند.
  - به‌روز شدند:
    - `LockScreen`: ورود چندکاربره و شمارش معکوس قفل.
    - `DisplayPanel`: نام فروشگاه، نمایش قیمت پایه یا قیمت نقد، نمایش یا پنهان کردن موجودی و تصویر.
    - `NewSaleScreen`: قیمت با موتور اعشاری و تخفیف پلکانی، جستجوی کالا، تقویم شمسی، مجوز ویرایش قیمت و هشدار زیر حداقل یا زیر سربه‌سر.
    - `SaleDetailDialog`: مالیات، تخفیف، سود و کاربر ثبت‌کننده.
    - `BackupScreen`: تأیید قبل از بازیابی، تاریخ آخرین پشتیبان و لینک ورود و خروج CSV.
- `ui/navigation/Routes.kt` و `KalabanNavHost.kt` (بازنویسی):
  - پوستهٔ ادمین با ۵ تب: داشبورد | کالاها | فروش‌ها | گزارش‌ها | مدیریت. هر تب بر اساس مجوز نمایش داده می‌شود.
  - همهٔ مسیرهای جدید و میزبان سراسری Snackbar.
  - بعد از خروج از حساب، برگشت خودکار به پنل نمایش.


### بیلد سبز (۱۴۰۵/۰۸/۱۱→۱۲)

- ۶۳۵ کلید متنی به `values/strings.xml` و `values-fa/strings.xml` اضافه شد؛ بررسی `used ⊆ en` و `en ≡ fa` فعلاً خالی از جاافتادگی است.
- آیکون‌های `ic_launcher_background/foreground/monochrome`، `ic_stat_kalaban` و adaptive icon در `mipmap-anydpi-v26`.
- حذف import کهنهٔ `ItemFormDraft`؛ فیکس تایپ سوئیچ اعلان در `AdminScreens` (lambda باید `Unit` برگرداند).
- `assembleDebug` موفق؛ `testDebugUnitTest` پاس؛ یک‌بار `adb install -r` روی Redmi Note 8 Pro.
- stub اولیهٔ `assets/help/help_*.json` (فقط `getting_started`).

### ابزار

- `tools/verify_room_migration.py`: راستی‌آزمایی مهاجرت بدون شبیه‌ساز (بخش ۱ را ببینید).

---


---

## ۳. کارهای باقی‌مانده / پیگیری

> بیشتر موارد این بخش در دور ۱۴۰۵/۰۸/۱۲ بسته شدند. زیربخش‌ها برای تاریخچه و مرجع نگه داشته شده‌اند.

### ۳.۱. سبز کردن بیلد — ✅ انجام شد

**وضعیت قبلی این بخش (برای تاریخچه):** حدود ۹۰۳ خطای کامپایل به‌خاطر رشته‌های تعریف‌نشده و چند منبع گم‌شده؛ خطاهای غیررشته‌ای اصلی این‌ها بودند و رفع شدند:

| خطا | راه‌حل | وضعیت |
|-----|--------|--------|
| `ShopWorkers.kt` — `R.drawable.ic_stat_kalaban` | ساخت drawable اعلان | ✅ |
| `HelpScreens.kt` — `R.drawable.ic_launcher_foreground` | آیکون تطبیقی | ✅ |
| `Screens.kt` — import کهنهٔ `ItemFormDraft` | حذف import | ✅ |
| ۶۳۵ کلید `R.string.*` | افزودن EN + FA | ✅ |
| `AdminScreens` Switch lambda → `Job` | برگرداندن صریح `Unit` | ✅ |

دستور سلامت رشته‌ها (باید هر دو خروجی خالی باشند):

```bash
rg -o --no-filename "R\.string\.[a-z0-9_]+" app/src/main/java | sed 's/R.string.//' | sort -u > /tmp/used.txt
rg -o --no-filename 'name="[a-z0-9_]+"' app/src/main/res/values/strings.xml | sed 's/name="//;s/"//' | sort -u > /tmp/en.txt
rg -o --no-filename 'name="[a-z0-9_]+"' app/src/main/res/values-fa/strings.xml | sed 's/name="//;s/"//' | sort -u > /tmp/fa.txt
comm -23 /tmp/used.txt /tmp/en.txt
comm -3  /tmp/en.txt /tmp/fa.txt
```

#### الف) مرجع قالب رشته‌های دارای آرگومان (برای بازبینی متن‌ها الزامی است)

متن‌ها اضافه شده‌اند، ولی هنگام بازنویسی لحن باید قالب آرگومان‌ها حفظ شود:

قواعد نوشتن:

- آرگومان‌ها جایگاهی باشند (`%1$s`، `%1$d`). `Long` هم با `%d` کار می‌کند.
- در رشته‌هایی که آرگومان دارند، علامت درصد را `%%` بنویسید. در رشته‌های بدون آرگومان، `%` معمولی درست است.
- آپستروف را escape کنید (`\'`)، یا جمله را طوری بنویسید که آپستروف نداشته باشد.
- معنی هر کلید را از محل استفاده‌اش پیدا کنید (`rg "R.string.<key>" app/src/main/java`).
- متن‌ها کوتاه باشند و با لحن UI فعلی هماهنگ (مثل «ذخیره»، «انصراف»).

کلیدهایی که آرگومان دارند (قالب باید دقیقاً با کد بخواند):

| کلید | آرگومان‌ها |
|------|------------|
| `dashboard_greeting` | `%1$s` نام کاربر، `%2$s` تاریخ شمسی |
| `metric_today_sales`، `metric_open_credits`، `metric_revenue` | `%1$d` تعداد |
| `outdated_prices_banner`، `scheduled_batches_banner`، `profit_partial_note` | `%1$d` تعداد |
| `products_count`، `selected_count`، `delete_products_title`، `apply_changes`، `rollback_apply`، `import_rows`، `more_rows`، `versions_count` | `%1$d` تعداد |
| `products_archived`، `products_restored`، `products_moved`، `products_deleted`، `bulk_applied`، `export_done` | `%1$d` تعداد |
| `alert_low_stock`، `alert_negative_profit`، `alert_low_margin` | `%1$d` تعداد کالا |
| `products_delete_blocked` | `%1$d` حذف‌شده، `%2$d` مسدودشده |
| `bulk_applied_conflicts`، `rollback_done`، `alert_scheduled_text` | `%1$d` اعمال‌شده، `%2$d` تعارض |
| `permissions_count` | `%1$d` از `%2$d` |
| `import_file_summary` | `%1$d` ردیف، `%2$d` ستون |
| `import_done` | `%1$d` جدید، `%2$d` به‌روزشده، `%3$d` ردشده، `%4$d` خطا |
| `pin_locked_out` | `%1$d` ثانیه |
| `rollback_title` | `%1$d` شمارهٔ دسته |
| `copyright_notice` | `%1$d` سال |
| `ferr_format` | `%1$s` پیام خطا، `%2$d` موقعیت |
| `batch_title_recalc` | `%1$s` دامنه |
| `batch_title_percent`، `batch_title_amount`، `batch_title_factor`، `batch_title_set` | `%1$s` مقدار، `%2$s` دامنه (در percent بعد از `%1$s` باید `%%` بیاید) |
| `discount_rule_summary` | `%1$s` حداقل تعداد، `%2$s` درصد (بعدش `%%`) |
| `tier_discount_applied` | `%1$s` درصد (بعدش `%%`)، `%2$s` قیمت فهرست |
| `price_outdated_detail`، `below_minimum_price`، `below_break_even`، `product_saved` | `%1$s` مبلغ |
| `scheduled_for`، `last_backup_info` | `%1$s` تاریخ |
| `delete_user_title` | `%1$s` نام کاربر |
| `operation_failed` | `%1$s` متن خطا |


#### ب) آیکون‌ها — ✅ ساخته شد

فایل‌های زیر الان وجود دارند (دیگر «نبود drawable» صدق نمی‌کند):

- `res/drawable/ic_launcher_background.xml` (جوهر `#2F3E46`)
- `res/drawable/ic_launcher_foreground.xml` / `ic_launcher_monochrome.xml`
- `res/mipmap-anydpi-v26/ic_launcher.xml` و `ic_launcher_round.xml`
- `res/drawable/ic_stat_kalaban.xml`

**انجام شد:** `ReminderNotifications` و `ShopWorkers` هر دو از `ic_stat_kalaban` استفاده می‌کنند.

#### ج) جمع‌وجوری بعد از بیلد سبز — ✅ انجام شد

- آیکون‌های غیرآینه‌ای منسوخ (`TrendingUp`، `TrendingDown`، `ReceiptLong`، `Sort`، `Label`، `ShowChart` و…) را به نسخهٔ `Icons.AutoMirrored.*` تغییر دهید تا هشدار نماند.
- در `ui/LocaleProvider.kt`، علاوه بر `LocalContext` و `LocalConfiguration`، `LocalResources` را هم provide کنید. در Compose UI 1.11، `stringResource` از `LocalResources` می‌خواند.
- در `Screens.kt`: متغیر بی‌استفادهٔ `itemMenuOpen` در `NewSaleScreen` و importهای بی‌استفاده (`ExposedDropdownMenu*`، `RetailPeriodChip` و…) را حذف کنید.
- `core/PricingEngine.kt`:
  - توابع قدیمی `recalculateSellPrice`، `saleUnitPrice`، `installmentPrice` و `withTaxAndProfit` دیگر در کد اپ استفاده نمی‌شوند. فقط `purchaseRateAfterEdit` استفاده می‌شود.
  - تست‌های `ExampleUnitTest` را به `PriceCalculator` منتقل کنید و توابع قدیمی را حذف کنید.
  - توجه: رفتار نرخ خرید ≤ ۰ در موتور جدید عمداً فرق دارد: نسبت ۱ در نظر گرفته می‌شود، درصد مدیر اعمال می‌شود و هشدار `INVALID_PURCHASE_RATE` می‌آید.
- `AppViewModel.loadReport(range)` و `ShopRepository.salesReport(range)` و `ReportRange` احتمالاً دیگر جایی صدا زده نمی‌شوند. اگر بی‌استفاده‌اند، حذف کنید.
- `SuccessDialog`، `RetailMenuRow` و `RetailPeriodChip` در `RetailUi.kt` احتمالاً بی‌استفاده شده‌اند. حذف کنید.
- رشته‌های قدیمیِ احتمالاً بی‌استفاده را با گزارش lint (`UnusedResources`) پیدا و حذف کنید: `batch_ok`، `batch_pricing`، `price_factor`، `help_price_factor`، `choose_mode`، `offline_note`، `home`، `settings` و…


### ۳.۲. محتوای راهنمای درون‌برنامه

فایل‌های `help_fa.json` و `help_en.json` با **۱۶ مقاله** پر شده‌اند. ساختار (کلاس‌ها در `data/help/HelpContent.kt`):

```json
{ "articles": [ { "id": "...", "title": "...", "summary": "...", "keywords": ["..."],
  "sections": [ { "heading": "...", "paragraphs": ["..."], "steps": ["..."], "tips": ["..."] } ] } ] }
```

مقاله‌ها با این شناسه‌ها؛ فقط ویژگی‌هایی را توضیح دهید که واقعاً پیاده شده‌اند:

| id | موضوع |
|----|--------|
| `getting_started` | پنل نمایش و پنل ادمین، تنظیم پین، تب‌های پایین |
| `dashboard` | کارت‌ها، هشدارها، اقدام‌های سریع |
| `products` | افزودن و ویرایش کالا، تصویر، آرشیو و حذف و تکثیر، جستجو و فیلتر و مرتب‌سازی، انتخاب گروهی |
| `categories_units` | دسته‌ها، واحدها، ویژگی‌های سفارشی (متغیر `attr_<key>` در فرمول) |
| `pricing` | قاعدهٔ دلاری، هزینه‌های جانبی، سربار، روش‌های قیمت‌گذاری، فرق مارک‌آپ و حاشیهٔ سود، گرد کردن، حد قیمت، قیمت پیشخوان، شکستِ قیمت |
| `formulas` | متغیرها، توابع، عملگرها، شرط، اولویت و دامنه، تست و مقایسه، نسخه‌ها و بازگردانی، چند مثال |
| `bulk_updates` | پیش‌نمایش، اعمال، زمان‌بندی، تاریخچه و بازگردانی، رفتار بعد از تغییر نرخ دلار |
| `sales_credits` | ثبت فروش، تخفیف پلکانی، مجوز ویرایش قیمت، نسیه و یادآور |
| `reports` | تب فروش، تحلیل قیمت (پرچم‌ها، ستون‌ها، ارزش موجودی)، تغییرات قیمت، خروجی CSV |
| `import` | قالب، نگاشت ستون‌ها، تکراری‌ها، خطاها و خروجی خطاها |
| `export` | خروجی کالاها، گزارش‌ها و لاگ ممیزی، باز کردن در اکسل |
| `backup` | پشتیبان و بازیابی `.kalb`، یادآور پشتیبان |
| `users_permissions` | پین مالک، کاربران، نقش‌ها و مجوزها، قفل موقت بعد از تلاش ناموفق |
| `settings` | تنظیمات عمومی، قوانین قیمت، اعلان‌ها، امنیت |
| `theme` | حالت سیستم، روشن و تاریک؛ زبان و جهت متن |
| `troubleshooting` | قیمت به‌روز نیست، خطای فرمول، ورود CSV رد شد، اعلان نمی‌آید، فراموشی پین |

مرجع فرمول برای نوشتن راهنما:

- **متغیرها:**
  - `cost` (= خرید × نسبت + هزینه‌های جانبی)، `full_cost` (= cost × (۱ + سربار))
  - `purchase_price`، `purchase_rate`، `rate`، `ratio`
  - `transport`، `packaging`، `other_cost`، `extra_cost`، `overhead`، `overhead_pct`
  - `manager_pct`، `profit_pct`، `tax_pct`، `dollar_change_pct`، `installment_pct`
  - `value`، `current_price`، `min_price`، `max_price`، `stock`، `pieces`، `dollar_linked`، `category_id`
  - `attr_<key>` برای ویژگی‌های عددی
- **توابع:** `min`، `max`، `abs`، `round(x[, digits])`، `ceil`، `floor`، `roundto/ceilto/floorto(x, step)`، `clamp(x, lo, hi)`، `pct(x, p)`، `addpct(x, p)`، `subpct(x, p)`، `margin(cost, m)` و `if(cond, a, b)` (ارزیابی تنبل: فقط شاخهٔ انتخاب‌شده محاسبه می‌شود).
- **عملگرها:** `+ - * / ^`، مقایسه‌ها (`<`، `<=`، `>`، `>=`، `==`، `!=`) و `and/or/not` (همچنین `&& || !`). ارقام فارسی و `×` و `÷` هم پذیرفته می‌شوند.

### ۳.۳. تست‌ها — ✅

- همهٔ تست‌های موجود دوباره پاس شدند (`./gradlew testDebugUnitTest`).
- تست‌های خالص پیشنهادی که هنوز نوشته نشده‌اند:
  - رفت‌وبرگشت `AppConfig.toMap/fromMap` و مقدار پیش‌فرض برای ورودی خراب.
  - `Permission.parse/encode`.
  - قفل تصاعدی `SessionManager` (با ساعت تزریقی).
  - `PricingRepository.decode` برای payload زمان‌بندی (یا جدا کردن encode/decode به یک تابع خالص).

### ۳.۴. راستی‌آزمایی (فقط چیزهایی را «تست‌شده» گزارش کنید که واقعاً اجرا شده‌اند)

**انجام‌شده بدون پین:** بیلد+تست+lint+release، مهاجرت روی بکاپ v2، پشتیبان تازه، `adb install -r`، start، pid زنده، crash buffer خالی، اسکرین.
**مانده با پین مالک:** بند ۴ زیر.

1. اجرای بیلد و تست و lint:

   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
   ```

   بیلد release همان R8 است: keepهای Gson را بررسی کنید.
2. اجرای دوبارهٔ `tools/verify_room_migration.py` روی پشتیبان تازه.
3. روی گوشی، به این ترتیب:
   1. پشتیبان تازه (بخش ۱).
   2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
   3. `adb shell am start -W -n com.kalaban.farshidb2011/.MainActivity`.
   4. بررسی `adb logcat -b crash -d` و زنده بودن پروسه.
   5. گرفتن اسکرین‌شات.
4. آزمون دستی، که برای ورود به ادمین **پین مالک لازم است** و باید کاربر انجام دهد یا آن را در اختیار بگذارد:
   - **تم:** سیستم → روشن → گشتن در همهٔ تب‌ها و دیالوگ‌ها و منوها → تاریک بدون ری‌استارت → دوباره روشن → ری‌استارت → ماندگاری تم.
   - **نرخ دلار:** تغییر نرخ دلار در «قوانین قیمت» → پیام بازبینی → پیش‌نمایش → اعمال → در تاریخچهٔ قیمت → بازگردانی.
   - **فرمول:** ساخت فرمول، تست روی کالا، فعال‌سازی، محاسبهٔ مجدد.
   - **کالا:** ایجاد کالا، ویرایش کالای فروخته‌شده، آرشیو، حذف کالای فروخته‌شده که باید مسدود شود.
   - **فروش:** فروش با تخفیف پلکانی، گزارش فروش و سود.
   - **ورود و خروج داده:** خروجی CSV کالاها و ورود دوبارهٔ همان فایل با گزینهٔ «به‌روزرسانی موجودها»، پشتیبان و بازیابی (با دیالوگ تأیید).
   - **کاربران:** ساخت کاربر «فروشنده» و ورود با پین او؛ تب‌ها و دکمه‌های غیرمجاز نباید دیده شوند یا کار کنند.
   - **امنیت و ممیزی:** ۵ پین اشتباه پشت سر هم → قفل موقت؛ لاگ ممیزی؛ تغییر زبان فارسی و انگلیسی (جهت راست‌به‌چپ و چپ‌به‌راست).
5. آیکون اپ در لانچر گوشی دیده شود (اسکرین‌شات از صفحهٔ اصلی).

### ۳.۵. مستندات — ✅

- `RUNBOOK.md` (فارسی): وضعیت v3، ConfigKeys، مجوزها، مدل داده، ساخت/ایمنی:
  - معماری نسخهٔ ۳ و مدل داده.
  - موتور قیمت و فرمول.
  - مجوزها، تنظیمات (`ConfigKeys`)، گزارش‌ها، ورود و خروج داده.
  - ابزار مهاجرت، تصمیم‌های رفتاری (بخش ۴) و محدودیت‌ها (بخش ۵).
- `README.md`: دستورهای تست، lint، release، ابزار مهاجرت، schemas، هشدار `connectedAndroidTest`.

### ۳.۶. گزارش نهایی — ✅ در `PRODUCT_REPORT.md`

محتوای تحویلی:

- مرور محصول، ویژگی‌ها، امکانات ادمین، موتور قیمت، UI/UX و هویت بصری.
- معماری، تست‌ها (با نتیجهٔ واقعی)، محدودیت‌ها، فایل‌های تغییرکرده.
- این‌که کدام نیازها با تنظیمات قابل انجام‌اند و کدام هنوز به برنامه‌نویسی نیاز دارند.

---


---

## ۴. تغییرهای رفتاری و تصمیم‌های طراحی (برای اطلاع مالک)

1. **ذخیرهٔ تنظیمات دیگر بی‌صدا همهٔ قیمت‌ها را عوض نمی‌کند.** با تغییر نرخ دلار یا درصد مدیر، اپ پیشنهاد «بازبینی محاسبهٔ مجدد» می‌دهد (پیش‌نمایش → اعمال به‌صورت یک دسته، با امکان بازگردانی). این پیشنهاد در قوانین قیمت قابل خاموش کردن است.
2. «ضریب قیمت دسته‌ای» قدیمی به صفحهٔ «تغییر گروهی قیمت» منتقل شد (عملیات «ضریب»)، با پیش‌نمایش و ثبت در تاریخچه.
3. هر قلم فروش جدید، بهای تمام‌شده و مالیات و تخفیف همان لحظه را ذخیره می‌کند. پس گزارش سود فقط برای فروش‌های جدید دقیق است (گزارش تعداد قلم‌های بدون بها را جدا نشان می‌دهد).
4. مبالغ نمایشی به‌طور پیش‌فرض جداکنندهٔ هزارگان دارند. تعداد اعشار و جداکننده در تنظیمات عمومی قابل تغییر است.
5. فیلدهای عددی ارقام فارسی را می‌پذیرند.
6. بازیابی پشتیبان حالا دیالوگ تأیید دارد.
7. پوستهٔ ادمین ۵ تب دارد و تب «مدیریت» جای منوی سه‌نقطه و صفحهٔ AdminHub را گرفته است.
8. صفحهٔ ورود: پین کارکنان هم پذیرفته می‌شود. بعد از ۵ تلاش ناموفق، قفل موقت ۳۰ ثانیه‌ای فعال می‌شود که با تکرار تصاعدی بیشتر می‌شود. اثر انگشت همیشه به‌عنوان مالک وارد می‌کند.
9. پنل نمایش: نام فروشگاه در عنوان؛ نمایش «قیمت پایه» (پیش‌فرض، مثل قبل) یا «قیمت نقد با مالیات»؛ امکان پنهان کردن موجودی و تصویر.
10. پول با `BigDecimal` محاسبه می‌شود، ولی برای سازگاری با داده‌های موجود همچنان در ستون REAL ذخیره می‌شود. اعتبارسنجی، مقدار را در محدودهٔ بدون‌اتلاف (۱۵ رقم معنادار) نگه می‌دارد.

## ۵. محدودیت‌ها و موارد پیاده‌نشده

- خروجی XLSX و PDF پیاده نشده است. فقط CSV با BOM وجود دارد که اکسل آن را درست باز می‌کند.
- گردش کار تأیید (approval) برای تغییر قیمت پیاده نشده است.
- اسکن بارکد با دوربین پیاده نشده است. جستجو با بارکد تایپی یا اسکنر سخت‌افزاری کار می‌کند.
- بازیابی کامل پشتیبان، لاگ ممیزی را هم با نسخهٔ داخل پشتیبان جایگزین می‌کند. خودِ رویداد بازیابی بعد از آن ثبت می‌شود.
- اجرای تغییرات زمان‌بندی‌شده و هشدار روزانه به WorkManager وابسته است و ممکن است با بهینه‌سازی باتری بعضی برندها کمی دیر اجرا شود.
- مهاجرت دیتابیس به‌جای تست instrumented، با ابزار Python روی کپی دادهٔ واقعی تأیید شده است.
- صفحه‌های جدید نصب و لانچ شده‌اند؛ آزمون دستی سرتاسری با پین مالک هنوز با کاربر نهایی است.
- راهنمای درون‌برنامه ۱۶ مقاله دارد.
- متن‌های UI بازبینی شده‌اند؛ بهبود لحن بیشتر همیشه ممکن است.
- دیتابیس روی دستگاه بعد از نصب فعلی `user_version=3` است.

## ۶. سؤال‌های باز برای مالک

1. پیش‌فرض پنل نمایش «قیمت پایه» بماند یا «قیمت نقد با مالیات»؟ (اگر درصد سود یا مالیات صفر نباشد، این دو متفاوت‌اند.)
2. برای ریال، تعداد اعشار ۰ و گام گرد کردن (مثلاً ۱۰۰۰) به‌عنوان پیش‌فرض تنظیم شود؟
3. آستانهٔ «حاشیهٔ سود کم» چند درصد باشد؟ (پیش‌فرض ۰، یعنی غیرفعال.)
4. قابلیت چندکاربره (کارکنان با نقش محدود) لازم است یا فقط پین مالک کافی است؟ پیاده‌سازی طوری است که بدون ساختن کاربر، تجربه همان حالت تک‌کاربرهٔ قبلی است.
