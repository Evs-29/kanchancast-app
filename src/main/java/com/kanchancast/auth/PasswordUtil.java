package com.kanchancast.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {

    // Tunables
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {}

    /** Basic strength check: 8+ chars, at least one letter and one digit. */
    public static boolean isStrongEnough(String raw) {
        if (raw == null || raw.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : raw.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) return true;
        }
        return false;
    }

    /** Hash a password with PBKDF2+random . */
    public static String hashPassword(String raw) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            RNG.nextBytes(salt);

            byte[] hash = pbkdf2(raw.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return "pbkdf2$" + ITERATIONS + "$" +
                    Base64.getEncoder().encodeToString(salt) + "$" +
                    Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    /** Verify raw password against a stored string created by {link #hashPassword(String)}. */
    public static boolean verifyPassword(String raw, String stored) {
        if (raw == null || stored == null) return false;
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);

            byte[] actual = pbkdf2(raw.toCharArray(), salt, iterations, expected.length * 8);
            return slowEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    // ---- helpers ----
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }
    public static String hash(String password) {
        if (password == null || password.isBlank()) return "";

        int iterations = 65536;
        byte[] salt = java.util.UUID.randomUUID().toString().substring(0, 16).getBytes();
        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, 160);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return String.format("pbkdf2$%d$%s$%s",
                    iterations,
                    java.util.Base64.getEncoder().encodeToString(salt),
                    java.util.Base64.getEncoder().encodeToString(hash)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
