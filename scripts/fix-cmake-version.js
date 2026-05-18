const fs = require('fs');
const path = require('path');

const patches = [
  {
    file: 'react-native-reanimated/android/build.gradle',
    from: /version = System\.getenv\("CMAKE_VERSION"\) \?: "3\.22\.1"/g,
    to: 'version = System.getenv("CMAKE_VERSION") ?: "3.31.6"',
  },
  {
    file: 'react-native-worklets/android/build.gradle',
    from: /version = System\.getenv\("CMAKE_VERSION"\) \?: "3\.22\.1"/g,
    to: 'version = System.getenv("CMAKE_VERSION") ?: "3.31.6"',
  },
  {
    file: 'react-native-gesture-handler/android/build.gradle',
    from: /externalNativeBuild \{\s*cmake \{\s*path "src\/main\/jni\/CMakeLists\.txt"\s*\}\s*\}/,
    to: 'externalNativeBuild {\n            cmake {\n                version "3.31.6"\n                path "src/main/jni/CMakeLists.txt"\n            }\n        }',
  },
];

patches.forEach(({ file, from, to }) => {
  const filePath = path.join(__dirname, '..', 'node_modules', file);
  if (!fs.existsSync(filePath)) return;

  const content = fs.readFileSync(filePath, 'utf8');
  const fixed = content.replace(from, to);

  if (content !== fixed) {
    fs.writeFileSync(filePath, fixed, 'utf8');
    console.log(`[fix-cmake] Patched: ${file}`);
  }
});
