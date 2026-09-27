package com.authservice.authservice.tools;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordGenerator {

    public static void main(String[] args) {

        generate("admin");
        generate("user");
        generate("jefeproyecto");
    }

    private static void generate(String password) {
        //codigo para compilar mvn compile exec:java "-Dexec.mainClass=com.authservice.authservice.tools.PasswordGenerator"
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));

        System.out.println("================================");
        System.out.println("PASSWORD: " + password);
        System.out.println("HASH GENERATED:");
        System.out.println(hash);
        System.out.println("================================");
    }
}