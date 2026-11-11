import { screen } from "@testing-library/react";
import type { Classificacao } from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { TelaDeClassificacao } from "./TelaDeClassificacao";

const temporada = {
  id: 1,
  nome: "Dezembro de 2026",
  inicio: "2026-12-01",
  fim: "2026-12-31",
  premioTexto: "Brinde",
};

describe("TelaDeClassificacao", () => {
  it("destaca o líder, as medalhas e o prêmio", async () => {
    const classificacao: Classificacao = {
      temporada,
      topo: [
        { posicao: 1, nome: "Ana Exemplo", setor: "TI", valor: 1240, souEu: false },
        { posicao: 2, nome: "Bruno Teste", setor: "TI", valor: 900, souEu: true },
        { posicao: 3, nome: "Carla", setor: "Faturamento", valor: 50, souEu: false },
      ],
      minhaPosicao: { posicao: 2, nome: "Bruno Teste", setor: "TI", valor: 900, souEu: true },
      pontosParaSubir: 341,
    };
    simularApi({ "GET /classificacao": { corpo: classificacao } });
    renderizar(<TelaDeClassificacao />);

    expect(await screen.findByText("Prêmio do 1º lugar: Brinde")).toBeInTheDocument();
    expect(screen.getByText(/Referência em Excelência/)).toBeInTheDocument();
    expect(screen.getByText(/Medalha de Prata/)).toBeInTheDocument();
    expect(screen.getByText(/Medalha de Bronze/)).toBeInTheDocument();
    expect(screen.getByText("1.240")).toBeInTheDocument();
    expect(screen.getByText("(você)")).toBeInTheDocument();
    expect(screen.queryByText(/Você está em/)).not.toBeInTheDocument();
  });

  it("mostra a posição de quem está fora do topo e quanto falta para subir", async () => {
    simularApi({
      "GET /classificacao": {
        corpo: {
          temporada,
          topo: [{ posicao: 1, nome: "Ana Exemplo", setor: "TI", valor: 100, souEu: false }],
          minhaPosicao: { posicao: 12, nome: "Eu", setor: "TI", valor: 70, souEu: true },
          pontosParaSubir: 31,
        },
      },
    });
    renderizar(<TelaDeClassificacao />);

    expect(await screen.findByText("Você está em 12º lugar")).toBeInTheDocument();
    expect(screen.getByText(/Faltam 31 pontos para subir/)).toBeInTheDocument();
  });

  it("convida a jogar quando ninguém pontuou", async () => {
    simularApi({ "GET /classificacao": { corpo: { temporada, topo: [] } } });
    renderizar(<TelaDeClassificacao />);

    expect(await screen.findByText(/Ninguém pontuou ainda/)).toBeInTheDocument();
    expect(screen.getByText("Faça uma lição para entrar na classificação.")).toBeInTheDocument();
  });

  it("avisa quando não há temporada aberta", async () => {
    simularApi({ "GET /classificacao": { corpo: { topo: [] } } });
    renderizar(<TelaDeClassificacao />);

    expect(await screen.findByText(/Nenhuma temporada aberta agora/)).toBeInTheDocument();
  });
});
