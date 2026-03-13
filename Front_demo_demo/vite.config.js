import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: "window", // ★ SockJS global 에러 해결 핵심
  },
  server: {
    proxy: {
      "/api": {
        target: "http://192.168.0.103:8080",
        changeOrigin: true,
        secure: false,
        timeout: 0, // SSE를 위한 타임아웃 무제한 설정
        proxyTimeout: 0,
      },
    },
    port: 5173,
  },
});
