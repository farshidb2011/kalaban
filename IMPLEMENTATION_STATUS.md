# وضعیت پیاده‌سازی نسخهٔ ۳ کالابان و کارهای باقی‌مانده

> **آخرین به‌روزرسانی:** ۱۴۰۵/۰۸/۱۱ (2026-11-02)
>
> **وضعیت:** نیمه‌کاره. هستهٔ محاسباتی، لایهٔ داده، مهاجرت دیتابیس، ViewModelها و بیشتر صفحه‌ها نوشته شده‌اند، اما **پروژه فعلاً کامپایل نمی‌شود**، چون هنوز رشته‌های متنی، آیکون‌ها و فایل‌های راهنما اضافه نشده‌اند (جزئیات در بخش «کارهای باقی‌مانده»).
>
> **روی گوشی هنوز نسخهٔ قبلی (دیتابیس نسخهٔ ۲) نصب است.** تا وقتی همهٔ مراحل «راستی‌آزمایی» انجام نشده، نسخهٔ جدید را نصب نکنید.

این سند برای ادامهٔ کار در یک نشست جدید نوشته شده است: چه چیزی انجام شده، دقیقاً چه چیزی مانده، با چه دستوری بررسی شود و چه نکات ایمنی‌ای باید رعایت شود.

---

## ۱. نکات ایمنی (قبل از هر کاری بخوانید)

1. **دادهٔ واقعی روی گوشی است.** اولین اجرای نسخهٔ ۳ مهاجرت دیتابیس ۲→۳ را اجرا می‌کند. قبل از نصب، حتماً یک پشتیبان تازه بگیرید (پشتیبان قبلی مال ۱۴۰۵/۰۸/۱۱ ساعت ۰۱:۰۳ است و ممکن است داده بعد از آن تغییر کرده باشد):

   ```bash
   BK="$HOME/kalaban-device-backups/$(date +%Y%m%d-%H%M%S)" && mkdir -p "$BK" \
     && adb shell am force-stop com.kalaban.farshidb2011 \
     && adb exec-out "run-as com.kalaban.farshidb2011 tar -cf - databases files cache 2>/dev/null" > "$BK/app-data.tar" \
     && tar -tvf "$BK/app-data.tar"
   ```

   پشتیبان قبلی: `~/kalaban-device-backups/20261102-010322/app-data.tar`

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

### ابزار

- `tools/verify_room_migration.py`: راستی‌آزمایی مهاجرت بدون شبیه‌ساز (بخش ۱ را ببینید).

---

## ۳. کارهای باقی‌مانده (به ترتیب انجام)

### ۳.۱. سبز کردن بیلد

وضعیت فعلی: ۹۰۳ خطای کامپایل. تقریباً همه به‌خاطر رشته‌های تعریف‌نشده‌اند یا خطای استنتاج نوعی هستند که از همان‌ها ناشی می‌شود. خطاهای واقعیِ غیررشته‌ای فقط سه مورد است:

| خطا | راه‌حل |
|-----|--------|
| `ShopWorkers.kt:87` — `R.drawable` وجود ندارد | ساخت `res/drawable/ic_stat_kalaban.xml` (بخش ۳.۱.ب) |
| `HelpScreens.kt:138` — `R.drawable.ic_launcher_foreground` | ساخت آیکون تطبیقی (بخش ۳.۱.ب) |
| `Screens.kt:140` — import کهنهٔ `com.kalaban.farshidb2011.ui.ItemFormDraft` | حذف همین خط import |

#### الف) افزودن ۶۳۵ رشتهٔ متنی (انگلیسی در `values/strings.xml` و فارسی در `values-fa/strings.xml`)

فهرست به‌روز کلیدهای جاافتاده را همیشه با این دستور بگیرید. این دستور مرجع اصلی است و فهرست پایین فقط برای دیدن حجم کار است:

