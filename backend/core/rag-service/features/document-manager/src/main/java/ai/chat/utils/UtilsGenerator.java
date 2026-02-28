package ai.chat.utils;

import lombok.experimental.UtilityClass;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@UtilityClass
public class UtilsGenerator {
    private static final int DEFAULT_VERIFICATION_BYTES = 32;
    private final SecureRandom secureRandom;
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
    private static final int DEFAULT_REFRESH_BYTES = 16;

    static {
        try {
            secureRandom = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e + "Failed to initialize SecureRandom instance");
        }
    }

    private static String generateName() {
        byte[] randomBytes = new byte[DEFAULT_VERIFICATION_BYTES];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);

    }

    public static String generateUniqueObjectName(String objectName) {
        return UUID.randomUUID() + "_" + objectName;
    }


    public static String getHash256FromFile(File file) {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        } catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
