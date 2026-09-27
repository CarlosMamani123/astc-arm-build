package com.backoffice.shared.util;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public final class UuidGenerator {

    private UuidGenerator() {
        // Evita instanciación
    }

    /**
     * 🔥 Genera UUID versión 7 (ordenado por tiempo)
     */
    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    /**
     * 🔥 Genera UUID7 en formato String
     */
    public static String generateString() {
        return generate().toString();
    }
}