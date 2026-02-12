package ai.chat.utils;

import lombok.experimental.UtilityClass;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
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
}
