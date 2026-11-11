export type Dificuldade = "FACIL" | "MEDIO" | "DIFICIL";
export type TipoDesafio = "MULTIPLA_ESCOLHA" | "VERDADEIRO_OU_FALSO" | "COMPLETAR_CLICANDO";
export type StatusPublicacao = "RASCUNHO" | "PUBLICADO" | "ARQUIVADO";
export type SituacaoDaLicao = "CONCLUIDA" | "DISPONIVEL" | "BLOQUEADA";

export interface ItemDaLista {
  id: number;
  nome: string;
}

export interface Trofeu {
  codigo: string;
  nome: string;
  descricao: string;
  icone: string;
  conquistadoEm?: string;
}

export interface Certificado {
  codigo: string;
  moduloId: number;
  modulo: string;
  emitidoEm: string;
}

export interface Perfil {
  nome: string;
  setor: string;
  empresa: string;
  pontosNaTemporada: number;
  pontosTotais: number;
  nivel: number;
  pontosParaProximoNivel: number;
  diasSeguidos: number;
  maiorSequencia: number;
  trofeus: Trofeu[];
  certificados: Certificado[];
}

export interface LicaoNoCaminho {
  id: number;
  titulo: string;
  situacao: SituacaoDaLicao;
}

export interface ModuloNoCaminho {
  id: number;
  titulo: string;
  descricao: string;
  temMaterial: boolean;
  concluido: boolean;
  licoesConcluidas: number;
  licoes: LicaoNoCaminho[];
}

export interface Material {
  moduloId: number;
  titulo: string;
  conteudo: string;
}

export interface ConteudoMultiplaEscolhaPublico {
  alternativas: { id: string; texto: string }[];
}

export interface ConteudoVerdadeiroOuFalsoPublico {
  afirmacao: string;
}

export interface ConteudoCompletarClicandoPublico {
  texto: string;
  lacunas: { id: number; palavras: string[] }[];
}

interface DesafioBase {
  id: number;
  enunciado: string;
  dica?: string;
  dificuldade: Dificuldade;
}

export type DesafioPublico = DesafioBase &
  (
    | { tipo: "MULTIPLA_ESCOLHA"; conteudo: ConteudoMultiplaEscolhaPublico }
    | { tipo: "VERDADEIRO_OU_FALSO"; conteudo: ConteudoVerdadeiroOuFalsoPublico }
    | { tipo: "COMPLETAR_CLICANDO"; conteudo: ConteudoCompletarClicandoPublico }
  );

export interface RespostaMultiplaEscolha {
  alternativaId: string;
}

export interface RespostaVerdadeiroOuFalso {
  valor: boolean;
}

export interface RespostaCompletarClicando {
  lacunas: Record<string, string>;
}

export type RespostaDoColaborador = RespostaMultiplaEscolha | RespostaVerdadeiroOuFalso | RespostaCompletarClicando;

export interface TentativaIniciada {
  tentativaId: number;
  titulo: string;
  valePontosIntegrais: boolean;
  desafios: DesafioPublico[];
}

export interface Correcao {
  correta: boolean;
  motivoDoErro?: string;
  respostaCerta: string;
  explicacao: string;
  pontos: number;
  bonusDeSequencia: number;
  segundaChance: boolean;
  desafiosParaSegundaChance: number[];
}

export type MotivoDosPontos = "ACERTOS" | "LICAO_PERFEITA" | "PRIMEIRA_LICAO_DO_DIA" | "AJUSTE_DO_ADMIN";

export interface ResultadoDaLicao {
  acertos: number;
  total: number;
  perfeita: boolean;
  tempoMs: number;
  ganhos: { motivo: MotivoDosPontos; pontos: number }[];
  pontosGanhos: number;
  diasSeguidos: number;
  trofeusNovos: Trofeu[];
  moduloConcluido?: { id: number; titulo: string };
}

export interface Temporada {
  id: number;
  nome: string;
  inicio: string;
  fim: string;
  premioTexto?: string;
}

export interface Posicao {
  posicao: number;
  nome: string;
  setor: string;
  valor: number;
  souEu: boolean;
}

export interface Classificacao {
  temporada?: Temporada;
  topo: Posicao[];
  minhaPosicao?: Posicao;
  pontosParaSubir?: number;
}

export interface ValidacaoDeCertificado {
  codigo: string;
  nome: string;
  modulo: string;
  emitidoEm: string;
}

export interface LicaoParaAdmin {
  id: number;
  moduloId: number;
  titulo: string;
  ordem: number;
  status: StatusPublicacao;
  desafiosPublicados: number;
}

export interface ModuloParaAdmin {
  id: number;
  titulo: string;
  descricao: string;
  materialEstudo?: string;
  ordem: number;
  status: StatusPublicacao;
  licoes: LicaoParaAdmin[];
}

export interface AlternativaAdmin {
  id: string;
  texto: string;
  correta: boolean;
  motivo: string;
}

export interface ConteudoMultiplaEscolhaAdmin {
  alternativas: AlternativaAdmin[];
}

export interface ConteudoVerdadeiroOuFalsoAdmin {
  afirmacao: string;
  verdadeira: boolean;
  motivoDoErro: string;
}

export interface PalavraDoBanco {
  palavra: string;
  correta: boolean;
  motivo: string;
}

export interface ConteudoCompletarClicandoAdmin {
  texto: string;
  lacunas: { id: number; banco: PalavraDoBanco[] }[];
}

export type ConteudoAdmin =
  | { tipo: "MULTIPLA_ESCOLHA"; conteudo: ConteudoMultiplaEscolhaAdmin }
  | { tipo: "VERDADEIRO_OU_FALSO"; conteudo: ConteudoVerdadeiroOuFalsoAdmin }
  | { tipo: "COMPLETAR_CLICANDO"; conteudo: ConteudoCompletarClicandoAdmin };

export type DesafioParaAdmin = {
  id: number;
  licaoId: number;
  enunciado: string;
  dificuldade: Dificuldade;
  pontos: number;
  explicacao: string;
  dica?: string;
  versao: number;
  ordem: number;
  status: StatusPublicacao;
} & ConteudoAdmin;

type OmitirEmCadaTipo<T, K extends PropertyKey> = T extends unknown ? Omit<T, K> : never;

export type DadosDoDesafio = OmitirEmCadaTipo<DesafioParaAdmin, "id" | "licaoId" | "versao" | "ordem" | "status">;

export interface ColaboradorParaAdmin {
  id: number;
  nome: string;
  setor: string;
  empresa: string;
  ativo: boolean;
}

export interface ResumoDaImportacao {
  novos: number;
  mantidos: number;
  reativados: number;
  inativados: number;
  problemas: string[];
  homonimos: string[];
  podeConfirmar: boolean;
}
