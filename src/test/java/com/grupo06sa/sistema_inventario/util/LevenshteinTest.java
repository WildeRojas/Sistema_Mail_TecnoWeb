package com.grupo06sa.sistema_inventario.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class LevenshteinTest {

    @Test
    void distanciaEntreCadenasIgualesEsCero() {
        assertThat(Levenshtein.distancia("HELP", "HELP")).isZero();
        assertThat(Levenshtein.distancia("", "")).isZero();
    }

    @Test
    void distanciaContraCadenaVaciaEsLaLongitudDeLaOtra() {
        assertThat(Levenshtein.distancia("", "abc")).isEqualTo(3);
        assertThat(Levenshtein.distancia("abc", "")).isEqualTo(3);
    }

    @Test
    void distanciaConValoresNulosSeTrataComoCadenaVacia() {
        assertThat(Levenshtein.distancia(null, "abc")).isEqualTo(3);
        assertThat(Levenshtein.distancia("abc", null)).isEqualTo(3);
        assertThat(Levenshtein.distancia(null, null)).isZero();
    }

    @Test
    void distanciaClasicaKittenSitting() {
        assertThat(Levenshtein.distancia("kitten", "sitting")).isEqualTo(3);
    }

    @Test
    void distanciaPorTransposicionDeDosCaracteresAdyacentes() {

        assertThat(Levenshtein.distancia("HEPL", "HELP")).isEqualTo(2);
    }

    @Test
    void distanciaPorUnaSolaInsercionOEliminacion() {
        assertThat(Levenshtein.distancia("HELP", "HELPS")).isEqualTo(1);
        assertThat(Levenshtein.distancia("HELPS", "HELP")).isEqualTo(1);
    }

    @Test
    void distanciaEsSimetrica() {
        assertThat(Levenshtein.distancia("INS_COMPRA", "INS_COMPRAS"))
            .isEqualTo(Levenshtein.distancia("INS_COMPRAS", "INS_COMPRA"));
    }

    @Test
    void prefijoComunCuentaCaracteresIniciales() {
        assertThat(Levenshtein.prefijoComun("HELP", "HELP")).isEqualTo(4);
        assertThat(Levenshtein.prefijoComun("HEL", "HELP")).isEqualTo(3);
        assertThat(Levenshtein.prefijoComun("abc", "xyz")).isZero();
    }

    @Test
    void prefijoComunIgnoraMayusculasMinusculas() {
        assertThat(Levenshtein.prefijoComun("help", "HELP")).isEqualTo(4);
        assertThat(Levenshtein.prefijoComun("HeLp", "hELP")).isEqualTo(4);
    }

    @Test
    void prefijoComunConValoresNulosEsCero() {
        assertThat(Levenshtein.prefijoComun(null, "HELP")).isZero();
        assertThat(Levenshtein.prefijoComun("HELP", null)).isZero();
    }

    @Test
    void heplEsMasParecidoAHelpQueOtrosComandosDelCatalogo() {
        List<String> candidatos = List.of(
            "HELP", "INS_COMPRA", "LIS_PRODUCTO", "PAGAR_COMPRA", "REP_INVENTARIO", "TRASLADO_STOCK"
        );

        String masParecido = candidatos.stream()
            .min(Comparator.comparingInt(candidato -> Levenshtein.distancia("HEPL", candidato)))
            .orElseThrow();

        assertThat(masParecido).isEqualTo("HELP");
    }

    @Test
    void lisProductoConTypoEsMasParecidoALisProductoQueAOtrosComandos() {
        List<String> candidatos = List.of("LIS_PRODUCTO", "LIS_PROVEEDOR", "LIS_COMPRA", "GET_PRODUCTO");

        String masParecido = candidatos.stream()
            .min(Comparator.comparingInt(candidato -> Levenshtein.distancia("LIS_PRODCUTO", candidato)))
            .orElseThrow();

        assertThat(masParecido).isEqualTo("LIS_PRODUCTO");
    }
}
