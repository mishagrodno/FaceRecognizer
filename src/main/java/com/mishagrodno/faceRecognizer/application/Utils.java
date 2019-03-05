package com.mishagrodno.faceRecognizer.application;

import org.bytedeco.javacpp.indexer.DoubleRawIndexer;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.getRotationMatrix2D;

/**
 * The class with utility methods.
 *
 * @author Gomanchuk Mikhail.
 */
public class Utils {

    public static ToIplImage TO_IPL_IMAGE = new ToIplImage();
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
    public static BufferedImage convertToBufferedImage(final Mat image) {
        return PAINT_CONVERTER.getBufferedImage(TO_IPL_IMAGE.convert(image));
    }

    public static Mat rotate(Mat image, double angle) {
        final Mat dst = new Mat();

        final Mat rotationMatrix = getRotationMatrix2D(new Point2f(image.size().width() / 2.0f, image.size().height() / 2.0f), angle, 1);
        final DoubleRawIndexer indexer = rotationMatrix.createIndexer();

        final Rect2f rotatedRect = new RotatedRect(new Point2f(), new Size2f(image.size().width(), image.size().height()), (float) angle)
                .boundingRect2f();

        indexer.put(0, 2, indexer.get(0, 2) + rotatedRect.width() / 2.0 - image.cols() / 2.0);
        indexer.put(1, 2, indexer.get(1, 2) + rotatedRect.height() / 2.0 - image.rows() / 2.0);

        opencv_imgproc.warpAffine(image, dst, rotationMatrix, new Size((int) rotatedRect.width(), (int) rotatedRect.height()));

        return dst;
    }

    public static Mat fit(Mat image, int originalWidth, int origianlHeight) {
        final int x = (image.size().width() - originalWidth) / 2;
        final int y = (image.size().height() - origianlHeight) / 2;
        final Rect rectangle = new Rect(x, y, originalWidth, origianlHeight);
        return new Mat(image, rectangle);
    }

    public static Mat mirrow(Mat image) {
        final Mat dst = new Mat();
        flip(image, dst, 1);
        return dst;
    }
}
