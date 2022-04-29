package tech.phegy.api.service;

import com.google.common.hash.Hashing;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Simple hashing service.
 * Mainly used for hashing sensitive data (refresh tokens, activation tokens) before saving it to the database
 *
 * @author Nikita
 */
@Service
public class HashingService {
    /**
     * Performs hashing algorithm for a given string
     *
     * @param string string to be hashed
     * @return hashed string
     */
    public String hashString(String string) {
        // uses google guava sha256 hashing algorithm to hash provided string
        return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
    }
}
