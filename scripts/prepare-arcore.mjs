import { copyFile, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const root = process.cwd();
const config = JSON.parse(await readFile(path.join(root, 'capacitor.config.json'), 'utf8'));
const appId = config.appId;
if (!appId || !/^[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)+$/.test(appId)) {
  throw new Error('appId inválido em capacitor.config.json.');
}

const packagePath = appId.split('.').join(path.sep);
const javaRoot = path.join(root, 'android/app/src/main/java', packagePath, 'arcore');
await mkdir(javaRoot, { recursive: true });

for (const filename of ['BioARCorePlugin.java', 'MarkerScannerActivity.java']) {
  await copyFile(path.join(root, 'native/android/arcore', filename), path.join(javaRoot, filename));
}

const markerSource = path.join(root, 'dist/ar-markers/heart-marker.png');
const markerTarget = path.join(root, 'android/app/src/main/assets/ar_markers/heart-marker.png');
await mkdir(path.dirname(markerTarget), { recursive: true });
await copyFile(markerSource, markerTarget);

const gradlePath = path.join(root, 'android/app/build.gradle');
let gradle = await readFile(gradlePath, 'utf8');
if (!gradle.includes("com.google.ar:core:1.56.0")) {
  const match = gradle.match(/dependencies\\s*\\{/);
  if (!match) throw new Error('Bloco dependencies não encontrado em android/app/build.gradle.');
  const index = match.index + match[0].length;
  gradle = gradle.slice(0, index) + "\\n    // ARCore Augmented Images.\\n    implementation 'com.google.ar:core:1.56.0'\\n" + gradle.slice(index);
}
await writeFile(gradlePath, gradle, 'utf8');

const manifestPath = path.join(root, 'android/app/src/main/AndroidManifest.xml');
let manifest = await readFile(manifestPath, 'utf8');
if (!manifest.includes('android.hardware.camera.ar')) {
  manifest = manifest.replace('<application', '    <uses-feature android:name="android.hardware.camera.ar" android:required="false" />\\n\\n    <application');
}
if (!manifest.includes('com.google.ar.core')) {
  manifest = manifest.replace('</application>', '        <meta-data android:name="com.google.ar.core" android:value="optional" />\\n        <activity android:name=".arcore.MarkerScannerActivity" android:exported="false" android:screenOrientation="fullSensor" />\\n    </application>');
} else if (!manifest.includes('MarkerScannerActivity')) {
  manifest = manifest.replace('</application>', '        <activity android:name=".arcore.MarkerScannerActivity" android:exported="false" android:screenOrientation="fullSensor" />\\n    </application>');
}
await writeFile(manifestPath, manifest, 'utf8');

const mainActivityPath = path.join(root, 'android/app/src/main/java', packagePath, 'MainActivity.java');
let mainActivity = await readFile(mainActivityPath, 'utf8');
if (!mainActivity.includes('BioARCorePlugin')) {
  const packageLine = mainActivity.match(/package\\s+[\\w.]+;\\s*/);
  if (!packageLine) throw new Error('Declaração package não encontrada em MainActivity.java.');
  mainActivity = mainActivity.replace(packageLine[0], packageLine[0] + '\\nimport android.os.Bundle;\\nimport ' + appId + '.arcore.BioARCorePlugin;\\n');
}
if (!mainActivity.includes('registerPlugin(BioARCorePlugin.class)')) {
  const classMatch = mainActivity.match(/public\\s+class\\s+MainActivity[^{]*\\{/);
  if (!classMatch) throw new Error('Classe MainActivity não encontrada.');
  const index = classMatch.index + classMatch[0].length;
  mainActivity = mainActivity.slice(0, index) + '\\n    @Override\\n    public void onCreate(Bundle savedInstanceState) {\\n        registerPlugin(BioARCorePlugin.class);\\n        super.onCreate(savedInstanceState);\\n    }\\n' + mainActivity.slice(index);
}
await writeFile(mainActivityPath, mainActivity, 'utf8');

console.log('Plugin nativo ARCore, atividade de scanner e marcador offline configurados.');
