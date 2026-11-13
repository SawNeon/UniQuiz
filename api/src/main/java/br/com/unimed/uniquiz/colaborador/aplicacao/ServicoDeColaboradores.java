package br.com.unimed.uniquiz.colaborador.aplicacao;

import br.com.unimed.uniquiz.colaborador.persistencia.Colaborador;
import br.com.unimed.uniquiz.colaborador.persistencia.ColaboradorRepository;
import br.com.unimed.uniquiz.colaborador.persistencia.Empresa;
import br.com.unimed.uniquiz.colaborador.persistencia.EmpresaRepository;
import br.com.unimed.uniquiz.colaborador.persistencia.Setor;
import br.com.unimed.uniquiz.colaborador.persistencia.SetorRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.comum.Textos;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeColaboradores {

    public record Identificacao(Long id, String nome, String setor, String empresa) {}

    public record ItemDaLista(Long id, String nome) {}

    public record ColaboradorDaEntrada(Long id, String nome, boolean temPin) {}

    public record ColaboradorParaAdmin(
            Long id, String nome, String setor, String empresa, boolean ativo, boolean temPin) {}

    private final EmpresaRepository empresas;
    private final SetorRepository setores;
    private final ColaboradorRepository colaboradores;
    private final PasswordEncoder codificador;
    private final Clock relogio;

    public ServicoDeColaboradores(
            EmpresaRepository empresas,
            SetorRepository setores,
            ColaboradorRepository colaboradores,
            PasswordEncoder codificador,
            Clock relogio) {
        this.empresas = empresas;
        this.setores = setores;
        this.colaboradores = colaboradores;
        this.codificador = codificador;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<ItemDaLista> empresas() {
        return empresas.findAllByOrderByNomeAsc().stream()
                .map(empresa -> new ItemDaLista(empresa.getId(), empresa.getNome()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemDaLista> setoresDaEmpresa(Long empresaId) {
        return setores.findByEmpresaIdOrderByNomeAsc(empresaId).stream()
                .map(setor -> new ItemDaLista(setor.getId(), setor.getNome()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemDaLista> setoresComEmpresa() {
        Map<Long, String> nomesDasEmpresas =
                empresas.findAll().stream().collect(Collectors.toMap(Empresa::getId, Empresa::getNome));
        return setores.findAll().stream()
                .map(setor -> new ItemDaLista(
                        setor.getId(), nomesDasEmpresas.get(setor.getEmpresaId()) + " · " + setor.getNome()))
                .sorted(Comparator.comparing(ItemDaLista::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean setoresExistem(Set<Long> setorIds) {
        return setores.findAllById(setorIds).size() == setorIds.size();
    }

    @Transactional(readOnly = true)
    public List<ColaboradorDaEntrada> ativosDoSetor(Long setorId) {
        return colaboradores.findBySetorIdAndAtivoTrueOrderByNomeAsc(setorId).stream()
                .map(colaborador ->
                        new ColaboradorDaEntrada(colaborador.getId(), colaborador.getNome(), colaborador.temPin()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Identificacao identificar(Long colaboradorId) {
        Colaborador colaborador = colaboradores
                .findById(colaboradorId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Colaborador não encontrado."));
        Setor setor = setores.getReferenceById(colaborador.getSetorId());
        return new Identificacao(
                colaborador.getId(),
                colaborador.getNome(),
                setor.getNome(),
                empresas.getReferenceById(colaborador.getEmpresaId()).getNome());
    }

    @Transactional(readOnly = true)
    public boolean estaAtivo(Long colaboradorId) {
        return colaboradores.findById(colaboradorId).map(Colaborador::isAtivo).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<ColaboradorParaAdmin> buscar(String trecho) {
        Map<Long, String> nomesDosSetores =
                setores.findAll().stream().collect(Collectors.toMap(Setor::getId, Setor::getNome));
        Map<Long, String> nomesDasEmpresas =
                empresas.findAll().stream().collect(Collectors.toMap(Empresa::getId, Empresa::getNome));
        return colaboradores.findTop200ByNomeNormalizadoContainingOrderByNomeAsc(Textos.normalizar(trecho)).stream()
                .map(colaborador -> new ColaboradorParaAdmin(
                        colaborador.getId(),
                        colaborador.getNome(),
                        nomesDosSetores.get(colaborador.getSetorId()),
                        nomesDasEmpresas.get(colaborador.getEmpresaId()),
                        colaborador.isAtivo(),
                        colaborador.temPin()))
                .toList();
    }

    @Transactional
    public ColaboradorParaAdmin cadastrar(String nome, String nomeDaEmpresa, String nomeDoSetor, String pin) {
        Empresa empresa = empresaExistenteOuNova(Textos.limpar(nomeDaEmpresa));
        Setor setor = setorExistenteOuNovo(empresa, Textos.limpar(nomeDoSetor));
        exigirNomeLivre(empresa, setor, nome);
        Colaborador colaborador = colaboradores.save(novoColaborador(empresa, setor, nome, pin));
        return new ColaboradorParaAdmin(
                colaborador.getId(),
                colaborador.getNome(),
                setor.getNome(),
                empresa.getNome(),
                true,
                colaborador.temPin());
    }

    private Empresa empresaExistenteOuNova(String nome) {
        return empresas.findAll().stream()
                .filter(existente -> mesmoNome(existente.getNome(), nome))
                .findFirst()
                .orElseGet(() -> empresas.save(new Empresa(nome)));
    }

    private Setor setorExistenteOuNovo(Empresa empresa, String nome) {
        return setores.findByEmpresaIdOrderByNomeAsc(empresa.getId()).stream()
                .filter(existente -> mesmoNome(existente.getNome(), nome))
                .findFirst()
                .orElseGet(() -> setores.save(new Setor(empresa.getId(), nome)));
    }

    private void exigirNomeLivre(Empresa empresa, Setor setor, String nome) {
        if (colaboradores.existsByEmpresaIdAndSetorIdAndNomeNormalizado(
                empresa.getId(), setor.getId(), Textos.normalizar(nome))) {
            throw new ErroDeNegocio(
                    "Já existe uma pessoa com esse nome nesse setor. Procure por ela em \"Ajustar colaboradores\".");
        }
    }

    private Colaborador novoColaborador(Empresa empresa, Setor setor, String nome, String pin) {
        Colaborador colaborador = new Colaborador(empresa.getId(), setor.getId(), nome, relogio.instant());
        if (pin != null) {
            colaborador.definirPin(codificador.encode(pin));
        }
        return colaborador;
    }

    private static boolean mesmoNome(String um, String outro) {
        return Textos.normalizar(um).equals(Textos.normalizar(outro));
    }

    @Transactional
    public void ajustar(Long colaboradorId, String nome, boolean ativo) {
        Colaborador colaborador = colaboradores
                .findById(colaboradorId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Colaborador não encontrado."));
        boolean nomeRepetido = colaboradores.existsByEmpresaIdAndSetorIdAndNomeNormalizadoAndIdNot(
                colaborador.getEmpresaId(), colaborador.getSetorId(), Textos.normalizar(nome), colaboradorId);
        if (nomeRepetido) {
            throw new ErroDeNegocio("Já existe outra pessoa com esse nome no mesmo setor.");
        }
        colaborador.renomear(nome);
        if (ativo) {
            colaborador.ativar();
        } else {
            colaborador.desativar();
        }
    }
}