```bash
rg -o --no-filename "R\.string\.[a-z0-9_]+" app/src/main/java | sed 's/R.string.//' | sort -u > /tmp/used.txt
rg -o --no-filename 'name="[a-z0-9_]+"' app/src/main/res/values/strings.xml | sed 's/name="//;s/"//' | sort -u > /tmp/en.txt
rg -o --no-filename 'name="[a-z0-9_]+"' app/src/main/res/values-fa/strings.xml | sed 's/name="//;s/"//' | sort -u > /tmp/fa.txt
comm -23 /tmp/used.txt /tmp/en.txt   # جاافتاده در انگلیسی
comm -3  /tmp/en.txt  /tmp/fa.txt    # ناهماهنگی انگلیسی/فارسی (باید خالی باشد)
```

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

فهرست کامل ۶۳۵ کلید جاافتاده در لحظهٔ نوشتن این سند:

```text
a: about about_description about_sub action active actor_owner actor_system add_attribute add_role add_user
   adjusted_purchase admin_role_locked affected_products alert_backup_due alert_low_margin alert_low_stock
   alert_negative_profit alerts_channel alert_scheduled_text alert_scheduled_title alert_title all_actions
   all_products appearance_language apply_changes apply_changes_text apply_rounding app_tagline archive
   attribute_invalid attribute_key attribute_key_help attribute_options attribute_options_help attributes
   attributes_intro attributes_sub attribute_type attr_type_boolean attr_type_choice attr_type_number
   attr_type_text audit_balance_zero audit_catalog audit_config audit_export audit_formula_activate
   audit_formula_delete audit_formula_revert audit_formula_save audit_history_clear audit_import audit_log
   audit_login audit_login_failed audit_logout audit_log_sub audit_other audit_payment audit_pin_change
   audit_price_batch audit_price_rollback audit_price_schedule audit_price_schedule_cancel audit_product_archive
   audit_product_category audit_product_create audit_product_delete audit_product_duplicate audit_product_restore
   audit_product_update audit_protected_note audit_restore audit_role_delete audit_role_save audit_sale
   audit_settings audit_user_delete audit_user_save average_change average_margin
b: back_to_mapping backup_due_banner backup_help backup_reminder_days backup_reminder_help backup_sub barcode
   batch_applied batch_cancelled batch_failed batch_rolled_back batch_scheduled batch_title_amount
   batch_title_factor batch_title_percent batch_title_recalc batch_title_set below_break_even below_cost_count
   below_minimum_price biometric_owner_note brand break_even_price build_number build_type built_in bulk_applied
   bulk_applied_conflicts bulk_operation bulk_price_update bulk_price_update_sub bulk_scheduled bulk_scope
   business_address business_info business_name business_note business_note_help business_phone
c: calculated_price cancel_schedule cancel_schedule_title cap_bulk cap_data cap_formulas cap_pricing cap_products
   cap_reports cap_sales cap_security cash_price categories_sub change_category changed_products
   change_owner_pin change_percent change_pin choose_csv clear_search column_mapping columns compare_formulas
   computed_base_price condition_matched condition_not_matched conflicting_products copyright_notice copy_suffix
   cost_section created_at create_missing_categories create_missing_units credit_price credits_sub csv_settings
   current_pin current_result current_rule custom_range
d: daily_sales_chart dashboard dashboard_attention dashboard_greeting dashboard_today database_version date
   decimal_places decimal_places_help default_unit delete_attribute_text delete_attribute_title
   delete_formula_text delete_formula_title delete_products_text delete_products_title delete_role_title
   delete_user_title delimiter_comma delimiter_semicolon delimiter_tab description details difference
   discount_invalid discount_percent discount_rule_summary discounts_given discounts_intro display_panel_settings
   display_price_base display_price_cash display_price_mode display_price_mode_help display_show_images
   display_show_stock dollar_history_sub dollar_linked dollar_linked_help dollar_ratio download_template
   dup_create duplicate duplicates_help duplicates_policy dup_skip dup_update
e: edit_attribute edit_formula edit_role edit_user entity error_attribute_invalid error_attribute_required
   error_barcode_duplicate error_formula_required error_manual_price error_margin_range error_min_above_max
   error_name_taken error_negative error_pieces error_pin_in_use error_pricing_value error_required
   error_sku_duplicate error_title_required error_too_large error_unit_required excel_note export_csv
   export_done export_errors export_products export_products_help extra_costs extra_costs_help
f: ferr_argument_count ferr_division_by_zero ferr_empty ferr_expected_condition ferr_expected_number ferr_format
   ferr_invalid_argument ferr_missing_value ferr_parentheses ferr_too_complex ferr_too_large ferr_too_long
   ferr_unexpected_char ferr_unexpected_end ferr_unexpected_token ferr_unknown_function ferr_unknown_variable
   fixed_profit_amount fix_highlighted_fields flag_formula_error flag_low_margin flag_missing_cost
   flag_missing_price flag_negative_profit flag_outdated flag_out_of_bounds format_example formula
   formula_condition formula_condition_help formula_condition_optional formula_deleted formula_duplicated
   formula_examples formula_expression formula_expression_help formula_limits formula_limits_help formula_name
   formula_palette formula_palette_help formula_reverted formulas_apply_hint formula_saved formula_scope
   formulas_intro formula_test formula_valid formula_versions full_cost functions
g-h: general_settings general_settings_sub help_center help_center_sub hour_of_day
i: impact_summary import_another import_batch_title import_confirm_text import_done import_errors_note
   import_export import_export_sub import_file_summary import_intro import_preview import_products import_rows
   import_title_required inactive include_archived insert_into invalid_number invalid_products
   issue_barcode_conflict issue_duplicate_in_file issue_invalid_number issue_negative issue_pricing_mode
   issue_title_missing issue_unknown_category issue_unknown_unit
l: last_backup last_backup_info last_login licenses limited_by_max limited_by_min low_stock_sub
m: manage manual_price margin margin_distribution margin_percent markup markup_percent max_price max_stock
   method_dollar_manager metric_dollar_rate metric_low_margin metric_low_stock metric_month_profit
   metric_month_sales metric_negative_profit metric_open_credits metric_products metric_revenue
   metric_today_sales min_margin_help min_margin_not_set min_margin_percent min_price min_quantity mode_auto
   mode_auto_help mode_fixed_profit mode_fixed_profit_help mode_formula mode_formula_help mode_manual
   mode_manual_help mode_margin mode_margin_help mode_markup mode_markup_help more_details more_rows
n: name name_en name_fa net_profit never new_formula new_pin_optional new_price new_product no no_attributes
   no_audit_entries no_changes_needed no_discounts no_formulas no_help_results no_items_hint
   no_matching_products no_price_batches no_price_changes no_price_history no_search_results notes
   notification_settings notification_settings_sub notifications_intro notify_credit notify_low_margin
   notify_low_stock notify_negative_profit notify_scheduled not_mapped no_users number_format
o: old_price op_amount op_amount_help operation_failed op_factor op_factor_help op_percent op_percent_help
   op_recalculate op_recalculate_help op_set op_set_help other_cost outdated_prices_banner overhead
   overhead_help overhead_percent
p: package_name packaging_cost payment_recorded payment_type perm_audit perm_delete_products perm_edit_products
   perm_edit_sale_price perm_export perm_import permission_denied permissions_count perm_manage_catalog
   perm_manage_credits perm_manage_formulas perm_manage_prices perm_record_sales perm_settings perm_users
   perm_view_costs perm_view_products perm_view_reports pin pin_changed pin_change_failed pin_locked_out
   potential_profit pre_tax_price preview_changes preview_rollback price_batches price_batches_sub price_bounds
   price_bounds_help price_breakdown price_changes_count price_decreases price_edit_not_allowed price_history
   price_increases price_outdated_detail price_preview prices pricing_formulas pricing_formulas_sub
   pricing_method pricing_rules pricing_rules_sub pricing_value priority priority_help privacy_text
   privacy_title product product_duplicated products products_archived product_saved products_count
   products_delete_blocked products_deleted products_moved products_restored profit_impact_stock
   profit_partial_note purchase_dollar_rate purchase_price_piece
q: quantity_discount quantity_discounts quantity_discounts_sub quick_actions
r: range_30_days range_365_days range_7_days range_90_days range_this_month range_today rates_and_percentages
   recalc_prompt_banner recalc_prompt_setting recalc_prompt_text recalc_prompt_title recalculate_hint
   recent_price_changes reminders_sub report_price_changes report_pricing report_sales required respect_bounds
   restore restore_confirm_text restore_confirm_title restore_version_text restore_version_title review role
   role_in_use role_not_editable roles roles_sub rollback_apply rollback_done rollback_preview rollback_text
   rollback_title rollback_unavailable rounding_adjustment rounding_and_costs rounding_bankers rounding_down
   rounding_global rounding_mode rounding_nearest rounding_step rounding_step_help rounding_up rows_error
   rows_new rows_skip rows_update run_test
s: sale_recorded sample_product save_rounding schedule schedule_cancelled schedule_change
   scheduled_batches_banner scheduled_for schedule_help schedule_in_past search_help search_product_sale
   search_products_hint section_admin section_catalog section_data section_help section_pricing section_sales
   security security_lockout_note security_sub select_all selected_count selected_item sell_price_base
   signed_in_as sign_out sort sort_margin_asc sort_margin_desc sort_price_asc sort_price_desc sort_profit_desc
   sort_recent sort_stock_asc sort_stock_value sort_title source source_bulk source_create source_dollar_rate
   source_edit source_import source_recalculation source_rollback source_scheduled status status_active
   status_archived status_error status_new status_skip status_update stock_value_cost stock_value_retail
   success summary supplier
t: tags tags_help tax_collected template_saved this_formula thousands_separator tier_discount_applied
   top_products total_difference transport_cost
u-y: unchanged_products units_sub updated_at use_all_products user users users_intro users_sub validate_preview
   value variables variable_values version versions_count view view_license view_price_batches
   warn_below_cost warn_condition_error warn_formula_error warn_formula_inactive warn_formula_missing
   warn_invalid_rate warn_invalid_value warn_missing_cost warn_outside_bounds yes
```

