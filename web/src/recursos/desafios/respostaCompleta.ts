import type { DesafioPublico, RespostaCompletarClicando, RespostaDoColaborador } from "../../api/tipos";

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
