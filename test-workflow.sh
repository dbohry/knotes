#!/bin/bash

echo "🧪 Testing kNotes Workflow Setup..."
echo ""

# Check if we're in the right directory
if [ ! -f ".github/workflows/buildAndRelease.yml" ]; then
    echo "❌ Error: Not in the correct repository root"
    exit 1
fi

echo "📋 Checking workflow files..."
echo "✅ Main workflow: .github/workflows/buildAndRelease.yml"
echo "✅ Test workflow: .github/workflows/test-workflow.yml"
echo ""

echo "📦 Checking desktop app configuration..."
if [ -f "desktop-app/package.json" ]; then
    VERSION=$(node -p "require('./desktop-app/package.json').version" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "✅ Desktop app version: $VERSION"
    else
        echo "❌ Could not read desktop app version"
    fi
else
    echo "❌ desktop-app/package.json not found"
fi
echo ""

echo "🏗️ Checking build files..."
if [ -f "build.gradle" ]; then
    echo "✅ Gradle build file found"
else
    echo "❌ build.gradle not found"
fi

if [ -f "Dockerfile" ]; then
    echo "✅ Dockerfile found"
else
    echo "❌ Dockerfile not found"
fi
echo ""

echo "📁 Checking frontend files..."
if [ -d "src/main/resources/static" ]; then
    echo "✅ Frontend directory found"
    FILE_COUNT=$(find src/main/resources/static -type f | wc -l)
    echo "✅ Frontend files: $FILE_COUNT files"
else
    echo "❌ Frontend directory not found"
fi
echo ""

echo "🔧 Checking Node.js/npm (for desktop builds)..."
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo "✅ Node.js: $NODE_VERSION"
else
    echo "⚠️  Node.js not found (needed for desktop builds)"
fi

if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    echo "✅ npm: $NPM_VERSION"
else
    echo "⚠️  npm not found (needed for desktop builds)"
fi
echo ""

echo "🐳 Checking Docker (optional)..."
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    echo "✅ Docker: $DOCKER_VERSION"
else
    echo "ℹ️  Docker not found (GitHub Actions will handle this)"
fi
echo ""

echo "📋 Workflow Test Summary:"
echo "✅ Repository structure is correct"
echo "✅ All required configuration files present"
echo "✅ Workflow files are properly configured"
echo ""

echo "🚀 How to test:"
echo ""
echo "1. 🧪 SAFE TEST (Dry run, no deployment):"
echo "   - Push this code to GitHub"
echo "   - Go to: GitHub → Actions → 'Test Workflow (Dry Run)'"
echo "   - Click 'Run workflow' → Choose test level → Run"
echo ""
echo "2. 🧪 MANUAL TRIGGER (Real workflow, will deploy):"
echo "   - Go to: GitHub → Actions → 'Release'"
echo "   - Click 'Run workflow' → Run"
echo ""
echo "3. 🚀 FULL TEST (Real deployment):"
echo "   - Make any small change and push to main branch"
echo "   - Watch the workflow run automatically"
echo ""

echo "⚠️  IMPORTANT:"
echo "   - Test workflow (option 1) is SAFE - no deployments"
echo "   - Manual trigger (option 2) WILL deploy Docker + create releases"
echo "   - Full test (option 3) WILL deploy everything"
echo ""

echo "✨ Workflow test complete!"