package br.com.unimed.uniquiz.conteudo.dominio;

import static br.com.unimed.uniquiz.conteudo.dominio.Validacoes.exigir;
import static br.com.unimed.uniquiz.conteudo.dominio.Validacoes.preenchido;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record ConteudoCompletarClicando(String texto, List<Lacuna> lacunas) implements ConteudoDesafio {

    private static final Pattern MARCADOR = Pattern.compile("\\{\\{(\\d+)}}");
    private static final String MOTIVO_PADRAO = "Essa palavra não completa a frase corretamente.";

    public record Lacuna(int id, List<PalavraDoBanco> banco) {

        public Lacuna {
            banco = banco == null ? List.of() : List.copyOf(banco);
        }

        String palavraCorreta() {
            return banco.stream()
                    .filter(PalavraDoBanco::correta)
                    .map(PalavraDoBanco::palavra)
                    .findFirst()
                    .orElseThrow();
        }

        Optional<PalavraDoBanco> buscar(String palavra) {
            return banco.stream()
                    .filter(opcao -> opcao.palavra().equals(palavra))
                    .findFirst();
        }
    }

    public record PalavraDoBanco(String palavra, boolean correta, String motivo) {}

    public ConteudoCompletarClicando {
        lacunas = lacunas == null ? List.of() : List.copyOf(lacunas);
    }

    @Override
    public void validar() {
        exigir(preenchido(texto), "Escreva a frase.");
        exigir(!lacunas.isEmpty(), "Selecione ao menos uma palavra e transforme em lacuna.");
        Set<Integer> idsDasLacunas = lacunas.stream().map(Lacuna::id).collect(Collectors.toCollection(TreeSet::new));
        exigir(idsDasLacunas.size() == lacunas.size(), "Cada lacuna precisa de um número diferente.");
        exigir(marcadoresNoTexto().equals(idsDasLacunas), "Cada lacuna da frase precisa ter o seu banco de palavras.");
        for (Lacuna lacuna : lacunas) {
            exigir(lacuna.banco().size() >= 2, "Cada lacuna precisa da palavra certa e de ao menos uma errada.");
            exigir(
                    lacuna.banco().stream().filter(PalavraDoBanco::correta).count() == 1,
                    "Cada lacuna precisa de exatamente uma palavra certa.");
            for (PalavraDoBanco opcao : lacuna.banco()) {
                exigir(preenchido(opcao.palavra()), "Preencha todas as palavras do banco.");
                exigir(
                        opcao.correta() || preenchido(opcao.motivo()),
                        "Explique por que \"%s\" está errada.".formatted(opcao.palavra()));
            }
        }
    }

    @Override
    public ConteudoPublico paraColaborador(RandomGenerator aleatorio) {
        Random embaralhador = new Random(aleatorio.nextLong());
        return new ConteudoPublico.CompletarClicando(
                texto,
                lacunas.stream()
                        .map(lacuna -> {
                            List<String> palavras = new ArrayList<>(lacuna.banco().stream()
                                    .map(PalavraDoBanco::palavra)
                                    .toList());
                            Collections.shuffle(palavras, embaralhador);
                            return new ConteudoPublico.LacunaPublica(lacuna.id(), List.copyOf(palavras));
                        })
                        .toList());
    }

    @Override
    public Correcao corrigir(RespostaDoColaborador resposta) {
        var preenchimento = Validacoes.respostaDoTipo(resposta, RespostaDoColaborador.CompletarClicando.class);
        Map<Integer, String> escolhas = preenchimento.lacunas() == null ? Map.of() : preenchimento.lacunas();
        String respostaCerta = textoCompleto();
        for (Lacuna lacuna : lacunas) {
            String escolhida = escolhas.get(lacuna.id());
            exigir(escolhida != null, "Preencha todas as lacunas antes de verificar.");
            if (!lacuna.palavraCorreta().equals(escolhida)) {
                String motivo = lacuna.buscar(escolhida)
                        .map(PalavraDoBanco::motivo)
                        .filter(Validacoes::preenchido)
                        .orElse(MOTIVO_PADRAO);
                return Correcao.erro(motivo, respostaCerta);
            }
        }
        return Correcao.acerto(respostaCerta);
    }

    private Set<Integer> marcadoresNoTexto() {
        Set<Integer> marcadores = new TreeSet<>();
        Matcher encontrados = MARCADOR.matcher(texto);
        while (encontrados.find()) {
            marcadores.add(Integer.parseInt(encontrados.group(1)));
        }
        return marcadores;
    }

    private String textoCompleto() {
        Map<Integer, String> corretas = lacunas.stream().collect(Collectors.toMap(Lacuna::id, Lacuna::palavraCorreta));
        return MARCADOR.matcher(texto)
                .replaceAll(marcador -> Matcher.quoteReplacement(
                        corretas.getOrDefault(Integer.parseInt(marcador.group(1)), marcador.group())));
    }
}
