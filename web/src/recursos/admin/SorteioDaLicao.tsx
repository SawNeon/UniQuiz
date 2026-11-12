import { useState } from "react";
import { useDefinirQuantidades } from "../../api/admin";
import type { LicaoParaAdmin, PorNivel } from "../../api/tipos";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { Entrada } from "./componentes";
import { MAXIMO_POR_NIVEL, NIVEIS, quantidadeDigitada, totalDeDesafios } from "./niveis";

type Nivel = (typeof NIVEIS)[number];

interface EditorDeQuantidadesProps {
  valor: PorNivel;
  aoMudar: (novo: PorNivel) => void;
  rotuloDaLicao: string;
  aoSair?: () => void;
  publicados?: PorNivel;
}

export function EditorDeQuantidades({
  valor,
  aoMudar,
  rotuloDaLicao,
  aoSair,
  publicados,
}: EditorDeQuantidadesProps) {
  return (
    <div className="flex flex-wrap gap-4">
      {NIVEIS.map((nivel) => (
        <CampoDeQuantidade
          key={nivel.chave}
          nivel={nivel}
          quantidade={valor[nivel.chave]}
          aoMudar={(quantidade) => aoMudar({ ...valor, [nivel.chave]: quantidade })}
          rotuloDaLicao={rotuloDaLicao}
          aoSair={aoSair}
          noBanco={publicados?.[nivel.chave]}
        />
      ))}
    </div>
  );
}

interface CampoDeQuantidadeProps {
  nivel: Nivel;
  quantidade: number;
  aoMudar: (quantidade: number) => void;
  rotuloDaLicao: string;
  aoSair?: () => void;
  noBanco?: number;
}

function CampoDeQuantidade({
  nivel,
  quantidade,
  aoMudar,
  rotuloDaLicao,
  aoSair,
  noBanco,
}: CampoDeQuantidadeProps) {
  const faltamDesafios = noBanco !== undefined && noBanco < quantidade;
  return (
    <div className="flex flex-col gap-1">
      <label className="flex flex-col gap-1 text-sm font-bold">
        {nivel.rotulo} por pessoa
        <Entrada
          type="number"
          inputMode="numeric"
          min={0}
          max={MAXIMO_POR_NIVEL}
          aria-label={`${nivel.rotulo} por pessoa ${rotuloDaLicao}`}
          value={quantidade}
          onChange={(evento) => aoMudar(quantidadeDigitada(evento.target.value))}
          onBlur={aoSair}
          className="w-28"
        />
      </label>
      {noBanco !== undefined && (
        <span className={`text-sm ${faltamDesafios ? "font-extrabold" : ""}`}>
          Banco: {noBanco} publicados{faltamDesafios ? " (faltam desafios)" : ""}
        </span>
      )}
    </div>
  );
}

function quantidadesMudaram(atuais: PorNivel, anteriores: PorNivel): boolean {
  return NIVEIS.some(({ chave }) => atuais[chave] !== anteriores[chave]);
}

export function SorteioDaLicao({ licao }: { licao: LicaoParaAdmin }) {
  const [valor, setValor] = useState<PorNivel>(licao.quantidades);
  const definir = useDefinirQuantidades();

  function salvar() {
    if (!quantidadesMudaram(valor, licao.quantidades)) {
      return;
    }
    definir.mutate(
      { licaoId: licao.id, titulo: licao.titulo, quantidades: valor },
      { onError: () => setValor(licao.quantidades) },
    );
  }

  return (
    <div className="flex w-full flex-col gap-2 border-t border-neutro pt-3">
      <p className="text-sm">
        Cada pessoa responde <strong>{totalDeDesafios(valor)}</strong> desafios, sorteados do banco publicado
        (podem ser mais do que isso).
      </p>
      <EditorDeQuantidades
        valor={valor}
        aoMudar={setValor}
        rotuloDaLicao={`em ${licao.titulo}`}
        aoSair={salvar}
        publicados={licao.publicadosPorNivel}
      />
      {definir.isError && <MensagemDeErro erro={definir.error} />}
    </div>
  );
}
