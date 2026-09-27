package com.authservice.shared.util;

import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class JwtKeyGenerator {

    public static void main(String[] args) throws Exception {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);

        KeyPair pair = keyGen.generateKeyPair();

        // PRIVATE KEY
        String privateKey = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(pair.getPrivate().getEncoded())
                +
                "\n-----END PRIVATE KEY-----";

        // PUBLIC KEY
        String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(pair.getPublic().getEncoded())
                +
                "\n-----END PUBLIC KEY-----";

        // Guardar archivos
        try (FileOutputStream fos = new FileOutputStream("privateKey.pem")) {
            fos.write(privateKey.getBytes());
        }

        try (FileOutputStream fos = new FileOutputStream("publicKey.pem")) {
            fos.write(publicKey.getBytes());
        }

        System.out.println("Keys generadas en la raíz del proyecto");
    }
}