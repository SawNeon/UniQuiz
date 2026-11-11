import type { Correcao, TentativaIniciada } from "../../api/tipos";
import { desafioAtual, totalDePassos, useEstadoDaLicao } from "./estadoDaLicao";

const tentativa: TentativaIniciada = {
  tentativaId: 9,
  titulo: "IA no trabalho",
  valePontosIntegrais: true,
  desafios: [1, 2].map((id) => ({
    id,
    tipo: "VERDADEIRO_OU_FALSO" as const,
    enunciado: "Verdadeiro ou falso?",
    dificuldade: "FACIL" as const,
    conteudo: { afirmacao: `Afirmação ${id}` },
  })),
};

function correcao(correta: boolean, pendentes: number[] = []): Correcao {
  return {
    correta,
    respostaCerta: "Verdadeiro",
    explicacao: "",
    pontos: 0,
    bonusDeSequencia: 0,
    segundaChance: false,
    desafiosParaSegundaChance: pendentes,
  };
}

describe("estado da lição", () => {
  beforeEach(() => useEstadoDaLicao.getState().limpar());

  it("traz os desafios errados de volta no fim, uma vez só", () => {
    const estado = useEstadoDaLicao.getState();
    estado.iniciar(tentativa);

    estado.registrarCorrecao(correcao(false));
    expect(estado.avancar()).toBe("continuar");

    useEstadoDaLicao.getState().registrarCorrecao(correcao(true, [1]));
    expect(useEstadoDaLicao.getState().avancar()).toBe("continuar");
    expect(desafioAtual(useEstadoDaLicao.getState())?.id).toBe(1);
    expect(totalDePassos(useEstadoDaLicao.getState())).toBe(3);

    useEstadoDaLicao.getState().registrarCorrecao(correcao(false, [1]));
    expect(useEstadoDaLicao.getState().avancar()).toBe("concluir");
  });

  it("não troca a resposta depois de corrigida", () => {
    const estado = useEstadoDaLicao.getState();
    estado.iniciar(tentativa);
    estado.responder({ valor: true });
    estado.registrarCorrecao(correcao(true));

    useEstadoDaLicao.getState().responder({ valor: false });

    expect(useEstadoDaLicao.getState().resposta).toEqual({ valor: true });
  });
});
