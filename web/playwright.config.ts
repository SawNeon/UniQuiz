import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  use: {
    baseURL: process.env.UNIQUIZ_URL ?? "http://localhost:5173",
    locale: "pt-BR",
  },
  projects: [
    { name: "celular", use: { ...devices["Pixel 7"] } },
    { name: "computador", use: { ...devices["Desktop Chrome"] } },
  ],
  webServer: process.env.UNIQUIZ_URL
    ? undefined
    : { command: "npm run dev", url: "http://localhost:5173", reuseExistingServer: true },
});
