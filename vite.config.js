import { defineConfig } from 'vite';
import tailwindcss from 'tailwindcss';
import autoprefixer from 'autoprefixer';

export default defineConfig({
  // O site do GitHub Pages mantém o index original. O APK é compilado
  // a partir da cópia isolada criada em .offline-build.
  base: './',
  publicDir: 'public',
  css: {
    postcss: {
      plugins: [
        tailwindcss({ config: './tailwind.config.js' }),
        autoprefixer()
      ]
    }
  },
  build: {
    outDir: '../dist',
    emptyOutDir: true,
    assetsDir: 'assets',
    sourcemap: false
  },
  server: {
    host: '0.0.0.0'
  }
});
