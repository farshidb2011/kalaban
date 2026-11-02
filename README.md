# Kalaban

اپ آفلاین مدیریت فروشگاه (`com.kalaban.farshidb2011`) با Jetpack Compose و Material 3.

نسخهٔ ۳: موتور قیمت اعشاری/فرمول، تغییر گروهی، چندکاربره اختیاری، گزارش‌ها، ورود CSV، ممیزی. جزئیات محصول و وضعیت در [`RUNBOOK.md`](RUNBOOK.md)، [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md) و [`PRODUCT_REPORT.md`](PRODUCT_REPORT.md).

## Prerequisites

- **Java 17** (`JAVA_HOME`؛ روی این ماشین معمولاً `~/.jdks/current`)
- **Android SDK** در `local.properties` → `sdk.dir=...` (gitignore شده)
- `adb` از `$ANDROID_HOME/platform-tools`

## Build and run

```bash
export JAVA_HOME=~/.jdks/current
export ANDROID_HOME=~/Apps/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.kalaban.farshidb2011/.MainActivity

# یا با Makefile
make build && make install && make run
```

## Tests, lint, release

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease   # R8؛ keepهای Gson در app/proguard-rules.pro
```

**هشدار:** هرگز `connectedAndroidTest` / `./gradlew connectedDebugAndroidTest` را روی گوشی فروشگاه اجرا نکنید — در پایان اپ را uninstall می‌کند و داده از بین می‌رود.

## Room schema and migration

- Schema نسخهٔ ۳: `app/schemas/com.kalaban.farshidb2011.data.local.KalabanDatabase/3.json`
- مهاجرت ۲→۳ فقط‌افزودنی است (`KalabanDatabase.kt`)
- راستی‌آزمایی روی کپی دیتابیس واقعی:

```bash
python3 tools/verify_room_migration.py /path/to/kalaban.db
```

پشتیبان دستگاه (نمونه):

```bash
BK="$HOME/kalaban-device-backups/$(date +%Y%m%d-%H%M%S)" && mkdir -p "$BK" \
  && adb shell am force-stop com.kalaban.farshidb2011 \
  && adb exec-out "run-as com.kalaban.farshidb2011 tar -cf - databases files cache 2>/dev/null" > "$BK/app-data.tar"
```

## Project layout

```
app/src/main/java/.../core/          Numbers, Money, PriceCalculator, FormulaEngine, Csv
app/src/main/java/.../data/          Room v3, repositories, help
app/src/main/java/.../domain/        Permission, Session
app/src/main/java/.../ui/            ۵ تب ادمین + پنل نمایش
app/src/main/assets/help/            help_fa.json / help_en.json
tools/verify_room_migration.py
```

## Notes

- Release signing فعلاً از debug keystore به‌عنوان placeholder استفاده می‌کند؛ قبل از انتشار عوض شود.
- `new-project.sh` برای کلون/تغییر package template است.
