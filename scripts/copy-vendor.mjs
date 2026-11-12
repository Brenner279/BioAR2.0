import { copyFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(fileURLToPath(new URL('..', import.meta.url)));
const source = path.join(root, 'node_modules', 'three', 'build', 'three.min.js');
const targetDir = path.join(root, '.offline-build', 'public', 'vendor');
const target = path.join(targetDir, 'three.min.js');

await mkdir(targetDir, { recursive: true });
await copyFile(source, target);
console.log('Three.js copiado para a versão Android offline.');
