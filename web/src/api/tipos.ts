export type Dificuldade = "FACIL" | "MEDIO" | "DIFICIL";
export type TipoDesafio = "MULTIPLA_ESCOLHA" | "VERDADEIRO_OU_FALSO" | "COMPLETAR_CLICANDO";
export type StatusPublicacao = "RASCUNHO" | "PUBLICADO" | "ARQUIVADO";
export type SituacaoDaLicao = "CONCLUIDA" | "DISPONIVEL" | "BLOQUEADA";

export interface ItemDaLista {
  id: number;
  nome: string;
}

export interface ColaboradorDaEntrada extends ItemDaLista {
  temPin: boolean;
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

export type RespostaDoColaborador =
  RespostaMultiplaEscolha | RespostaVerdadeiroOuFalso | RespostaCompletarClicando;

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

export interface PorNivel {
  facil: number;
  medio: number;
  dificil: number;
}

export interface LicaoParaAdmin {
  id: number;
  moduloId: number;
  titulo: string;
  ordem: number;
  status: StatusPublicacao;
  desafiosPublicados: number;
  quantidades: PorNivel;
  publicadosPorNivel: PorNivel;
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

export type DadosDoDesafio = OmitirEmCadaTipo<
  DesafioParaAdmin,
  "id" | "licaoId" | "versao" | "ordem" | "status"
>;

export interface ColaboradorParaAdmin {
  id: number;
  nome: string;
  setor: string;
  empresa: string;
  ativo: boolean;
  temPin: boolean;
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

export type Permissao =
  | "GERIR_CONTEUDO"
  | "GERIR_COLABORADORES"
  | "GERIR_TEMPORADAS"
  | "GERIR_USUARIOS_DO_PAINEL"
  | "VER_RELATORIOS"
  | "EXPORTAR_RELATORIOS"
  | "VER_AUDITORIA";

export type PerfilDoPainel = "ADMINISTRADOR" | "GESTOR" | "AUDITOR";

export interface ContaLogada {
  id: number;
  nome: string;
  usuario: string;
  perfil: PerfilDoPainel;
  permissoes: Permissao[];
  precisaTrocarSenha: boolean;
}

export interface UsuarioDoPainel {
  id: number;
  nome: string;
  usuario: string;
  perfil: PerfilDoPainel;
  setores: number[];
  ativo: boolean;
  bloqueado: boolean;
  precisaTrocarSenha: boolean;
  criadoEm: string;
  ultimoAcessoEm?: string | null;
}

export interface DadosDoUsuarioDoPainel {
  nome: string;
  perfil: PerfilDoPainel;
  setores: number[];
}

export interface SenhaProvisoria {
  usuarioId: number;
  usuario: string;
  senha: string;
}

export interface SessaoAtiva {
  id: string;
  criadaEm: string;
  ultimoUsoEm: string;
  ip?: string | null;
  dispositivo?: string | null;
  atual: boolean;
}

export type TipoDeAtor = "ADMINISTRADOR" | "COLABORADOR" | "ANONIMO";
export type ResultadoDaAcao = "SUCESSO" | "FALHA";

export interface RegistroDeAuditoria {
  id: number;
  momento: string;
  atorTipo: TipoDeAtor;
  atorId?: number | null;
  atorNome?: string | null;
  acao: string;
  acaoDescricao: string;
  alvo?: string | null;
  resultado: ResultadoDaAcao;
  detalhe?: string | null;
  ip?: string | null;
}

export interface PaginaDeAuditoria {
  itens: RegistroDeAuditoria[];
  pagina: number;
  tamanho: number;
  total: number;
}

export interface FalhasDeEntrada {
  quem?: string | null;
  ip?: string | null;
  falhas: number;
  ultima: string;
}

export interface ContaBloqueada {
  tipo: "ADMINISTRADOR" | "COLABORADOR";
  id: number;
  nome: string;
  ate: string;
}

export interface AlertasDeAuditoria {
  desde: string;
  falhasPorPessoa: FalhasDeEntrada[];
  falhasPorIp: FalhasDeEntrada[];
  contasBloqueadas: ContaBloqueada[];
}

export interface FiltroDeAuditoria {
  atorTipo?: TipoDeAtor;
  acao?: string;
  resultado?: ResultadoDaAcao;
  busca?: string;
  de?: string;
  ate?: string;
  pagina: number;
}
