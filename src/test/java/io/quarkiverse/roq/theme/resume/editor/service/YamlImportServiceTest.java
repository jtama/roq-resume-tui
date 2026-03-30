package io.quarkiverse.roq.theme.resume.editor.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkiverse.roq.theme.resume.editor.exception.YamlImportException;
import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkus.test.junit.QuarkusTest;

/// Tests for YamlImportService.
/// Tests cover:
/// - Valid YAML file import via public importBio() method
/// - Invalid/malformed YAML
/// - Missing file handling
/// - Empty file handling
/// - Empty/null source handling
/// - Nested items parsing
@QuarkusTest
@DisplayName("YamlImportService Tests")
class YamlImportServiceTest {

    @Inject
    YamlImportService importService;

    @Inject
    YamlExportService exportService;

    @TempDir
    Path tempDir;

    private Path validYamlFile;
    private Path invalidYamlFile;
    private Path emptyFile;

    @BeforeEach
    void setup() throws Exception {
        /// Create a valid YAML file with bio structure
        String validYaml = """
                list:
                  - title: "Experience"
                    items:
                      - header: "Senior Developer"
                        title: "Acme Corp"
                        link: "https://acme.com"
                        content: "Worked on backend systems"
                      - header: "Developer"
                        title: "TechStart"
                        link: "https://techstart.com"
                        content: "Built web applications"
                  - title: "Education"
                    items:
                      - header: "BS Computer Science"
                        title: "State University"
                        link: "https://university.edu"
                        content: "Graduated with honors"
                """;
        validYamlFile = tempDir.resolve("valid_bio.yaml");
        Files.writeString(validYamlFile, validYaml);

        /// Create an invalid YAML file
        String invalidYaml = """
                list:
                  - title: "Experience"
                    items: [
                      - header: "Senior Developer"  # Invalid YAML syntax
                """;
        invalidYamlFile = tempDir.resolve("invalid_bio.yaml");
        Files.writeString(invalidYamlFile, invalidYaml);

        /// Create an empty file
        emptyFile = tempDir.resolve("empty.yaml");
        Files.writeString(emptyFile, "");
    }

    @Test
    @DisplayName("Should import valid YAML from file")
    void testImportValidYamlFile() {
        Optional<Bio> result = importService.importFromFile(validYamlFile);

        assertTrue(result.isPresent(), "Should successfully parse valid YAML");
        Bio bio = result.get();
        assertNotNull(bio.list(), "Bio should have sections");
        assertEquals(2, bio.list().size(), "Should have 2 sections");
        assertEquals("Experience", bio.list().get(0).title());
        assertEquals("Education", bio.list().get(1).title());
    }

    @Test
    @DisplayName("Should fail gracefully on missing file")
    void testImportMissingFile() {
        Path nonExistent = tempDir.resolve("nonexistent.yaml");
        assertThrows(YamlImportException.class, () -> {
            importService.importFromFile(nonExistent);
        }, "Should throw YamlImportException for missing file");
    }

    @Test
    @DisplayName("Should fail on invalid YAML syntax")
    void testImportInvalidYaml() {
        assertThrows(YamlImportException.class, () -> {
            importService.importFromFile(invalidYamlFile);
        }, "Should throw YamlImportException for invalid YAML");
    }

    @Test
    @DisplayName("Should fail on empty file")
    void testImportEmptyFile() {
        assertThrows(YamlImportException.class, () -> {
            importService.importFromFile(emptyFile);
        }, "Should throw YamlImportException for empty YAML");
    }

    @Test
    @DisplayName("Should correctly import files via public importBio() method")
    void testImportBioAutoDetection() {
        // Test file import via importBio() - should auto-detect it's a file path
        Optional<Bio> fileResult = importService.importBio(validYamlFile.toString());
        assertTrue(fileResult.isPresent(), "Should successfully import file");

        Bio bio = fileResult.get();
        assertNotNull(bio.list(), "Bio should have sections");
        assertEquals(2, bio.list().size(), "Should have 2 sections");
        assertEquals("Experience", bio.list().get(0).title());
    }

    @Test
    @DisplayName("Should reject empty or null source in importBio()")
    void testImportBioEmptySource() {
        assertThrows(YamlImportException.class, () -> {
            importService.importBio("");
        }, "Should throw YamlImportException for empty source");

        assertThrows(YamlImportException.class, () -> {
            importService.importBio(null);
        }, "Should throw YamlImportException for null source");
    }

