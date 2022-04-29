package tech.phegy.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class HashingServiceTest {

    HashingService hashingService = new HashingService();

    @ParameterizedTest
    @DisplayName("Should return the same hash for the same words")
    @ValueSource(strings = {"test", " test2 ", "2323i9daj-D#", "!@###$$%%*(&(*))_)*&", "@%$$Q$C#%^&N*M&^M(&*)M#$ DFDFS DFSADFSDFDTD $%", ""})
    void shouldReturnTheSameHashForTheSameWords(String string) {
        String hashed1 = hashingService.hashString(string);
        String hashed2 = hashingService.hashString(string);

        assertThat(hashed1).isEqualTo(hashed2);
    }

    @ParameterizedTest
    @DisplayName("Should return different hash for different words")
    @ValueSource(strings = {"test", " test2 ", "2323i9daj-D#", "!@###$$%%*(&(*))_)*&", "@%$$Q$C#%^&N*M&^M(&*)M#$ DFDFS DFSADFSDFDTD $%", ""})
    void shouldReturnDifferentHashForDifferentWords(String string) {
        final String newString = string + " ";
        String hashed1 = hashingService.hashString(newString);
        String hashed2 = hashingService.hashString(string);

        assertThat(hashed1).isNotEqualTo(hashed2);
    }
}
