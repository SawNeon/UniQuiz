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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeColaboradores {

    public record Identificacao(Long id, String nome, String setor, String empresa) {}

    public record ItemDaLista(Long id, String nome) {}

    public record ColaboradorParaAdmin(Long id, String nome, String setor, String empresa, boolean ativo) {}

    private final EmpresaRepository empresas;
    private final SetorRepository setores;
    private final ColaboradorRepository colaboradores;

    public ServicoDeColaboradores(
            EmpresaRepository empresas, SetorRepository setores, ColaboradorRepository colaboradores) {
        this.empresas = empresas;
        this.setores = setores;
        this.colaboradores = colaboradores;
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
    public List<ItemDaLista> ativosDoSetor(Long setorId) {
        return colaboradores.findBySetorIdAndAtivoTrueOrderByNomeAsc(setorId).stream()
                .map(colaborador -> new ItemDaLista(colaborador.getId(), colaborador.getNome()))
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
                        colaborador.isAtivo()))
                .toList();
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
