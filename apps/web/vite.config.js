import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
            '@yummy/ui': path.resolve(__dirname, '../../packages/ui/src'),
            '@yummy/api': path.resolve(__dirname, '../../packages/api/src'),
            '@yummy/utils': path.resolve(__dirname, '../../packages/utils/src'),
        },
    },
    server: {
        port: 3000,
        host: true,
    },
});
