package com.swp391.scientific_journal_tracker.dto.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class BackfillOpenAlexRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void acceptsTwentyThousandResults() {
        BackfillOpenAlexRequest request = validRequest(20_000);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMoreThanTwentyThousandResults() {
        BackfillOpenAlexRequest request = validRequest(20_001);

        assertFalse(validator.validate(request).isEmpty());
    }

    private BackfillOpenAlexRequest validRequest(int maxResults) {
        BackfillOpenAlexRequest request = new BackfillOpenAlexRequest();
        request.setFromYear(2020);
        request.setToYear(2026);
        request.setFieldIds(List.of("17"));
        request.setMaxResults(maxResults);
        return request;
    }
}
