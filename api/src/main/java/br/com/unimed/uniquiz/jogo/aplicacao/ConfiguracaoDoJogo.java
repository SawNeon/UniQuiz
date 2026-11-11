package br.com.unimed.uniquiz.jogo.aplicacao;

import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import br.com.unimed.uniquiz.jogo.dominio.RegrasDePontuacao;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ConfiguracaoDoJogo {

    @Bean
    RegrasDePontuacao regrasDePontuacao(PropriedadesDoUniQuiz propriedades) {
        PropriedadesDoUniQuiz.Pontuacao pontuacao = propriedades.pontuacao();
        return new RegrasDePontuacao(
                pontuacao.multiplicadorFacil(),
                pontuacao.multiplicadorMedio(),
                pontuacao.multiplicadorDificil(),
                pontuacao.acertosParaBonus(),
                pontuacao.bonusSequenciaAcertos(),
                pontuacao.bonusLicaoPerfeita(),
                pontuacao.bonusPrimeiraLicaoDoDia(),
                pontuacao.pontosPorNivel());
    }

    @Bean
    RandomGenerator aleatorio() {
        return new SecureRandom();
    }
}
