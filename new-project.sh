#!/bin/bash
# ========================
# Android Project Creator
# از این template یک پروژه جدید می‌سازد
# Usage: ./new-project.sh <ProjectName> <package.name> <destination_path>
# Example: ./new-project.sh MyChat com.example.mychat ~/workspace
# ========================

set -e

PROJECT_NAME=${1}
PACKAGE_NAME=${2}
DEST_DIR=${3:-$HOME/workspace}

if [ -z "$PROJECT_NAME" ] || [ -z "$PACKAGE_NAME" ]; then
    echo "Usage: $0 <ProjectName> <package.name> [destination_path]"
    echo "Example: $0 MyChat com.example.mychat ~/workspace"
    exit 1
fi

TEMPLATE_REPO="https://github.com/farshidb2011/android-starter.git"
OLD_PACKAGE="com.kalaban.farshidb2011"
OLD_NAME="Kalaban"
TARGET="$DEST_DIR/$PROJECT_NAME"

echo "🚀 Creating project: $PROJECT_NAME"
echo "   Package: $PACKAGE_NAME"
echo "   Path:    $TARGET"
echo ""

# Clone
git clone "$TEMPLATE_REPO" "$TARGET"
cd "$TARGET"

# حذف git history قدیمی
rm -rf .git
git init

# جایگزینی package name در تمام فایل‌ها
echo "🔄 Replacing package name..."
find . -type f \( -name "*.kt" -o -name "*.xml" -o -name "*.kts" -o -name "*.gradle" \) | while read file; do
    sed -i "s|$OLD_PACKAGE|$PACKAGE_NAME|g" "$file"
    sed -i "s|$OLD_NAME|$PROJECT_NAME|g" "$file"
done

# تغییر نام پوشه‌های سورس
OLD_PATH="app/src/main/java/com/kalaban/farshidb2011"
NEW_PATH="app/src/main/java/$(echo $PACKAGE_NAME | tr '.' '/')"
mkdir -p "$NEW_PATH"
cp -r "$OLD_PATH/." "$NEW_PATH/"
rm -rf "app/src/main/java/com"

# ساخت local.properties برای SDK
echo "sdk.dir=$HOME/Apps/Android/Sdk" > local.properties

# اولین commit
git add .
git commit -m "Initial commit from android-starter template"

echo ""
echo "✅ Project '$PROJECT_NAME' created at: $TARGET"
echo ""
echo "Next steps:"
echo "  cd $TARGET"
echo "  make run"
