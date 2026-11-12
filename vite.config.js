import { defineConfig } from 'vite';

export default defineConfig({
  // Caminhos relativos permitem executar no Capacitor e também hospedar em GitHub Pages.
  base: './',
  publicDir: 'public',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    assetsDir: 'assets',
    sourcemap: false
  },
  server: {
    host: '0.0.0.0'
  }
});