    @Test
    @DisplayName("Should parse nested items correctly")
    void testNestedItemsParsing() {
        String yamlWithSubItems = """
                list:
                  - title: "Work"
                    items:
                      - title: "Project A"
                        header: "Tech Lead"
                        subItems:
                          - title: "Subproject A1"
                            header: "Developer"
                """;
        Path nestedFile = tempDir.resolve("nested.yaml");
        try {
            Files.writeString(nestedFile, yamlWithSubItems);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Optional<Bio> result = importService.importFromFile(nestedFile);
        assertTrue(result.isPresent());
        Bio bio = result.get();
        Bio.Item rootItem = bio.list().get(0).items().get(0);
        assertEquals("Project A", rootItem.title());
        assertNotNull(rootItem.subItems());
        assertEquals(1, rootItem.subItems().size());
        assertEquals("Subproject A1", rootItem.subItems().get(0).title());
    }

    @Test
    @DisplayName("Should import YAML with unwrapped list (no 'list:' key)")
    void testImportUnwrappedList() {
        String unwrappedYaml = """
                - title: "Experience"
                  items:
                    - header: "Senior Dev"
                      title: "Acme Corp"
                      link: "https://acme.com"
                - title: "Education"
                  items:
                    - header: "BS CS"
                      title: "University"
                      link: "https://university.edu"
                """;
        Path unwrappedFile = tempDir.resolve("unwrapped.yaml");
        try {
            Files.writeString(unwrappedFile, unwrappedYaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Optional<Bio> result = importService.importFromFile(unwrappedFile);
        assertTrue(result.isPresent(), "Should successfully parse unwrapped YAML list");
        Bio bio = result.get();
        assertNotNull(bio.list(), "Bio should have sections");
        assertEquals(2, bio.list().size(), "Should have 2 sections");
        assertEquals("Experience", bio.list().get(0).title());
        assertEquals("Education", bio.list().get(1).title());
    }

    @Test
    @DisplayName("Should import Bio items with logos")
    void testImportWithLogos() {
        String yamlWithLogos = """
                - title: "Work"
                  items:
                    - header: "Tech Lead"
                      title: "Company A"
                      link: "https://company-a.com"
                      logo:
                        label: "Company A"
                        imageUrl: "https://example.com/logo-a.png"
                        link: "https://company-a.com"
                    - header: "Developer"
                      title: "Company B"
                      logo:
                        label: "Company B"
                        imageUrl: "https://example.com/logo-b.png"
                """;
        Path logoFile = tempDir.resolve("logos.yaml");
        try {
            Files.writeString(logoFile, yamlWithLogos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Optional<Bio> result = importService.importFromFile(logoFile);
        assertTrue(result.isPresent(), "Should import YAML with logos");
        Bio bio = result.get();

        Bio.Item firstItem = bio.list().get(0).items().get(0);
        assertNotNull(firstItem.logo(), "First item should have logo");
        assertEquals("Company A", firstItem.logo().label());
        assertEquals("https://example.com/logo-a.png", firstItem.logo().imageUrl());
        assertEquals("https://company-a.com", firstItem.logo().link());

        Bio.Item secondItem = bio.list().get(0).items().get(1);
        assertNotNull(secondItem.logo(), "Second item should have logo");
        assertEquals("Company B", secondItem.logo().label());
        assertEquals("https://example.com/logo-b.png", secondItem.logo().imageUrl());
        assertNull(secondItem.logo().link(), "Second item logo link should be null");
    }

    @Test
    @DisplayName("Should support round-trip export-import with logos")
    void testRoundTripWithLogos() {
        // Create a Bio with logos
        var logo = new Bio.Logo("Tech Corp", "https://example.com/logo.png", "https://techcorp.com");
        var item = new Bio.Item(null, "CTO", "Tech Corporation", "https://techcorp.com",
                "Worked on architecture", logo, null, null, null, List.of(), List.of());
        var section = new Bio.Section(null, "Experience", List.of(item));
        var bio = new Bio(List.of(section));

        // Export to file
        try {
            exportService.exportBio(bio, tempDir.toString());
        } catch (Exception e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }

        // Import back
        Path exportedFile = tempDir.resolve("bio.yaml");
        assertTrue(Files.exists(exportedFile), "Exported file should exist");

        Optional<Bio> importedBio = importService.importFromFile(exportedFile);
        assertTrue(importedBio.isPresent(), "Should re-import exported Bio");

        // Verify round-trip preserved logos
        Bio result = importedBio.get();
        assertEquals(1, result.list().size(), "Should have 1 section");
        assertEquals(1, result.list().get(0).items().size(), "Section should have 1 item");

        Bio.Item importedItem = result.list().get(0).items().get(0);
        assertNotNull(importedItem.logo(), "Logo should be preserved in round-trip");
        assertEquals("Tech Corp", importedItem.logo().label());
        assertEquals("https://example.com/logo.png", importedItem.logo().imageUrl());
        assertEquals("https://techcorp.com", importedItem.logo().link());
    }
}
