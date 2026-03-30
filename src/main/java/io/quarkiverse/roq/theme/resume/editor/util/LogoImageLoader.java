package io.quarkiverse.roq.theme.resume.editor.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.jboss.logging.Logger;

import dev.tamboui.image.Image;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.ImageScaling;

/// Utility for loading and rendering logo images from file paths.
/// Supports both absolute and relative paths (relative to current working directory).
public class LogoImageLoader {
    private static final Logger logger = Logger.getLogger(LogoImageLoader.class);

    private LogoImageLoader() {
        // Utility class
    }

    /// Load an image widget from a file path (absolute or relative).
    /// @param imagePath the image path (absolute or relative to working directory)
    /// @return Optional containing the Image widget, or empty if loading fails
    public static Optional<Image> loadLogoImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return Optional.empty();
        }

        try {
            Path path = Paths.get(imagePath);
            var image = Image.builder()
                    .data(ImageData.fromPath(path))
                    .scaling(ImageScaling.FIT)
                    .build();
            return Optional.of(image);
        } catch (IOException e) {
            logger.warn("Failed to load logo image from path: " + imagePath, e);
            return Optional.empty();
        }
    }
}
