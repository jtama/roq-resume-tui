package io.quarkiverse.roq.theme.resume.editor.service;

import java.util.List;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkiverse.roq.theme.resume.editor.model.Resume;
import io.quarkiverse.roq.theme.resume.editor.model.ResumeSummary;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CVServiceTest {

    @Inject
    CVService cvService;

    @Inject
    Flyway flyway;

    @BeforeEach
    public void migrateDB() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    public void testCreateAndGetResume() {
        // Create a new resume
        Resume created = cvService.create("Test Resume", "test-resume");
        assertNotNull(created);
        assertEquals("Test Resume", created.title());
        assertEquals("test-resume", created.slug());

        // Get all resumes and check if the created one is there
        List<ResumeSummary> resumes = cvService.getResumes();
        assertTrue(resumes.stream().anyMatch(r -> r.slug().equals("test-resume")));

        // Get the full resume and check its details
        Resume found = cvService.getResume(created.id()).orElseThrow();
        assertNotNull(found);
        assertEquals("Test Resume", found.title());
        assertNotNull(found.profile());
    }
}
