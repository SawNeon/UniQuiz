import { useNavigate } from "react-router";
import { enderecoDoPdf, usePerfil, useSair } from "../../api/consultas";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando } from "../../componentes/ui/Estados";
import { Icone, IconeDoTrofeu } from "../../componentes/ui/Icone";

function dataCurta(data: string): string {
  return new Date(`${data}T12:00:00`).toLocaleDateString("pt-BR");
}

export function TelaDePerfil() {
  const perfil = usePerfil();
  const sair = useSair();
  const navegar = useNavigate();

  if (!perfil.data) {
    return <Carregando />;
  }
  const dados = perfil.data;

  return (
    <div className="flex flex-col gap-6">
      <header>
        <h1 className="text-3xl font-extrabold">{dados.nome}</h1>
        <p className="font-bold">
          {dados.setor} · {dados.empresa}
        </p>
      </header>

      <dl className="grid grid-cols-2 gap-3">
        <div className="rounded-padrao bg-neutro p-4">
          <dt className="text-sm font-bold">Nível</dt>
          <dd className="font-numeros text-4xl font-bold">{dados.nivel}</dd>
          <dd className="text-sm">Faltam {dados.pontosParaProximoNivel} pontos para o próximo</dd>
        </div>
        <div className="rounded-padrao bg-neutro p-4">
          <dt className="text-sm font-bold">Pontos na temporada</dt>
          <dd className="font-numeros text-4xl font-bold">
            {dados.pontosNaTemporada.toLocaleString("pt-BR")}
          </dd>
          <dd className="text-sm">{dados.pontosTotais.toLocaleString("pt-BR")} no total</dd>
        </div>
        <div className="col-span-2 flex items-center gap-3 rounded-padrao bg-neutro p-4">
          <Icone nome="chama" tamanho={32} className="fill-energia text-energia" />
          <div>
            <dt className="text-sm font-bold">Sequência de dias</dt>
            <dd className="font-extrabold">
              {dados.diasSeguidos} agora · maior: {dados.maiorSequencia}
            </dd>
          </div>
        </div>
      </dl>

      <section aria-labelledby="titulo-certificados" className="flex flex-col gap-3">
        <h2 id="titulo-certificados" className="text-2xl font-extrabold">
          Certificados
        </h2>
        {dados.certificados.length === 0 ? (
          <p>Conclua todas as lições de um módulo para gerar o seu certificado.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {dados.certificados.map((certificado) => (
              <li
                key={certificado.codigo}
                className="flex items-center gap-3 rounded-padrao bg-acerto-suave p-3"
              >
                <Icone nome="certificado" tamanho={28} />
                <div className="flex-1">
                  <p className="font-extrabold">{certificado.modulo}</p>
                  <p className="text-sm">Emitido em {dataCurta(certificado.emitidoEm)}</p>
                </div>
                <a
                  href={enderecoDoPdf(certificado.codigo)}
                  download
                  aria-label={`Baixar o certificado de ${certificado.modulo}`}
                  className="flex size-11 items-center justify-center rounded-padrao bg-white"
                >
                  <Icone nome="baixar" />
                </a>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="titulo-trofeus" className="flex flex-col gap-3">
        <h2 id="titulo-trofeus" className="text-2xl font-extrabold">
          Troféus
        </h2>
        <ul className="grid grid-cols-2 gap-3">
          {dados.trofeus.map((trofeu) => {
            const conquistado = Boolean(trofeu.conquistadoEm);
            return (
              <li
                key={trofeu.codigo}
                className={`flex flex-col items-center gap-2 rounded-padrao p-4 text-center ${conquistado ? "bg-conquista" : "bg-neutro"}`}
              >
                {conquistado ? (
                  <IconeDoTrofeu icone={trofeu.icone} tamanho={32} />
                ) : (
                  <Icone nome="cadeado" tamanho={32} />
                )}
                <span className="font-extrabold">{trofeu.nome}</span>
                <span className="text-sm">{trofeu.descricao}</span>
                <span className="sr-only">{conquistado ? "Conquistado" : "Ainda não conquistado"}</span>
              </li>
            );
          })}
        </ul>
      </section>

      <Botao
        variante="secundario"
        onClick={() =>
          sair.mutate(undefined, { onSuccess: () => void navegar("/entrar", { replace: true }) })
        }
      >
        Não sou eu
      </Botao>
    </div>
  );
}
