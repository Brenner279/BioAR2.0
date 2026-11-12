import { copyFile, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const root = process.cwd();
const config = JSON.parse(await readFile(path.join(root, 'capacitor.config.json'), 'utf8'));
const appId = config.appId;
const appSegments = typeof appId === 'string' ? appId.split('.') : [];
if (appSegments.length < 2 || appSegments.some(part => !/^[A-Za-z][A-Za-z0-9]*$/.test(part))) {
  throw new Error('appId inválido em capacitor.config.json.');
}

const packagePath = appSegments.join(path.sep);
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
  const dependencyMarker = 'dependencies {';
  const dependencyIndex = gradle.indexOf(dependencyMarker);
  if (dependencyIndex < 0) throw new Error('Bloco dependencies não encontrado em android/app/build.gradle.');
  const insertAt = dependencyIndex + dependencyMarker.length;
  gradle = gradle.slice(0, insertAt) +
    "\n    // ARCore Augmented Images.\n    implementation 'com.google.ar:core:1.56.0'\n" +
    gradle.slice(insertAt);
}
await writeFile(gradlePath, gradle, 'utf8');

const manifestPath = path.join(root, 'android/app/src/main/AndroidManifest.xml');
let manifest = await readFile(manifestPath, 'utf8');
if (!manifest.includes('android.hardware.camera.ar')) {
  manifest = manifest.replace(
    '<application',
    '    <uses-feature android:name="android.hardware.camera.ar" android:required="false" />\n\n    <application'
  );
}
if (!manifest.includes('com.google.ar.core')) {
  manifest = manifest.replace(
    '</application>',
    '        <meta-data android:name="com.google.ar.core" android:value="optional" />\n' +
    '        <activity android:name=".arcore.MarkerScannerActivity" android:exported="false" android:screenOrientation="fullSensor" />\n' +
    '    </application>'
  );
} else if (!manifest.includes('MarkerScannerActivity')) {
  manifest = manifest.replace(
    '</application>',
    '        <activity android:name=".arcore.MarkerScannerActivity" android:exported="false" android:screenOrientation="fullSensor" />\n' +
    '    </application>'
  );
}
await writeFile(manifestPath, manifest, 'utf8');

const mainActivityPath = path.join(root, 'android/app/src/main/java', packagePath, 'MainActivity.java');
let mainActivity = await readFile(mainActivityPath, 'utf8');

const pluginImport = 'import ' + appId + '.arcore.BioARCorePlugin;';
if (!mainActivity.includes(pluginImport)) {
  const packageEnd = mainActivity.indexOf(';');
  if (packageEnd < 0) throw new Error('Declaração package não encontrada em MainActivity.java.');
  mainActivity = mainActivity.slice(0, packageEnd + 1) +
    '\n\nimport android.os.Bundle;\n' + pluginImport + '\n' +
    mainActivity.slice(packageEnd + 1);
}

if (!mainActivity.includes('registerPlugin(BioARCorePlugin.class)')) {
  const classStart = mainActivity.indexOf('public class MainActivity');
  const classOpen = mainActivity.indexOf('{', classStart);
  if (classStart < 0 || classOpen < 0) throw new Error('Classe MainActivity não encontrada.');
  const onCreate = '\n    @Override\n' +
    '    public void onCreate(Bundle savedInstanceState) {\n' +
    '        registerPlugin(BioARCorePlugin.class);\n' +
    '        super.onCreate(savedInstanceState);\n' +
    '    }\n';
  mainActivity = mainActivity.slice(0, classOpen + 1) + onCreate + mainActivity.slice(classOpen + 1);
}
await writeFile(mainActivityPath, mainActivity, 'utf8');

console.log('Plugin nativo ARCore, atividade de scanner e marcador offline configurados.');
