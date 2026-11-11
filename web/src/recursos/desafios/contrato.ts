import type { Correcao, DesafioPublico } from "../../api/tipos";

export type DesafioDoTipo<T extends DesafioPublico["tipo"]> = Extract<DesafioPublico, { tipo: T }>;

export interface PropsDesafio<T extends DesafioPublico["tipo"], TResposta> {
  desafio: DesafioDoTipo<T>;
  resposta: TResposta | null;
  aoResponder: (resposta: TResposta) => void;
  correcao: Pick<Correcao, "correta"> | null;
  desabilitado: boolean;
}
