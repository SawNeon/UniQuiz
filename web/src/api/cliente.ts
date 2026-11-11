interface Problema {
  title?: string;
  detail?: string;
  campos?: Record<string, string>;
}

export class ErroDaApi extends Error {
  readonly status: number;
  readonly campos: Record<string, string>;

  constructor(status: number, mensagem: string, campos: Record<string, string> = {}) {
    super(mensagem);
    this.name = "ErroDaApi";
    this.status = status;
    this.campos = campos;
  }
}

type Corpo = object | FormData | undefined;

async function requisitar<T>(metodo: string, caminho: string, corpo?: Corpo): Promise<T> {
  const ehFormulario = corpo instanceof FormData;
  const resposta = await fetch(`/api${caminho}`, {
    method: metodo,
    credentials: "same-origin",
    headers: {
      Accept: "application/json",
      ...(corpo !== undefined && !ehFormulario ? { "Content-Type": "application/json" } : {}),
    },
    body: corpo === undefined ? undefined : ehFormulario ? corpo : JSON.stringify(corpo),
  });

  if (!resposta.ok) {
    const problema = (await resposta.json().catch(() => null)) as Problema | null;
    throw new ErroDaApi(
      resposta.status,
      problema?.detail ?? "Não foi possível falar com o servidor. Tente de novo.",
      problema?.campos,
    );
  }
  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}

export const api = {
  get: <T>(caminho: string) => requisitar<T>("GET", caminho),
  post: <T = void>(caminho: string, corpo?: Corpo) => requisitar<T>("POST", caminho, corpo),
  put: <T>(caminho: string, corpo: Corpo) => requisitar<T>("PUT", caminho, corpo),
  patch: <T = void>(caminho: string, corpo: Corpo) => requisitar<T>("PATCH", caminho, corpo),
  delete: (caminho: string) => requisitar<void>("DELETE", caminho),
};

export function naoAutenticado(erro: unknown): boolean {
  return erro instanceof ErroDaApi && erro.status === 401;
}

export function mensagemDe(erro: unknown): string {
  return erro instanceof Error ? erro.message : "Algo deu errado. Tente de novo.";
}
