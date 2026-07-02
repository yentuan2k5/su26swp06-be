package com.swp391.scientific_journal_tracker.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.swp391.scientific_journal_tracker.dto.response.JournalResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.service.JournalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    public List<JournalResponse> getAllJournals() {
        return journalService.getAllJournals();
    }

    @GetMapping("/search")
    public List<JournalResponse> searchJournals(@RequestParam String keyword) {
        return journalService.searchJournals(keyword);
    }

    @GetMapping("/top")
    public List<JournalResponse> getTopJournals(
            @RequestParam(defaultValue = "10") int limit) {
        return journalService.getTopJournals(limit);
    }

    @GetMapping("/following")
    public List<JournalResponse> getMyFollowingJournals(Authentication authentication) {
        return journalService.getMyFollowingJournals(authentication);
    }

    @PostMapping("/{journalId}/follow")
    public JournalResponse followJournal(
            @PathVariable Long journalId,
            Authentication authentication) {
        return journalService.followJournal(journalId, authentication);
    }

    @DeleteMapping("/{journalId}/follow")
    public JournalResponse unfollowJournal(
            @PathVariable Long journalId,
            Authentication authentication) {
        return journalService.unfollowJournal(journalId, authentication);
    }

    @GetMapping("/{journalId}/follow/check")
    public Map<String, Boolean> checkJournalFollowed(
            @PathVariable Long journalId,
            Authentication authentication) {
        boolean followed = journalService.isJournalFollowed(journalId, authentication);

        return Map.of("followed", followed);
    }

    @GetMapping("/{journalId}")
    public JournalResponse getJournalDetail(@PathVariable Long journalId) {
        return journalService.getJournalDetail(journalId);
    }

    @GetMapping("/{journalId}/papers")
    public Page<PaperResponse> getPapersByJournal(
            @PathVariable Long journalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return journalService.getPapersByJournal(journalId, page, size);
    }
}