# Keep Compose and reflection-friendly entry points for release minify.
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keep class com.kalaban.farshidb2011.data.local.entity.** { *; }
-keep class com.kalaban.farshidb2011.data.repository.ShopRepository$BackupPayload { *; }
# Gson-reflected payloads: scheduled price batch requests and the in-app help documents.
-keep class com.kalaban.farshidb2011.data.repository.BulkRequestDto { *; }
-keep class com.kalaban.farshidb2011.data.help.** { *; }
-keepattributes Signature
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
