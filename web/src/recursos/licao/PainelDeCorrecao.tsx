import type { Correcao } from "../../api/tipos";
import { Icone } from "../../componentes/ui/Icone";
import { Mascote } from "../../mascote/Mascote";

export function PainelDeCorrecao({ correcao }: { correcao: Correcao }) {
  const pontos = correcao.pontos + correcao.bonusDeSequencia;
  return (
    <div
      className={`flex flex-col gap-3 rounded-t-[28px] p-6 ${correcao.correta ? "bg-acerto-suave" : "bg-erro-suave"}`}
    >
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <Icone nome={correcao.correta ? "certo" : "errado"} tamanho={30} strokeWidth={2.5} />
          <span className="text-[26px] font-extrabold">{correcao.correta ? "Muito bem!" : "Quase!"}</span>
        </div>
        <Mascote estado={correcao.correta ? "feliz" : "triste"} tamanho={64} />
      </div>

      {!correcao.correta && correcao.motivoDoErro && (
        <p>
          <strong>Por que não:</strong> {correcao.motivoDoErro}
        </p>
      )}
      {!correcao.correta && (
        <p>
          <strong>Resposta certa:</strong> {correcao.respostaCerta}
        </p>
      )}
      <p>{correcao.explicacao}</p>
      {pontos > 0 && (
        <p className="font-numeros text-xl font-bold">
          +{pontos} pontos{correcao.bonusDeSequencia > 0 ? ` (inclui ${correcao.bonusDeSequencia} de sequência)` : ""}
        </p>
      )}
      {correcao.segundaChance && <p className="text-sm font-bold">Segunda chance: não vale pontos, mas vale aprender.</p>}
    </div>
  );
}
