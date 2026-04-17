import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import {fileURLToPath} from 'url';
import {dirname, resolve} from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
      '@yummy/ui': resolve(__dirname, '../../packages/ui/src'),
      '@yummy/api': resolve(__dirname, '../../packages/api/src'),
      '@yummy/utils': resolve(__dirname, '../../packages/utils/src'),
    },
  },
  server: {
    port: 1001,
    host: true,
  },
});
