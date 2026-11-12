import { copyFile, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(fileURLToPath(new URL('..', import.meta.url)));
const sourceHtml = path.join(root, 'index.html');
const buildRoot = path.join(root, '.offline-build');
const sourceCss = path.join(root, 'src', 'styles.css');

const onlineResources = `    <!-- Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com"></script>
    <!-- Three.js para Gráficos 3D -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
    <!-- Ícones Phosphor -->
    <script src="https://unpkg.com/@phosphor-icons/web"></script>
    <!-- Google Fonts: Nunito (amigável e arredondada para estilo educativo) -->
    <link href="https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800;900&display=swap" rel="stylesheet">`;

const offlineResources = `    <!-- Recursos locais: incluídos no APK, sem CDN -->
    <link rel="stylesheet" href="./src/styles.css">
    <script src="/vendor/three.min.js"></script>`;

let html = await readFile(sourceHtml, 'utf8');
if (!html.includes(onlineResources)) {
  throw new Error('Não foi possível localizar o bloco original de dependências do site.');
}
html = html.replace(onlineResources, offlineResources);

const prohibited = [
  'cdn.tailwindcss.com',
  'cdnjs.cloudflare.com/ajax/libs/three.js',
  'unpkg.com/@phosphor-icons/web',
  'fonts.googleapis.com'
];
for (const url of prohibited) {
  if (html.includes(url)) {
    throw new Error(`Dependência externa permaneceu no HTML offline: ${url}`);
  }
}

await rm(buildRoot, { recursive: true, force: true });
await mkdir(path.join(buildRoot, 'src'), { recursive: true });
await mkdir(path.join(buildRoot, 'public', 'vendor'), { recursive: true });
await mkdir(path.join(buildRoot, 'public', 'ar-markers'), { recursive: true });
await copyFile(
  path.join(root, 'public', 'ar-markers', 'heart-marker.png'),
  path.join(buildRoot, 'public', 'ar-markers', 'heart-marker.png')
);
await writeFile(path.join(buildRoot, 'index.html'), html, 'utf8');
await copyFile(sourceCss, path.join(buildRoot, 'src', 'styles.css'));
console.log('Fonte isolada para o APK offline criada em .offline-build.');
