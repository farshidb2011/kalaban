# Keep Compose and reflection-friendly entry points for release minify.
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Compose runtime / Material3 are covered by consumer ProGuard rules from the libraries.
# Add app-specific keep rules here as needed.
