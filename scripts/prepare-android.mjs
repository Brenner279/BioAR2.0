import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const manifestPath = path.resolve('android/app/src/main/AndroidManifest.xml');
let manifest = await readFile(manifestPath, 'utf8');

// O APK funciona com os recursos empacotados. Retire a permissão de rede.
manifest = manifest.replace(
  /\s*<uses-permission[^>]*android:name="android\.permission\.INTERNET"[^>]*\/>/g,
  ''
);

const permissions = [
  '    <uses-permission android:name="android.permission.CAMERA" />',
  '    <uses-feature android:name="android.hardware.camera.any" android:required="false" />'
];

const additions = permissions.filter(item => {
  const name = item.match(/android:name="([^"]+)"/)?.[1];
  return name && !manifest.includes(name);
});

if (additions.length) {
  if (!manifest.includes('<application')) {
    throw new Error('Não foi encontrada a tag <application> no AndroidManifest.xml.');
  }
  manifest = manifest.replace(
    '<application',
    additions.join('\n') + '\n\n    <application'
  );
}

await writeFile(manifestPath, manifest, 'utf8');
console.log('Manifesto BioAR preparado: câmera opcional e sem permissão de internet.');