#### ب) آیکون‌ها (فعلاً هیچ drawable‌ای وجود ندارد)

فایل‌هایی که باید ساخته شوند:

- `res/drawable/ic_launcher_background.xml`: زمینهٔ یک‌دست جوهری `#2F3E46` روی کل بوم 108×108.
- `res/drawable/ic_launcher_foreground.xml`: برچسب قیمت مسی `#C17F4E` با سوراخ واقعی (`fillType="evenOdd"`) و سه میلهٔ کرم `#F7F1E8` به نشانهٔ رشد و تحلیل قیمت.
- `res/drawable/ic_launcher_monochrome.xml`: همان شکل به‌صورت تک‌رنگ (سوراخ و میله‌ها هم بریده شوند)، برای آیکون‌های تم‌دار اندروید ۱۳ به بعد.
- `res/mipmap-anydpi-v26/ic_launcher.xml` و `ic_launcher_round.xml`: تگ `<adaptive-icon>` با `background` و `foreground` و `monochrome`. چون minSdk برابر ۲۶ است، PNGهای قدیمی دیگر هیچ‌وقت استفاده نمی‌شوند.
- `res/drawable/ic_stat_kalaban.xml`: آیکون اعلان 24dp، فقط سفید یا آلفا (طرح برچسب). بعد از ساخت آن، در `ReminderNotifications.show` هم `android.R.drawable.ic_dialog_info` را با همین آیکون عوض کنید.

