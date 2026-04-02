package io.quarkiverse.roq.theme.resume.editor.element;

import dev.tamboui.image.Image;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.widget.Widget;

public class ImageElement<T extends Widget> extends StyledElement<ImageElement<Image>> {

    private final Image image;

    public ImageElement(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("Widget cannot be null");
        }
        this.image = image;
    }

    /**
     * Creates a new ImageElement wrapping the given image.
     * <p>
     *
     * @param <T> the type of the image
     * @param image the image to wrap
     * @return a new GenericWidgetElement
     * @throws IllegalArgumentException if image is null
     */
    public static ImageElement<Image> image(Image image) {
        return new ImageElement(image);
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.of(5, 5);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        frame.renderWidget(image, area);
    }
}
