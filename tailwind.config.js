/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './.offline-build/index.html'
  ],
  theme: {
    extend: {
      fontFamily: {
        nunito: ['Nunito', 'sans-serif']
      }
    }
  },
  plugins: []
};