مسیر پیشنهادی (viewport 108، داخل دایرهٔ امن به قطر 66):

```text
برچسب: M27,54 L43,37 L77,37 Q81,37 81,41 L81,67 Q81,71 77,71 L43,71 Z
سوراخ: M35.4,54 a3.6,3.6 0 1,0 7.2,0 a3.6,3.6 0 1,0 -7.2,0   (با برچسب در یک path و evenOdd)
میله‌ها: M50,58 h5 v7 h-5 Z   M59,53 h5 v12 h-5 Z   M68,47 h5 v18 h-5 Z
```

برای پیش‌نمایش، معادل SVG را بسازید و با ImageMagick به PNG تبدیل کنید (`convert icon.svg icon.png`)، سپس تصویر را ببینید. در `HelpScreens.kt`، `BrandMark` از `R.drawable.ic_launcher_foreground` روی رنگ `Ink40` استفاده می‌کند.

#### ج) بعد از کامپایل موفق، این موارد را هم بررسی و جمع‌وجور کنید

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

دو فایل باید ساخته شوند: `app/src/main/assets/help/help_fa.json` و `help_en.json`. ساختار هر فایل (کلاس‌ها در `data/help/HelpContent.kt`):

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

### ۳.۳. تست‌ها

- همهٔ تست‌های موجود دوباره پاس شوند (`./gradlew testDebugUnitTest`).
- تست‌های خالص پیشنهادی که هنوز نوشته نشده‌اند:
  - رفت‌وبرگشت `AppConfig.toMap/fromMap` و مقدار پیش‌فرض برای ورودی خراب.
  - `Permission.parse/encode`.
  - قفل تصاعدی `SessionManager` (با ساعت تزریقی).
  - `PricingRepository.decode` برای payload زمان‌بندی (یا جدا کردن encode/decode به یک تابع خالص).

