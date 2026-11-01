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
OLD_PKG_PATH="${OLD_PACKAGE//.//}"
NEW_PKG_PATH="${PACKAGE_NAME//.//}"
OLD_NAME_LOWER=$(echo "$OLD_NAME" | tr '[:upper:]' '[:lower:]')
NEW_NAME_LOWER=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]')

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

# جایگزینی package name / app name در سورس و بیلد
echo "🔄 Replacing package name..."
find . -type f \( -name "*.kt" -o -name "*.xml" -o -name "*.kts" -o -name "*.gradle" \) | while read -r file; do
    sed -i "s|$OLD_PACKAGE|$PACKAGE_NAME|g" "$file"
    sed -i "s|$OLD_NAME|$PROJECT_NAME|g" "$file"
done

# Makefile: APP_ID (and any other package refs) + logcat grep token
echo "🔄 Updating Makefile..."
sed -i "s|$OLD_PACKAGE|$PACKAGE_NAME|g" Makefile
sed -i "s|grep -i \"$OLD_NAME_LOWER\"|grep -i \"$NEW_NAME_LOWER\"|g" Makefile

# README: project name + package references for the new app
echo "🔄 Updating README..."
sed -i "s|$OLD_PACKAGE|$PACKAGE_NAME|g" README.md
sed -i "s|$OLD_NAME|$PROJECT_NAME|g" README.md

# جابه‌جایی پوشه‌های سورس: main + test + androidTest
echo "🔄 Moving package directories..."
for source_set in main test androidTest; do
    OLD_DIR="app/src/${source_set}/java/${OLD_PKG_PATH}"
    NEW_DIR="app/src/${source_set}/java/${NEW_PKG_PATH}"
    if [ -d "$OLD_DIR" ]; then
        mkdir -p "$(dirname "$NEW_DIR")"
        if [ "$OLD_DIR" != "$NEW_DIR" ]; then
            mkdir -p "$NEW_DIR"
            cp -a "$OLD_DIR"/. "$NEW_DIR"/
            rm -rf "$OLD_DIR"
            # prune empty leftover dirs under java/ (safe: only empty)
            find "app/src/${source_set}/java" -type d -empty -delete 2>/dev/null || true
        fi
    fi
done

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
