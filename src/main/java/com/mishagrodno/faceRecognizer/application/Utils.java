package com.mishagrodno.faceRecognizer.application;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The class with utility methods.
 *
 * @author Gomanchuk Mikhail.
 */
public class Utils {

    private static ToIplImage TO_IPL_IMAGE = new ToIplImage();
    private static Java2DFrameConverter PAINT_CONVERTER = new Java2DFrameConverter();

    /**
     * Tries to delete the file.
     *
     * @param file the file to delete.
     */
    public static void safeDelete(final Path file) {
        if (file == null) return;
        try {
            Files.delete(file);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts {@link BufferedImage} to {@link IplImage}.
     *
     * @param image image to convert.
     * @return converted image.
     */
    public static IplImage convertToIplImage(final BufferedImage image) {
        return TO_IPL_IMAGE.convert(PAINT_CONVERTER.convert(image));
    }

    /**
     * Converts {@link Frame} to {@link IplImage}.
     *
     * @param frame frame to convert.
     * @return converted image.
     */
    public static IplImage convertToIplImage(final Frame frame) {
        return TO_IPL_IMAGE.convert(frame);
    }

    /**
     * Converts {@link IplImage} to {@link BufferedImage}.
     *
     * @param image image to convert.
     * @return converted image.
     */
    public static BufferedImage convertToBufferedImage(final IplImage image) {
        return PAINT_CONVERTER.getBufferedImage(TO_IPL_IMAGE.convert(image));
    }
}
