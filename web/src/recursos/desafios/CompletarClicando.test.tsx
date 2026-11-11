import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import type { RespostaCompletarClicando } from "../../api/tipos";
import { CompletarClicando } from "./CompletarClicando";
import type { DesafioDoTipo } from "./contrato";

const desafio: DesafioDoTipo<"COMPLETAR_CLICANDO"> = {
  id: 1,
  tipo: "COMPLETAR_CLICANDO",
  enunciado: "Complete a frase.",
  dificuldade: "FACIL",
  conteudo: {
    texto: "Use apenas ferramentas {{1}} pela TI.",
    lacunas: [{ id: 1, palavras: ["gratuitas", "aprovadas"] }],
  },
};

function Cenario({ aoMudar }: { aoMudar: (resposta: RespostaCompletarClicando) => void }) {
  const [resposta, setResposta] = useState<RespostaCompletarClicando | null>(null);
  return (
    <CompletarClicando
      desafio={desafio}
      resposta={resposta}
      correcao={null}
      desabilitado={false}
      aoResponder={(nova) => {
        setResposta(nova);
        aoMudar(nova);
      }}
    />
  );
}

describe("CompletarClicando", () => {
  it("preenche a lacuna com a palavra tocada e permite esvaziar", async () => {
    const usuario = userEvent.setup();
    const aoMudar = vi.fn();
    render(<Cenario aoMudar={aoMudar} />);

    await usuario.click(screen.getByRole("button", { name: "aprovadas" }));
    expect(aoMudar).toHaveBeenLastCalledWith({ lacunas: { "1": "aprovadas" } });

    await usuario.click(screen.getByRole("button", { name: /Lacuna preenchida com aprovadas/ }));
    expect(aoMudar).toHaveBeenLastCalledWith({ lacunas: {} });
  });

  it("não mostra qual palavra é a certa", () => {
    render(<Cenario aoMudar={vi.fn()} />);

    expect(screen.getByRole("group", { name: "Banco de palavras" })).toHaveTextContent("gratuitas");
    expect(screen.queryByText(/correta/i)).not.toBeInTheDocument();
  });
});
