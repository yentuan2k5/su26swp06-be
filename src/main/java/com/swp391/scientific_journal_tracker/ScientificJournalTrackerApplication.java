package com.swp391.scientific_journal_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScientificJournalTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScientificJournalTrackerApplication.class, args);

	}

}
