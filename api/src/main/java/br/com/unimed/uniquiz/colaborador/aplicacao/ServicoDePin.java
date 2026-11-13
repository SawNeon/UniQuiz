package br.com.unimed.uniquiz.colaborador.aplicacao;

import br.com.unimed.uniquiz.colaborador.persistencia.Colaborador;
import br.com.unimed.uniquiz.colaborador.persistencia.ColaboradorRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDePin {

    private final ColaboradorRepository colaboradores;
    private final PasswordEncoder codificador;
    private final Clock relogio;

    public ServicoDePin(ColaboradorRepository colaboradores, PasswordEncoder codificador, Clock relogio) {
        this.colaboradores = colaboradores;
        this.codificador = codificador;
        this.relogio = relogio;
    }

    public enum ResultadoDoPin {
        PIN_CRIADO,
        PIN_CONFIRMADO
    }

    @Transactional(noRollbackFor = ErroDeNegocio.class)
    public ResultadoDoPin verificarOuCriar(Long colaboradorId, String pin) {
        Colaborador colaborador = colaboradorAtivo(colaboradorId);
        if (!colaborador.temPin()) {
            colaborador.definirPin(codificador.encode(pin));
            return ResultadoDoPin.PIN_CRIADO;
        }
        exigirDesbloqueado(colaborador);
        confirmarPin(colaborador, pin);
        return ResultadoDoPin.PIN_CONFIRMADO;
    }

    @Transactional
    public void redefinir(Long colaboradorId) {
        colaboradores
                .findById(colaboradorId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Colaborador não encontrado."))
                .removerPin();
    }

    private Colaborador colaboradorAtivo(Long colaboradorId) {
        return colaboradores
                .buscarTravadoPorId(colaboradorId)
                .filter(Colaborador::isAtivo)
                .orElseThrow(() -> new ErroDeNegocio("Não encontramos esse nome entre os colaboradores ativos."));
    }

    private void exigirDesbloqueado(Colaborador colaborador) {
        if (colaborador.pinBloqueado(relogio.instant())) {
            throw new ErroDeNegocio("Acesso bloqueado por excesso de tentativas. Tente de novo em 15 minutos.");
        }
    }

    private void confirmarPin(Colaborador colaborador, String pin) {
        if (codificador.matches(pin, colaborador.getPinHash())) {
            colaborador.registrarAcertoDePin();
            return;
        }
        Instant agora = relogio.instant();
        colaborador.registrarFalhaDePin(agora);
        throw new ErroDeNegocio("PIN incorreto.");
    }
}
