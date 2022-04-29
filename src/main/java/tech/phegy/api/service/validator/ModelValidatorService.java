package tech.phegy.api.service.validator;

import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for validating database models.
 *
 * @author Nikita
 */
@Service
public class ModelValidatorService {
    private final Validator validator;

    /**
     * Constructs a ModelValidatorService with needed dependencies.
     */
    public ModelValidatorService(Validator validator) {
        this.validator = validator;
    }

    public <T> void validate(T obj) {
        List<String> errorMessages = getErrorMessages(obj);

        if (errorMessages.size() > 0) {
            errorMessages.sort(String::compareTo);

            errorMessages.forEach(errorMessage -> {
                throw new RuntimeException(errorMessage);
            });
        }
    }

    private <T> List<String> getErrorMessages(T obj) {
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(obj);

        return constraintViolations
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
    }
}
