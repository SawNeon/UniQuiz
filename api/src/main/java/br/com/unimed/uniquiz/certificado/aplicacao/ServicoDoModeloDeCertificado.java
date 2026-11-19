package br.com.unimed.uniquiz.certificado.aplicacao;

import br.com.unimed.uniquiz.certificado.aplicacao.GeradorDePdf.DadosDoCertificado;
import br.com.unimed.uniquiz.certificado.aplicacao.GeradorDePdf.Modelo;
import br.com.unimed.uniquiz.certificado.dominio.LayoutDoCertificado;
import br.com.unimed.uniquiz.certificado.dominio.PosicaoDoCampo;
import br.com.unimed.uniquiz.certificado.persistencia.RepositorioDoModelo;
import br.com.unimed.uniquiz.certificado.persistencia.RepositorioDoModelo.ModeloGuardado;
import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.comum.Textos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ServicoDoModeloDeCertificado {

    private static final String CODIGO_DE_EXEMPLO = "EXMP2345ABCD";

    public record ModeloVista(
            List<PosicaoDoCampo> campos,
            boolean temArte,
            String nomeDaArte,
            Instant atualizadoEm,
            List<String> avisos) {}

    private record EmMemoria(Instant versao, Modelo modelo, String nomeDaArte) {}

    private final RepositorioDoModelo repositorio;
    private final PreparadorDaArte preparador;
    private final GeradorDePdf gerador;
    private final ObjectMapper json;
    private final Clock relogio;
    private final String urlPublica;
    private volatile EmMemoria emMemoria;

    public ServicoDoModeloDeCertificado(
            RepositorioDoModelo repositorio,
            PreparadorDaArte preparador,
            GeradorDePdf gerador,
            ObjectMapper json,
            Clock relogio,
            PropriedadesDoUniQuiz propriedades) {
        this.repositorio = repositorio;
        this.preparador = preparador;
        this.gerador = gerador;
        this.json = json;
        this.relogio = relogio;
        this.urlPublica = propriedades.urlPublica();
    }

    public ModeloVista vista() {
        return vista(List.of());
    }

    public ModeloVista salvarLayout(List<PosicaoDoCampo> campos, Long administradorId) {
        LayoutDoCertificado layout = new LayoutDoCertificado(campos);
        repositorio.salvarLayout(paraJson(layout), relogio.instant(), administradorId);
        return vista();
    }

    public ModeloVista enviarArte(String nomeDoArquivo, byte[] arquivo, Long administradorId) {
        PreparadorDaArte.ArtePreparada arte = preparador.preparar(arquivo);
        repositorio.salvarArte(
                paraJson(LayoutDoCertificado.padrao()),
                arte.fundoPdf(),
                arte.previa(),
                nomeDaArte(nomeDoArquivo),
                relogio.instant(),
                administradorId);
        return vista(arte.avisos());
    }

    public ModeloVista removerArte(Long administradorId) {
        repositorio.salvarArte(
                paraJson(LayoutDoCertificado.padrao()), null, null, null, relogio.instant(), administradorId);
        return vista();
    }

    public byte[] previaDaArte() {
        return repositorio
                .previaDaArte()
                .orElseThrow(() -> new RecursoNaoEncontrado("O certificado ainda não tem arte de fundo."));
    }

    public byte[] exemplo(List<PosicaoDoCampo> campos) {
        Modelo atual = atual();
        return gerador.gerar(
                new DadosDoCertificado(
                        "Maria Aparecida Exemplo da Silva",
                        "Segurança da informação no dia a dia",
                        LocalDate.now(relogio),
                        CODIGO_DE_EXEMPLO,
                        urlPublica + "/certificado/" + CODIGO_DE_EXEMPLO),
                new Modelo(new LayoutDoCertificado(campos), atual.fundoPdf()));
    }

    Modelo atual() {
        Optional<Instant> versao = repositorio.versao();
        if (versao.isEmpty()) {
            return Modelo.padrao();
        }
        EmMemoria guardado = emMemoria;
        if (guardado != null && guardado.versao().equals(versao.get())) {
            return guardado.modelo();
        }
        return carregar().modelo();
    }

    private EmMemoria carregar() {
        Optional<ModeloGuardado> guardado = repositorio.ler();
        if (guardado.isEmpty()) {
            return new EmMemoria(Instant.EPOCH, Modelo.padrao(), null);
        }
        ModeloGuardado modelo = guardado.get();
        EmMemoria novo = new EmMemoria(
                modelo.atualizadoEm(), new Modelo(deJson(modelo.layout()), modelo.fundoPdf()), modelo.nomeDaArte());
        emMemoria = novo;
        return novo;
    }

    private ModeloVista vista(List<String> avisos) {
        Optional<Instant> versao = repositorio.versao();
        if (versao.isEmpty()) {
            return new ModeloVista(LayoutDoCertificado.padrao().campos(), false, null, null, avisos);
        }
        EmMemoria guardado = emMemoria;
        if (guardado == null || !guardado.versao().equals(versao.get())) {
            guardado = carregar();
        }
        return new ModeloVista(
                guardado.modelo().layout().campos(),
                guardado.modelo().temArte(),
                guardado.nomeDaArte(),
                guardado.versao(),
                avisos);
    }

    private String paraJson(LayoutDoCertificado layout) {
        try {
            return json.writeValueAsString(layout.campos());
        } catch (JsonProcessingException erro) {
            throw new IllegalStateException("Não foi possível guardar o modelo do certificado", erro);
        }
    }

    private LayoutDoCertificado deJson(String layout) {
        try {
            return new LayoutDoCertificado(List.of(json.readValue(layout, PosicaoDoCampo[].class)));
        } catch (JsonProcessingException erro) {
            throw new IllegalStateException("O modelo do certificado guardado está ilegível", erro);
        }
    }

    private static String nomeDaArte(String nomeDoArquivo) {
        String limpo = Textos.limpar(nomeDoArquivo).replaceAll("\\p{Cntrl}", "");
        if (limpo.isEmpty()) {
            return "arte";
        }
        return limpo.length() <= 200 ? limpo : limpo.substring(0, 200);
    }
}
