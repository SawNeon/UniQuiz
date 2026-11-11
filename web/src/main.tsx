import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { registerSW } from "virtual:pwa-register";
import { App } from "./app/App";
import "./estilos/global.css";

const raiz = document.getElementById("raiz");
if (!raiz) {
  throw new Error("Elemento #raiz não encontrado em index.html");
}

registerSW({ immediate: true });

createRoot(raiz).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
