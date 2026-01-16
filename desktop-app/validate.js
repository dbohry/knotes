const fs = require('fs');
const path = require('path');

console.log('🔍 Validating Desktop App Structure...\n');

// Check required files
const requiredFiles = [
  'main.js',
  'package.json',
  'index.html',
  'home.html',
  'js/script.js',
  'js/components.js',
  'css/style.css'
];

let allFilesPresent = true;

requiredFiles.forEach(file => {
  const filePath = path.join(__dirname, file);
  if (fs.existsSync(filePath)) {
    console.log(`✅ ${file} - Present`);
  } else {
    console.log(`❌ ${file} - Missing`);
    allFilesPresent = false;
  }
});

// Check API configuration
try {
  const scriptContent = fs.readFileSync(path.join(__dirname, 'js/script.js'), 'utf8');
  if (scriptContent.includes('https://notes.lhamacorp.com/api/notes')) {
    console.log('✅ API_BASE - Correctly configured for production');
  } else {
    console.log('❌ API_BASE - Not configured for production');
    allFilesPresent = false;
  }
} catch (error) {
  console.log('❌ Could not validate API configuration');
  allFilesPresent = false;
}

// Check package.json scripts
try {
  const packageJson = JSON.parse(fs.readFileSync(path.join(__dirname, 'package.json'), 'utf8'));
  const requiredScripts = ['start', 'build', 'build-all', 'build-win', 'build-mac', 'build-linux'];

  console.log('\n📦 Build Scripts:');
  requiredScripts.forEach(script => {
    if (packageJson.scripts && packageJson.scripts[script]) {
      console.log(`✅ npm run ${script} - Available`);
    } else {
      console.log(`❌ npm run ${script} - Missing`);
    }
  });
} catch (error) {
  console.log('❌ Could not validate package.json');
}

// Check if build output exists
if (fs.existsSync(path.join(__dirname, 'dist/linux-unpacked/knotes-desktop'))) {
  console.log('\n🏗️  Build Status:');
  console.log('✅ Linux build - Completed successfully');
  console.log('📂 Executable: ./dist/linux-unpacked/knotes-desktop');
}

console.log('\n🎉 Desktop App Validation Complete!');
console.log(allFilesPresent ? '✅ All components are properly configured' : '❌ Some issues found');

console.log('\n📋 Next Steps:');
console.log('1. Run "npm start" on a machine with GUI to test');
console.log('2. Run "npm run build-linux" to create AppImage');
console.log('3. Run "npm run build-win" on Windows to create installer');
console.log('4. Run "npm run build-mac" on macOS to create DMG');