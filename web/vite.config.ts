/// <reference types="vitest/config" />
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["icone.svg"],
      manifest: {
        name: "UniQuiz",
        short_name: "UniQuiz",
        description: "Desafios da TI: LGPD, inteligência artificial e segurança no dia a dia.",
        lang: "pt-BR",
        theme_color: "#00995d",
        background_color: "#ffffff",
        display: "standalone",
        start_url: "/",
        icons: [{ src: "icone.svg", sizes: "any", type: "image/svg+xml", purpose: "any" }],
      },
      workbox: {
        navigateFallbackDenylist: [/^\/api\//],
        runtimeCaching: [],
      },
    }),
  ],
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/testes/configuracao.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      exclude: [
        "src/**/*.test.{ts,tsx}",
        "src/testes/**",
        "src/main.tsx",
        "src/api/tipos.ts",
        "src/api/esquema.d.ts",
      ],
      reporter: ["text-summary", "text", "html"],
      thresholds: { statements: 90, branches: 85, functions: 85, lines: 90 },
    },
  },
});
