package br.com.unimed.uniquiz.comum;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ConfiguracaoGeral {

    @Bean
    Clock relogio(PropriedadesDoUniQuiz propriedades) {
        return Clock.system(ZoneId.of(propriedades.fusoHorario()));
    }
}
