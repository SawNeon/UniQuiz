package br.com.unimed.uniquiz.colaborador.aplicacao;

import br.com.unimed.uniquiz.colaborador.dominio.ColaboradorExistente;
import br.com.unimed.uniquiz.colaborador.dominio.LinhaDaPlanilha;
import br.com.unimed.uniquiz.colaborador.dominio.PlanoDeImportacao;
import br.com.unimed.uniquiz.colaborador.persistencia.Colaborador;
import br.com.unimed.uniquiz.colaborador.persistencia.ColaboradorRepository;
import br.com.unimed.uniquiz.colaborador.persistencia.Empresa;
import br.com.unimed.uniquiz.colaborador.persistencia.EmpresaRepository;
import br.com.unimed.uniquiz.colaborador.persistencia.Setor;
import br.com.unimed.uniquiz.colaborador.persistencia.SetorRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.Textos;
import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeImportacao {

    private static final int TAMANHO_MAXIMO_DA_PLANILHA = 2 * 1024 * 1024;

    private final LeitorDePlanilha leitor;
    private final EmpresaRepository empresas;
    private final SetorRepository setores;
    private final ColaboradorRepository colaboradores;
    private final PasswordEncoder codificador;
    private final Clock relogio;

    public ServicoDeImportacao(
            LeitorDePlanilha leitor,
            EmpresaRepository empresas,
            SetorRepository setores,
            ColaboradorRepository colaboradores,
            PasswordEncoder codificador,
            Clock relogio) {
        this.leitor = leitor;
        this.empresas = empresas;
        this.setores = setores;
        this.colaboradores = colaboradores;
        this.codificador = codificador;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public ResumoDaImportacao previa(String nomeDoArquivo, byte[] conteudo) {
        return ResumoDaImportacao.de(planejar(nomeDoArquivo, conteudo));
    }

    @Transactional
    public ResumoDaImportacao confirmar(String nomeDoArquivo, byte[] conteudo) {
        PlanoDeImportacao plano = planejar(nomeDoArquivo, conteudo);
        if (!plano.podeConfirmar()) {
            throw new ErroDeNegocio("Corrija as linhas indicadas na prévia antes de confirmar.");
        }

        Map<String, Empresa> empresasPorNome = empresas.findAll().stream()
                .collect(Collectors.toMap(empresa -> Textos.normalizar(empresa.getNome()), Function.identity()));
        Map<String, Setor> setoresPorNome = new HashMap<>();
        setores.findAll()
                .forEach(setor -> setoresPorNome.put(chaveDoSetor(setor.getEmpresaId(), setor.getNome()), setor));

        Map<String, String> hashes = hashesDosPins(plano);
        for (LinhaDaPlanilha novo : plano.novos()) {
            Empresa empresa = empresasPorNome.computeIfAbsent(
                    Textos.normalizar(novo.empresa()), ignorado -> empresas.save(new Empresa(novo.empresa())));
            Setor setor = setoresPorNome.computeIfAbsent(
                    chaveDoSetor(empresa.getId(), novo.setor()),
                    ignorado -> setores.save(new Setor(empresa.getId(), novo.setor())));
            Colaborador colaborador = new Colaborador(empresa.getId(), setor.getId(), novo.nome(), relogio.instant());
            colaborador.definirPin(hashes.get(novo.pin()));
            colaboradores.save(colaborador);
        }
        colaboradores
                .findAllById(plano.pinsDosExistentes().keySet())
                .forEach(colaborador -> colaborador.definirPin(
                        hashes.get(plano.pinsDosExistentes().get(colaborador.getId()))));
        colaboradores.findAllById(plano.reativados()).forEach(Colaborador::ativar);
        colaboradores.findAllById(plano.inativados()).forEach(Colaborador::desativar);

        return ResumoDaImportacao.de(plano);
    }

    private Map<String, String> hashesDosPins(PlanoDeImportacao plano) {
        Set<String> pins = new HashSet<>(plano.pinsDosExistentes().values());
        plano.novos().forEach(novo -> pins.add(novo.pin()));
        return pins.parallelStream().collect(Collectors.toMap(Function.identity(), codificador::encode));
    }

    private PlanoDeImportacao planejar(String nomeDoArquivo, byte[] conteudo) {
        if (conteudo.length > TAMANHO_MAXIMO_DA_PLANILHA) {
            throw new ErroDeNegocio("A planilha pode ter no máximo 2 MB.");
        }
        List<LinhaDaPlanilha> linhas = leitor.ler(nomeDoArquivo, conteudo);
        Map<Long, String> nomesDasEmpresas =
                empresas.findAll().stream().collect(Collectors.toMap(Empresa::getId, Empresa::getNome));
        Map<Long, String> nomesDosSetores =
                setores.findAll().stream().collect(Collectors.toMap(Setor::getId, Setor::getNome));
        List<ColaboradorExistente> existentes = colaboradores.findAll().stream()
                .map(colaborador -> new ColaboradorExistente(
                        colaborador.getId(),
                        nomesDasEmpresas.get(colaborador.getEmpresaId()),
                        nomesDosSetores.get(colaborador.getSetorId()),
                        colaborador.getNome(),
                        colaborador.isAtivo()))
                .toList();
        return PlanoDeImportacao.montar(linhas, existentes);
    }

    private static String chaveDoSetor(Long empresaId, String nomeDoSetor) {
        return empresaId + "|" + Textos.normalizar(nomeDoSetor);
    }
}
