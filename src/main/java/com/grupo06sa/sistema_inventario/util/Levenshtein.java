package com.grupo06sa.sistema_inventario.util;

import java.util.Locale;

public final class Levenshtein {

    private Levenshtein() {
    }

    public static int distancia(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        int n = x.length();
        int m = y.length();

        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }

        int[] filaAnterior = new int[m + 1];
        int[] filaActual = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            filaAnterior[j] = j;
        }

        for (int i = 1; i <= n; i++) {
            filaActual[0] = i;
            char caracterX = x.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int costoSustitucion = caracterX == y.charAt(j - 1) ? 0 : 1;
                int borrar = filaAnterior[j] + 1;
                int insertar = filaActual[j - 1] + 1;
                int sustituir = filaAnterior[j - 1] + costoSustitucion;
                filaActual[j] = Math.min(Math.min(borrar, insertar), sustituir);
            }
            int[] intercambio = filaAnterior;
            filaAnterior = filaActual;
            filaActual = intercambio;
        }

        return filaAnterior[m];
    }

    public static int prefijoComun(String a, String b) {
        String x = a == null ? "" : a.toUpperCase(Locale.ROOT);
        String y = b == null ? "" : b.toUpperCase(Locale.ROOT);
        int maximo = Math.min(x.length(), y.length());
        int i = 0;
        while (i < maximo && x.charAt(i) == y.charAt(i)) {
            i++;
        }
        return i;
    }
}
