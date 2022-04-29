package tech.phegy.api.service.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerifierTest {
    EmailVerifier emailVerifier = new EmailVerifier();

    @ParameterizedTest
    @DisplayName("Should verify that email is valid")
    @ValueSource(strings = {"test@abv.bg", "gogoeqk@gmai.com", "pepe@mail.bg", "test@test.com", "s@a.b"})
    void shouldVerifyThatEmailIsValid(String email) {
        boolean isValid = emailVerifier.isValidEmail(email);
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Should verify that email is not valid")
    @ValueSource(strings = {"test@bg", "@gmail.com", "", " ", "cool-mail.com"})
    void shouldVerifyThatEmailIsNotValid(String email) {
        boolean isValid = emailVerifier.isValidEmail(email);
        assertThat(isValid).isFalse();
    }
}