package br.com.unimed.uniquiz.conteudo.aplicacao;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin.DadosDaLicao;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin.DadosDoDesafio;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin.DadosDoModulo;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoDesafio;
import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import br.com.unimed.uniquiz.conteudo.dominio.QuantidadesPorNivel;
import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import br.com.unimed.uniquiz.conteudo.persistencia.Desafio;
import br.com.unimed.uniquiz.conteudo.persistencia.DesafioRepository;
import br.com.unimed.uniquiz.conteudo.persistencia.Licao;
import br.com.unimed.uniquiz.conteudo.persistencia.LicaoRepository;
import br.com.unimed.uniquiz.conteudo.persistencia.Modulo;
import br.com.unimed.uniquiz.conteudo.persistencia.ModuloRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeConteudoAdmin {

    private final ModuloRepository modulos;
    private final LicaoRepository licoes;
    private final DesafioRepository desafios;
    private final LeitorDeConteudo leitor;

    public ServicoDeConteudoAdmin(
            ModuloRepository modulos, LicaoRepository licoes, DesafioRepository desafios, LeitorDeConteudo leitor) {
        this.modulos = modulos;
        this.licoes = licoes;
        this.desafios = desafios;
        this.leitor = leitor;
    }

    @Transactional(readOnly = true)
    public List<ConteudoParaAdmin.Modulo> listarModulos() {
        return modulos.findAllByOrderByOrdemAsc().stream().map(this::paraAdmin).toList();
    }

    @Transactional
    public ConteudoParaAdmin.Modulo criarModulo(DadosDoModulo dados) {
        Modulo modulo = modulos.save(
                new Modulo(dados.titulo(), dados.descricao(), dados.materialEstudo(), modulos.maiorOrdem() + 1));
        return paraAdmin(modulo);
    }

    @Transactional
    public ConteudoParaAdmin.Modulo atualizarModulo(Long moduloId, DadosDoModulo dados) {
        Modulo modulo = modulo(moduloId);
        modulo.atualizar(dados.titulo(), dados.descricao(), dados.materialEstudo());
        return paraAdmin(modulo);
    }

    @Transactional
    public void mudarStatusDoModulo(Long moduloId, StatusPublicacao status) {
        Modulo modulo = modulo(moduloId);
        if (status == StatusPublicacao.PUBLICADO
                && licoes.countByModuloIdAndStatus(moduloId, StatusPublicacao.PUBLICADO) == 0) {
            throw new ErroDeNegocio("Publique ao menos uma lição antes de publicar o módulo.");
        }
        modulo.mudarStatus(status);
    }

    @Transactional
    public void reordenarModulos(List<Long> ids) {
        List<Modulo> todos = modulos.findAllByOrderByOrdemAsc();
        reordenar(todos, ids, Modulo::getId, Modulo::mover);
    }

    @Transactional
    public ConteudoParaAdmin.Licao criarLicao(Long moduloId, DadosDaLicao dados) {
        modulo(moduloId);
        Licao licao = licoes.save(
                new Licao(moduloId, dados.titulo(), licoes.maiorOrdem(moduloId) + 1, dados.quantidadesOuNenhuma()));
        return paraAdmin(licao);
    }

    @Transactional
    public ConteudoParaAdmin.Licao atualizarLicao(Long licaoId, DadosDaLicao dados) {
        Licao licao = licao(licaoId);
        if (dados.mudaQuantidades()) {
            definirQuantidades(licao, dados.quantidadesOuNenhuma());
        }
        licao.renomear(dados.titulo());
        return paraAdmin(licao);
    }

    private void definirQuantidades(Licao licao, QuantidadesPorNivel quantidades) {
        if (licao.publicada()) {
            exigirBancoSuficiente(licao.getId(), quantidades);
        }
        licao.definirQuantidades(quantidades);
    }

    @Transactional
    public void mudarStatusDaLicao(Long licaoId, StatusPublicacao status) {
        Licao licao = licao(licaoId);
        if (status == StatusPublicacao.PUBLICADO) {
            exigirBancoSuficiente(licaoId, licao.quantidades());
        }
        licao.mudarStatus(status);
    }

    private void exigirBancoSuficiente(Long licaoId, QuantidadesPorNivel quantidades) {
        if (quantidades.total() == 0) {
            throw new ErroDeNegocio(
                    "Defina quantos desafios cada pessoa responde, ao menos 1, antes de publicar a lição.");
        }
        for (Dificuldade nivel : Dificuldade.values()) {
            long publicados =
                    desafios.countByLicaoIdAndStatusAndDificuldade(licaoId, StatusPublicacao.PUBLICADO, nivel);
            if (publicados < quantidades.de(nivel)) {
                throw new ErroDeNegocio(
                        "Faltam desafios no nível %s: a lição sorteia %d por pessoa e o banco publicado tem %d. Publique mais desafios desse nível ou diminua a quantidade."
                                .formatted(rotulo(nivel), quantidades.de(nivel), publicados));
            }
        }
    }

    private static String rotulo(Dificuldade nivel) {
        return switch (nivel) {
            case FACIL -> "fácil";
            case MEDIO -> "médio";
            case DIFICIL -> "difícil";
        };
    }

    @Transactional
    public void reordenarLicoes(Long moduloId, List<Long> ids) {
        reordenar(licoes.findByModuloIdOrderByOrdemAsc(moduloId), ids, Licao::getId, Licao::mover);
    }

    @Transactional(readOnly = true)
    public List<ConteudoParaAdmin.Desafio> listarDesafios(Long licaoId) {
        licao(licaoId);
        return desafios.findByLicaoIdOrderByOrdemAsc(licaoId).stream()
                .map(this::paraAdmin)
                .toList();
    }

    @Transactional
    public ConteudoParaAdmin.Desafio criarDesafio(Long licaoId, DadosDoDesafio dados) {
        licao(licaoId);
        Desafio desafio = desafios.save(new Desafio(licaoId, paraEntidade(dados), desafios.maiorOrdem(licaoId) + 1));
        return paraAdmin(desafio);
    }

    @Transactional
    public ConteudoParaAdmin.Desafio atualizarDesafio(Long desafioId, DadosDoDesafio dados) {
        Desafio desafio = desafio(desafioId);
        desafio.atualizar(paraEntidade(dados));
        return paraAdmin(desafio);
    }

    @Transactional
    public void mudarStatusDoDesafio(Long desafioId, StatusPublicacao status) {
        Desafio desafio = desafio(desafioId);
        if (desafio.saiDoBancoAoMudarPara(status)) {
            exigirQueOBancoAguenteSaidaDe(desafio);
        }
        desafio.mudarStatus(status);
    }

    private void exigirQueOBancoAguenteSaidaDe(Desafio desafio) {
        Licao licao = licao(desafio.getLicaoId());
        if (!licao.publicada()) {
            return;
        }
        long restantes = publicadosDoNivel(licao, desafio.getDificuldade()) - 1;
        int sorteados = licao.quantidades().de(desafio.getDificuldade());
        if (restantes < sorteados) {
            throw new ErroDeNegocio(
                    "A lição publicada sorteia %d desafios do nível %s e, sem este, o banco ficaria com %d. Publique outro desafio desse nível antes, ou diminua a quantidade da lição."
                            .formatted(sorteados, rotulo(desafio.getDificuldade()), restantes));
        }
    }

    @Transactional
    public ConteudoParaAdmin.Desafio duplicarDesafio(Long desafioId) {
        Desafio original = desafio(desafioId);
        Desafio copia = desafios.save(original.copiar(desafios.maiorOrdem(original.getLicaoId()) + 1));
        return paraAdmin(copia);
    }

    @Transactional
    public void reordenarDesafios(Long licaoId, List<Long> ids) {
        reordenar(desafios.findByLicaoIdOrderByOrdemAsc(licaoId), ids, Desafio::getId, Desafio::mover);
    }

    private Desafio.DadosDoDesafio paraEntidade(DadosDoDesafio dados) {
        ConteudoDesafio conteudo = leitor.converter(dados.tipo(), dados.conteudo());
        return new Desafio.DadosDoDesafio(
                dados.tipo(),
                dados.enunciado(),
                dados.dificuldade(),
                dados.pontos(),
                dados.explicacao(),
                dados.dica(),
                leitor.escrever(conteudo));
    }

    private static <T> void reordenar(
            List<T> itens, List<Long> novaOrdem, Function<T, Long> id, ObjIntConsumer<T> mover) {
        Map<Long, T> porId = itens.stream().collect(Collectors.toMap(id, Function.identity()));
        if (novaOrdem.size() != itens.size() || !porId.keySet().equals(new HashSet<>(novaOrdem))) {
            throw new ErroDeNegocio("A nova ordem precisa conter todos os itens, sem repetir.");
        }
        for (int posicao = 0; posicao < novaOrdem.size(); posicao++) {
            mover.accept(porId.get(novaOrdem.get(posicao)), posicao + 1);
        }
    }

    private ConteudoParaAdmin.Modulo paraAdmin(Modulo modulo) {
        List<ConteudoParaAdmin.Licao> licoesDoModulo = licoes.findByModuloIdOrderByOrdemAsc(modulo.getId()).stream()
                .map(this::paraAdmin)
                .toList();
        return new ConteudoParaAdmin.Modulo(
                modulo.getId(),
                modulo.getTitulo(),
                modulo.getDescricao(),
                modulo.getMaterialEstudo(),
                modulo.getOrdem(),
                modulo.getStatus(),
                licoesDoModulo);
    }

    private ConteudoParaAdmin.Licao paraAdmin(Licao licao) {
        return new ConteudoParaAdmin.Licao(
                licao.getId(),
                licao.getModuloId(),
                licao.getTitulo(),
                licao.getOrdem(),
                licao.getStatus(),
                desafios.countByLicaoIdAndStatus(licao.getId(), StatusPublicacao.PUBLICADO),
                licao.quantidades(),
                new ConteudoParaAdmin.ContagemPorNivel(
                        publicadosDoNivel(licao, Dificuldade.FACIL),
                        publicadosDoNivel(licao, Dificuldade.MEDIO),
                        publicadosDoNivel(licao, Dificuldade.DIFICIL)));
    }

    private long publicadosDoNivel(Licao licao, Dificuldade nivel) {
        return desafios.countByLicaoIdAndStatusAndDificuldade(licao.getId(), StatusPublicacao.PUBLICADO, nivel);
    }

    private ConteudoParaAdmin.Desafio paraAdmin(Desafio desafio) {
        return new ConteudoParaAdmin.Desafio(
                desafio.getId(),
                desafio.getLicaoId(),
                desafio.getTipo(),
                desafio.getEnunciado(),
                desafio.getDificuldade(),
                desafio.getPontos(),
                desafio.getExplicacao(),
                desafio.getDica(),
                leitor.paraArvore(desafio.getConteudoJson()),
                desafio.getVersao(),
                desafio.getOrdem(),
                desafio.getStatus());
    }

    private Modulo modulo(Long id) {
        return modulos.findById(id).orElseThrow(() -> new RecursoNaoEncontrado("Módulo não encontrado."));
    }

    private Licao licao(Long id) {
        return licoes.findById(id).orElseThrow(() -> new RecursoNaoEncontrado("Lição não encontrada."));
    }

    private Desafio desafio(Long id) {
        return desafios.findById(id).orElseThrow(() -> new RecursoNaoEncontrado("Desafio não encontrado."));
    }
}
