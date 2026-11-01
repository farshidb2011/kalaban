SHELL := /bin/bash

# Android SDK & Java (fallback اگر در shell تنظیم نشده باشد)
export ANDROID_HOME ?= $(HOME)/Apps/Android/Sdk
export JAVA_HOME    ?= $(HOME)/.jdks/current
export PATH         := $(JAVA_HOME)/bin:$(PATH):$(ANDROID_HOME)/platform-tools

APP_ID := com.kalaban.farshidb2011

APK_DEBUG  := app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE := app/build/outputs/apk/release/app-release.apk
AAB_RELEASE := app/build/outputs/bundle/release/app-release.aab

.PHONY: help build release install run clean devices logs uninstall info

## پیش‌فرض: نمایش راهنما
help:
	@echo ""
	@echo "  ╔══════════════════════════════════════════╗"
	@echo "  ║       🤖 Android Build Commands          ║"
	@echo "  ╚══════════════════════════════════════════╝"
	@echo ""
	@echo "  make build       بیلد debug"
	@echo "  make install     بیلد + نصب روی گوشی"
	@echo "  make run         بیلد + نصب + اجرای اپ"
	@echo "  make release     بیلد release (APK)"
	@echo "  make bundle      بیلد release (AAB برای Play Store)"
	@echo "  make clean       پاک‌سازی فایل‌های بیلد"
	@echo "  make devices     نمایش دستگاه‌های متصل"
	@echo "  make logs        مشاهده لاگ اپ (logcat)"
	@echo "  make uninstall   حذف اپ از گوشی"
	@echo "  make info        اطلاعات محیط توسعه"
	@echo ""

## بیلد debug
build:
	@echo "🔨 Building debug APK..."
	./gradlew assembleDebug
	@echo "✅ APK: $(APK_DEBUG)"

## بیلد + نصب
install: build
	@echo "📲 Installing on device..."
	adb install -r $(APK_DEBUG)
	@echo "✅ Installed!"

## بیلد + نصب + اجرا
run: install
	@echo "🚀 Launching app..."
	adb shell am start -n $(APP_ID)/.MainActivity
	@echo "✅ App launched!"

## بیلد release
release:
	@echo "📦 Building release APK..."
	./gradlew assembleRelease
	@echo "✅ APK: $(APK_RELEASE)"

## بیلد AAB برای Play Store
bundle:
	@echo "📦 Building release AAB..."
	./gradlew bundleRelease
	@echo "✅ AAB: $(AAB_RELEASE)"

## پاک‌سازی
clean:
	@echo "🧹 Cleaning..."
	./gradlew clean
	@echo "✅ Cleaned!"

## نمایش دستگاه‌های متصل
devices:
	adb devices -l

## لاگ اپ
logs:
	adb logcat --pid=$$(adb shell pidof $(APP_ID)) 2>/dev/null || adb logcat | grep -i "kalaban"

## حذف اپ از گوشی
uninstall:
	adb uninstall $(APP_ID)
	@echo "✅ Uninstalled!"

## اطلاعات محیط
info:
	@echo "Java:    $$(java -version 2>&1 | head -1)"
	@echo "ADB:     $$(adb version | head -1)"
	@echo "SDK:     $$ANDROID_HOME"
	@echo "Gradle:  $$(./gradlew --version 2>/dev/null | grep 'Gradle ' | head -1)"
