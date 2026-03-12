package uk.gov.dstl.sapient.generator.generator;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IdGenerator {

    /**
     * Generate a deterministic ULID from a seed string.
     * The same seed always produces the same ULID, ensuring persistent object IDs.
     */
    public static String deterministicUlid(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));

            // Use first 16 bytes of the hash as the ULID bytes
            byte[] ulidBytes = new byte[16];
            System.arraycopy(hash, 0, ulidBytes, 0, 16);

            return Ulid.from(ulidBytes).toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Generate a random ULID for report IDs. */
    public static String randomUlid() {
        return UlidCreator.getMonotonicUlid().toString();
    }
}
