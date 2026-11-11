package br.com.unimed.uniquiz.comum;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("uniquiz")
public record PropriedadesDoUniQuiz(
        String fusoHorario,
        String urlPublica,
        Sessao sessao,
        AdminInicial adminInicial,
        Pontuacao pontuacao,
        LimiteRequisicoes limiteRequisicoes) {

    public record Sessao(Duration duracao, boolean cookieSeguro) {}

    public record AdminInicial(String usuario, String senha) {}

    public record Pontuacao(
            double multiplicadorFacil,
            double multiplicadorMedio,
            double multiplicadorDificil,
            int acertosParaBonus,
            int bonusSequenciaAcertos,
            int bonusLicaoPerfeita,
            int bonusPrimeiraLicaoDoDia,
            int pontosPorNivel) {}

    public record LimiteRequisicoes(int geralPorMinuto, int loginAdminPorMinuto) {}
}
