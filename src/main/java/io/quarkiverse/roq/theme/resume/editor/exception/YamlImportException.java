package io.quarkiverse.roq.theme.resume.editor.exception;

/// Runtime exception for YAML import failures.
/// Thrown when importing Bio data from files or URLs fails.
public class YamlImportException extends RuntimeException {

    public YamlImportException(String message) {
        super(message);
    }

    public YamlImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
