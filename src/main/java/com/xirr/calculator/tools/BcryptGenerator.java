package com.xirr.calculator.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Console;
import java.util.Scanner;

public final class BcryptGenerator {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private BcryptGenerator() {
    }

    public static void main(String[] args) {
        String password = args.length > 0 ? String.join(" ", args) : promptForPassword();
        if (password == null || password.isBlank()) {
            System.err.println("Password cannot be blank.");
            System.exit(1);
        }

        String hash = PASSWORD_ENCODER.encode(password);

        System.out.println("BCrypt hash:");
        System.out.println(hash);
        System.out.println();
        System.out.println("application.yml snippet:");
        System.out.println("password-hash: \"" + hash + "\"");
    }

    private static String promptForPassword() {
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword("Enter password: ");
            return password == null ? "" : new String(password);
        }

        System.out.print("Enter password: ");
        return new Scanner(System.in).nextLine();
    }
}
