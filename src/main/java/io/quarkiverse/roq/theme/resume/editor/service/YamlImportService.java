package io.quarkiverse.roq.theme.resume.editor.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;

/// Service for importing Bio data from YAML sources (files and URLs).
/// Supports both local file paths and remote HTTP/HTTPS URLs.
/// All parsing is delegated to Jackson with YAML support.
@ApplicationScoped
public class YamlImportService {

    private final ObjectMapper yamlMapper;
    private final HttpClient httpClient;

    public YamlImportService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /// Import Bio from a local file path.
    /// @param path the Path to the YAML file
    /// @return Optional containing the parsed Bio, or empty if parsing fails
    /// @throws IOException if the file cannot be read
    public Optional<Bio> importFromFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("File is not readable: " + path);
        }
        try {
            var yamlContent = Files.readString(path);
            return Optional.of(yamlMapper.readValue(yamlContent, Bio.class));
        } catch (IOException e) {
            throw new IOException("Failed to parse YAML from file: " + path, e);
        }
    }

    /// Import Bio from a remote URL (HTTP/HTTPS).
    /// Uses virtual threads internally for I/O-bound operations.
    /// @param url the HTTP/HTTPS URL to fetch YAML from
    /// @return Optional containing the parsed Bio, or empty if parsing fails
    /// @throws IOException if the URL cannot be fetched or content is invalid
    private Optional<Bio> importFromUrl(String url) throws IOException {
        try {
            var uri = URI.create(url);
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP error: " + response.statusCode() + " from " + url);
            }

            var yamlContent = response.body();
            if (yamlContent.isBlank()) {
                throw new IOException("Empty response body from URL: " + url);
            }

            return Optional.of(yamlMapper.readValue(yamlContent, Bio.class));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted while fetching from URL: " + url, e);
        } catch (IOException e) {
            throw new IOException("Failed to import Bio from URL: " + url, e);
        }
    }

    /// Determine if the given string is a URL or a file path.
    /// Returns true if it starts with http:// or https://, false otherwise.
    /// @param source the source string
    /// @return true if it appears to be a URL
    private boolean isUrl(String source) {
        return source != null && (source.startsWith("http://") || source.startsWith("https://"));
    }

    /// Import Bio from either a file path or URL.
    /// Automatically detects whether the source is a URL or file path.
    /// @param source file path or HTTP/HTTPS URL to import from
    /// @return Optional containing the parsed Bio, or empty if parsing fails
    /// @throws IOException if the import fails for any reason
    public Optional<Bio> importBio(String source) throws IOException {
        if (source == null || source.isBlank()) {
            throw new IOException("Source path or URL cannot be empty");
        }

        return isUrl(source)
                ? importFromUrl(source)
                : importFromFile(java.nio.file.Path.of(source));
    }
}
