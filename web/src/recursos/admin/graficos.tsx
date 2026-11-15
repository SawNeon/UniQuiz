import { formatarPercentual } from "./painel";

export interface Fatia {
  rotulo: string;
  valor: number;
  cor: string;
}

function porcentagem(valor: number, total: number): number {
  return total === 0 ? 0 : (valor / total) * 100;
}

export function BarraEmpilhada({ fatias, rotulo }: { fatias: Fatia[]; rotulo: string }) {
  const total = fatias.reduce((soma, fatia) => soma + fatia.valor, 0);
  const descricao = fatias.map((fatia) => `${fatia.rotulo}: ${fatia.valor}`).join(", ");

  return (
    <div className="flex flex-col gap-2">
      <div
        role="img"
        aria-label={`${rotulo}. ${descricao}`}
        className="flex h-5 w-full overflow-hidden rounded-full bg-neutro"
      >
        {fatias.map((fatia) =>
          fatia.valor > 0 ? (
            <span
              key={fatia.rotulo}
              className={fatia.cor}
              style={{ width: `${porcentagem(fatia.valor, total)}%` }}
            />
          ) : null,
        )}
      </div>
      <ul className="flex flex-wrap gap-x-4 gap-y-1 text-sm" aria-hidden="true">
        {fatias.map((fatia) => (
          <li key={fatia.rotulo} className="flex items-center gap-1.5">
            <span className={`inline-block size-3 rounded-full ${fatia.cor}`} />
            {fatia.rotulo}: <strong>{fatia.valor}</strong>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function BarraDePercentual({ valor, rotulo }: { valor?: number | null; rotulo: string }) {
  const largura = Math.min(Math.max(valor ?? 0, 0), 100);
  return (
    <div className="flex min-w-36 items-center gap-2">
      <div
        role="meter"
        aria-label={rotulo}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={valor ?? undefined}
        aria-valuetext={formatarPercentual(valor)}
        className="h-3 flex-1 overflow-hidden rounded-full bg-neutro"
      >
        <div className="h-3 rounded-full bg-primaria" style={{ width: `${largura}%` }} />
      </div>
      <span className="w-14 text-right font-numeros text-lg font-bold">{formatarPercentual(valor)}</span>
    </div>
  );
}

export interface Coluna {
  rotulo: string;
  valor: number;
  detalhe: string;
}

export function GraficoDeColunas({ colunas, rotulo }: { colunas: Coluna[]; rotulo: string }) {
  const maior = Math.max(1, ...colunas.map((coluna) => coluna.valor));
  return (
    <figure className="flex flex-col gap-2">
      <figcaption className="font-bold">{rotulo}</figcaption>
      <div className="flex h-48 items-end gap-1 border-b-2 border-neutro" aria-hidden="true">
        {colunas.map((coluna) => (
          <div key={coluna.rotulo} className="flex h-full flex-1 flex-col items-center justify-end gap-1">
            <span className="text-xs font-bold">{coluna.valor}</span>
            <div
              className="w-full rounded-t-md bg-primaria"
              style={{ height: `${porcentagem(coluna.valor, maior)}%` }}
              title={coluna.detalhe}
            />
          </div>
        ))}
      </div>
      <div className="flex gap-1 text-xs" aria-hidden="true">
        {colunas.map((coluna) => (
          <span key={coluna.rotulo} className="flex-1 text-center">
            {coluna.rotulo}
          </span>
        ))}
      </div>
    </figure>
  );
}
