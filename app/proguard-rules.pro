# Keep Compose and reflection-friendly entry points for release minify.
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keep class com.kalaban.farshidb2011.data.local.entity.** { *; }
-keep class com.kalaban.farshidb2011.data.repository.ShopRepository$BackupPayload { *; }
-keepattributes Signature
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
