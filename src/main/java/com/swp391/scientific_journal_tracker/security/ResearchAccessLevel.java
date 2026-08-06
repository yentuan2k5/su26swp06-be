package com.swp391.scientific_journal_tracker.security;

/**
 * Mức truy cập cho các chức năng phân tích học thuật.
 *
 * BASIC dành cho Lecturer, còn FULL dành cho Researcher và Admin.
 */
public enum ResearchAccessLevel {
    BASIC,
    FULL
}
