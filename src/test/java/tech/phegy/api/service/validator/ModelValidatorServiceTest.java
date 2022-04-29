package tech.phegy.api.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelValidatorServiceTest {
    @Mock
    Validator validator;

    ModelValidatorService modelValidatorService;

    @BeforeEach
    void setUp() {
        modelValidatorService = new ModelValidatorService(validator);
    }

    @Test
    @DisplayName("Should not throw exception when no errors found")
    void shouldNotThrowExceptionWhenNoErrorsFound() {
        Object toValidate = new Object();
        when(validator.validate(toValidate)).thenReturn(Collections.emptySet());

        modelValidatorService.validate(toValidate);
    }

    @Test
    @DisplayName("Should throw exception when error found")
    void shouldThrowExceptionWhenErrorFound() {
        Object toValidate = new Object();
        String errorMessage = "error message";
        ConstraintViolation<Object> constraintViolation = Mockito.mock(ConstraintViolation.class);

        when(constraintViolation.getMessage()).thenReturn(errorMessage);

        when(validator.validate(toValidate)).thenReturn(Set.of(constraintViolation));

        assertThatThrownBy(() -> modelValidatorService.validate(toValidate))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(errorMessage);
    }
}