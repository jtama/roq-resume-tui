package io.quarkiverse.roq.theme.resume.editor.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import dev.tamboui.image.Image;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.ImageScaling;
import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;

import static io.quarkiverse.roq.theme.resume.editor.element.ImageElement.image;

import io.quarkiverse.roq.theme.resume.editor.context.AppContext;

@ApplicationScoped
public class LogoImageLoader {
    private static final Logger logger = Logger.getLogger(LogoImageLoader.class);
    private final AppContext context;

    public LogoImageLoader(AppContext context) {
        // Utility class
        this.context = context;
    }

    /// Load an image widget from a file path (absolute or relative).
    ///
    /// @param imagePath the image path (absolute or relative to working directory)
    /// @param title
    /// @return Optional containing the Image widget, or empty if loading fails
    public Optional<Element> loadLogoImage(String imagePath, String title) {
        if (imagePath == null || imagePath.isBlank()) {
            return Optional.empty();
        }

        try {
            Path path = Paths.get(imagePath);
            var image = Image.builder()
                    .data(ImageData.fromPath(path))
                    .scaling(ImageScaling.FIT)
                    .block(Block.builder()
                            .borders(Borders.ALL)
                            .borderType(BorderType.ROUNDED)
                            .borderStyle(Style.EMPTY.fg(Color.BLUE))
                            .title(Title.from(Line.from(Span.raw(" " + title + " ").blue())))
                            .build())
                    .build();
            return Optional.of(image(image).constraint(Constraint.max(5)));
        } catch (Exception e) {
            context.setException(e);
            return Optional.empty();
        }
    }
}
