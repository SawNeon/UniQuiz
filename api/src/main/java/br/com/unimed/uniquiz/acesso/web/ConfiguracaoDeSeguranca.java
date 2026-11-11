package br.com.unimed.uniquiz.acesso.web;

import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeSessao;
import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import br.com.unimed.uniquiz.comum.web.FiltroDeLimiteDeRequisicoes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
class ConfiguracaoDeSeguranca {

    private final FiltroDeLimiteDeRequisicoes limiteDeRequisicoes;

    ConfiguracaoDeSeguranca(PropriedadesDoUniQuiz propriedades, ObjectMapper json) {
        this.limiteDeRequisicoes = new FiltroDeLimiteDeRequisicoes(propriedades.limiteRequisicoes(), json);
    }

    @Bean
    SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, ServicoDeSessao sessoes) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        erros -> erros.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(rotas -> rotas.requestMatchers(
                                HttpMethod.GET,
                                "/api/empresas",
                                "/api/empresas/*/setores",
                                "/api/setores/*/colaboradores")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sessao", "/api/admin/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/sessao", "/api/admin/sessao")
                        .permitAll()
                        .requestMatchers("/api/publico/**", "/api/docs/**", "/swagger-ui/**", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/**")
                        .hasRole("COLABORADOR")
                        .anyRequest()
                        .denyAll())
                .addFilterBefore(limiteDeRequisicoes, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new FiltroDeSessao(sessoes), FiltroDeLimiteDeRequisicoes.class)
                .build();
    }

    @Bean
    PasswordEncoder codificadorDeSenha() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Scheduled(fixedRate = 3_600_000)
    void esvaziarLimites() {
        limiteDeRequisicoes.esvaziar();
    }
}
