import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MotionConfig } from "framer-motion";
import { RouterProvider } from "react-router";
import { naoAutenticado } from "../api/cliente";
import { rotas } from "./rotas";

const cliente = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (tentativas, erro) => !naoAutenticado(erro) && tentativas < 2,
      refetchOnWindowFocus: false,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={cliente}>
      <MotionConfig reducedMotion="user">
        <RouterProvider router={rotas} />
      </MotionConfig>
    </QueryClientProvider>
  );
}
