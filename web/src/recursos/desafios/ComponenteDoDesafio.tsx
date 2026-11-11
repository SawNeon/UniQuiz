import type {
  Correcao,
  DesafioPublico,
  RespostaCompletarClicando,
  RespostaDoColaborador,
  RespostaMultiplaEscolha,
  RespostaVerdadeiroOuFalso,
} from "../../api/tipos";
import { CompletarClicando } from "./CompletarClicando";
import { MultiplaEscolha } from "./MultiplaEscolha";
import { VerdadeiroOuFalso } from "./VerdadeiroOuFalso";

interface Props {
  desafio: DesafioPublico;
  resposta: RespostaDoColaborador | null;
  aoResponder: (resposta: RespostaDoColaborador) => void;
  correcao: Pick<Correcao, "correta"> | null;
  desabilitado: boolean;
}

export function ComponenteDoDesafio({ desafio, resposta, ...resto }: Props) {
  switch (desafio.tipo) {
    case "MULTIPLA_ESCOLHA":
      return <MultiplaEscolha desafio={desafio} resposta={resposta as RespostaMultiplaEscolha | null} {...resto} />;
    case "VERDADEIRO_OU_FALSO":
      return (
        <VerdadeiroOuFalso desafio={desafio} resposta={resposta as RespostaVerdadeiroOuFalso | null} {...resto} />
      );
    case "COMPLETAR_CLICANDO":
      return (
        <CompletarClicando desafio={desafio} resposta={resposta as RespostaCompletarClicando | null} {...resto} />
      );
  }
}

export function respostaCompleta(desafio: DesafioPublico, resposta: RespostaDoColaborador | null): boolean {
  if (!resposta) {
    return false;
  }
  if (desafio.tipo === "COMPLETAR_CLICANDO") {
    const lacunas = (resposta as RespostaCompletarClicando).lacunas;
    return desafio.conteudo.lacunas.every((lacuna) => Boolean(lacunas[String(lacuna.id)]));
  }
  return true;
}
