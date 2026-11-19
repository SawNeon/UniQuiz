package br.com.unimed.uniquiz.certificado.dominio;

import br.com.unimed.uniquiz.certificado.dominio.PosicaoDoCampo.Alinhamento;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public record LayoutDoCertificado(List<PosicaoDoCampo> campos) {

    public static final double TAMANHO_MINIMO = 6;
    public static final double TAMANHO_MAXIMO = 96;
    public static final int PREFIXO_MAXIMO = 80;
    private static final Pattern COR = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final String VERDE_UNIMED = "#00995D";
    private static final String VERDE_ESCURO = "#004E4C";

    public LayoutDoCertificado {
        exigir(campos != null && !campos.isEmpty(), "Informe a posição dos campos do certificado.");
        Set<CampoDoCertificado> vistos = EnumSet.noneOf(CampoDoCertificado.class);
        for (PosicaoDoCampo posicao : campos) {
            exigir(posicao != null && posicao.campo() != null, "Todo campo precisa dizer qual dado ele mostra.");
            exigir(vistos.add(posicao.campo()), "Cada campo do certificado só pode aparecer uma vez.");
            validar(posicao);
        }
        exigir(vistos.size() == CampoDoCertificado.values().length, "Informe a posição de todos os campos.");
        campos = campos.stream()
                .map(posicao -> new PosicaoDoCampo(
                        posicao.campo(),
                        posicao.visivel(),
                        posicao.x(),
                        posicao.y(),
                        posicao.tamanho(),
                        posicao.cor().toUpperCase(Locale.ROOT),
                        posicao.alinhamento(),
                        posicao.negrito(),
                        posicao.larguraMaxima(),
                        posicao.prefixoOuVazio()))
                .sorted(Comparator.comparing(PosicaoDoCampo::campo))
                .toList();
    }

    public static LayoutDoCertificado padrao() {
        return new LayoutDoCertificado(List.of(
                new PosicaoDoCampo(
                        CampoDoCertificado.NOME, true, 50, 41.2, 30, VERDE_UNIMED, Alinhamento.CENTRO, true, 85, ""),
                new PosicaoDoCampo(
                        CampoDoCertificado.MODULO, true, 50, 55.4, 24, VERDE_ESCURO, Alinhamento.CENTRO, true, 85, ""),
                new PosicaoDoCampo(
                        CampoDoCertificado.DATA, true, 50, 68.9, 14, VERDE_ESCURO, Alinhamento.CENTRO, false, 60, ""),
                new PosicaoDoCampo(
                        CampoDoCertificado.CODIGO,
                        true,
                        5.7,
                        89.9,
                        12,
                        VERDE_ESCURO,
                        Alinhamento.ESQUERDA,
                        true,
                        50,
                        "Código de validação: "),
                new PosicaoDoCampo(
                        CampoDoCertificado.ENDERECO_DE_VALIDACAO,
                        true,
                        5.7,
                        92.9,
                        11,
                        VERDE_ESCURO,
                        Alinhamento.ESQUERDA,
                        false,
                        70,
                        "Confira em ")));
    }

    public PosicaoDoCampo posicaoDo(CampoDoCertificado campo) {
        return campos.stream()
                .filter(posicao -> posicao.campo() == campo)
                .findFirst()
                .orElseThrow();
    }

    private static void validar(PosicaoDoCampo posicao) {
        exigir(entre(posicao.x(), 0, 100) && entre(posicao.y(), 0, 100), "A posição precisa ficar dentro da página.");
        exigir(
                entre(posicao.tamanho(), TAMANHO_MINIMO, TAMANHO_MAXIMO),
                "O tamanho da letra precisa ficar entre %.0f e %.0f.".formatted(TAMANHO_MINIMO, TAMANHO_MAXIMO));
        exigir(posicao.cor() != null && COR.matcher(posicao.cor()).matches(), "Use uma cor no formato #RRGGBB.");
        exigir(posicao.alinhamento() != null, "Escolha o alinhamento de cada campo.");
        exigir(entre(posicao.larguraMaxima(), 5, 100), "A largura máxima precisa ficar entre 5% e 100% da página.");
        exigir(
                posicao.prefixoOuVazio().length() <= PREFIXO_MAXIMO,
                "O texto antes do campo pode ter até %d letras.".formatted(PREFIXO_MAXIMO));
        exigir(
                posicao.prefixoOuVazio().chars().noneMatch(Character::isISOControl),
                "O texto antes do campo não pode ter quebra de linha.");
    }

    private static boolean entre(double valor, double minimo, double maximo) {
        return !Double.isNaN(valor) && valor >= minimo && valor <= maximo;
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new ErroDeNegocio(mensagem);
        }
    }
}
