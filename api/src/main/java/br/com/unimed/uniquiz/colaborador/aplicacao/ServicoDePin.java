package br.com.unimed.uniquiz.colaborador.aplicacao;

import br.com.unimed.uniquiz.colaborador.persistencia.Colaborador;
import br.com.unimed.uniquiz.colaborador.persistencia.ColaboradorRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDePin {

    private static final int TENTATIVAS_RESTANTES_PARA_AVISAR = 2;
    private static final String PROCURE_O_ADMINISTRADOR = "Se esqueceu o PIN, procure o administrador do UniQuiz.";
    private static final DateTimeFormatter FORMATO_DO_HORARIO = DateTimeFormatter.ofPattern("HH:mm");

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
            throw new ErroDeNegocio("Acesso bloqueado por excesso de tentativas. Tente de novo às %s. %s"
                    .formatted(horario(colaborador.getPinBloqueadoAte()), PROCURE_O_ADMINISTRADOR));
        }
    }

    private void confirmarPin(Colaborador colaborador, String pin) {
        if (codificador.matches(pin, colaborador.getPinHash())) {
            colaborador.registrarAcertoDePin();
            return;
        }
        Instant agora = relogio.instant();
        colaborador.registrarFalhaDePin(agora);
        throw new ErroDeNegocio(mensagemDoPinIncorreto(colaborador, agora));
    }

    private String mensagemDoPinIncorreto(Colaborador colaborador, Instant agora) {
        if (colaborador.pinBloqueado(agora)) {
            return "PIN incorreto. O acesso ficou bloqueado até as %s. %s"
                    .formatted(horario(colaborador.getPinBloqueadoAte()), PROCURE_O_ADMINISTRADOR);
        }
        int restantes = colaborador.tentativasDePinRestantes();
        if (restantes > TENTATIVAS_RESTANTES_PARA_AVISAR) {
            return "PIN incorreto.";
        }
        return restantes == 1
                ? "PIN incorreto. Resta 1 tentativa antes do bloqueio."
                : "PIN incorreto. Restam %d tentativas antes do bloqueio.".formatted(restantes);
    }

    private String horario(Instant instante) {
        return FORMATO_DO_HORARIO.format(instante.atZone(relogio.getZone()));
    }
}