### ۳.۴. راستی‌آزمایی (فقط چیزهایی را «تست‌شده» گزارش کنید که واقعاً اجرا شده‌اند)

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

### ۳.۵. مستندات

- `RUNBOOK.md` (فارسی):
  - معماری نسخهٔ ۳ و مدل داده.
  - موتور قیمت و فرمول.
  - مجوزها، تنظیمات (`ConfigKeys`)، گزارش‌ها، ورود و خروج داده.
  - ابزار مهاجرت، تصمیم‌های رفتاری (بخش ۴) و محدودیت‌ها (بخش ۵).
- `README.md`: دستورهای تست، lint، release، ابزار `tools/verify_room_migration.py`، پوشهٔ `app/schemas`، و هشدار دربارهٔ `connectedAndroidTest`.

### ۳.۶. گزارش نهایی

طبق درخواست اصلی، این بخش‌ها لازم است:

- مرور محصول، ویژگی‌ها، امکانات ادمین، موتور قیمت، UI/UX و هویت بصری.
- معماری، تست‌ها (با نتیجهٔ واقعی)، محدودیت‌ها، فایل‌های تغییرکرده.
- این‌که کدام نیازها با تنظیمات قابل انجام‌اند و کدام هنوز به برنامه‌نویسی نیاز دارند.

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
- هیچ‌کدام از صفحه‌های جدید هنوز روی دستگاه اجرا نشده‌اند.

## ۶. سؤال‌های باز برای مالک

1. پیش‌فرض پنل نمایش «قیمت پایه» بماند یا «قیمت نقد با مالیات»؟ (اگر درصد سود یا مالیات صفر نباشد، این دو متفاوت‌اند.)
2. برای ریال، تعداد اعشار ۰ و گام گرد کردن (مثلاً ۱۰۰۰) به‌عنوان پیش‌فرض تنظیم شود؟
3. آستانهٔ «حاشیهٔ سود کم» چند درصد باشد؟ (پیش‌فرض ۰، یعنی غیرفعال.)
4. قابلیت چندکاربره (کارکنان با نقش محدود) لازم است یا فقط پین مالک کافی است؟ پیاده‌سازی طوری است که بدون ساختن کاربر، تجربه همان حالت تک‌کاربرهٔ قبلی است.
