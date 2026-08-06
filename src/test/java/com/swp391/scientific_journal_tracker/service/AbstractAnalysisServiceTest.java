package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.swp391.scientific_journal_tracker.dto.response.AbstractAnalysisResponse;

class AbstractAnalysisServiceTest {

    private final AbstractAnalysisService abstractAnalysisService = new AbstractAnalysisService();

    @Test
    void extractsAcademicHighlightsFromAbstract() {
        String abstractText = "This study investigates a challenge in multimodal learning. "
                + "We propose a novel framework for robust fusion. "
                + "Experiments demonstrate improved performance on three datasets.";

        AbstractAnalysisResponse response = abstractAnalysisService.analyze(abstractText);

        assertEquals("ABSTRACT_AVAILABLE", response.getSource());
        assertEquals(1, response.getObjectiveHighlights().size());
        assertEquals(1, response.getProblemHighlights().size());
        assertEquals(1, response.getMethodHighlights().size());
        assertEquals(1, response.getResultHighlights().size());
    }

    @Test
    void returnsEmptyAnalysisWhenAbstractIsMissing() {
        AbstractAnalysisResponse response = abstractAnalysisService.analyze(null);

        assertEquals("ABSTRACT_NOT_AVAILABLE", response.getSource());
        assertTrue(response.getObjectiveHighlights().isEmpty());
        assertTrue(response.getProblemHighlights().isEmpty());
        assertTrue(response.getMethodHighlights().isEmpty());
        assertTrue(response.getResultHighlights().isEmpty());
    }

    @Test
    void doesNotTreatHoweverAloneAsAProblemIndicator() {
        AbstractAnalysisResponse response = abstractAnalysisService.analyze(
                "However, the evaluation demonstrates improved performance.");

        assertFalse(response.getResultHighlights().isEmpty());
        assertTrue(response.getProblemHighlights().isEmpty());
    }
}
