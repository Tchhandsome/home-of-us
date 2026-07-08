import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  base: process.env.VITE_BASE_PATH ?? "/",
  server: {
    port: 5174,
    proxy: {
      "/api": "http://localhost:8080"
    }
  }
});
