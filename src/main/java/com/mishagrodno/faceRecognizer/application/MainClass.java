package com.mishagrodno.faceRecognizer.application;

import com.mishagrodno.faceRecognizer.FaceRecognizerApplication;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import com.mishagrodno.faceRecognizer.db.service.HumanService;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Application class.
 *
 * @author Gomanchuk Mikhail.
 */
@Component
public class MainClass {

    private final FaceService faceService;
    private final Recognizer recognizer;
    private final Detector detector;
    private final HumanService humanService;
    private final MainForm mainForm = new MainForm();
    private final double scale = 1.5;

    @Autowired
    public MainClass(FaceService faceService, Recognizer recognizer, Detector detector, HumanService humanService) {
        this.faceService = faceService;
        this.recognizer = recognizer;
        this.detector = detector;
        this.humanService = humanService;
    }

    /**
     * Starts face recognizing from webCam.
     */
    public void start() {
        try {
            final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
            grabber.setImageHeight(1280);
            grabber.setImageWidth(720);

            grabber.start();

            Frame grabbedImage = grabber.grab();

            mainForm.create(grabbedImage.imageHeight, grabbedImage.imageWidth);
            mainForm.getReloadButton().addActionListener(e -> recognizer.init());

            recognizer.init();
            final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();

            while (mainForm.getMainFrame().isVisible() && (grabbedImage = grabber.grab()) != null) {

                long start = System.currentTimeMillis();

                final Mat matImage = toMat.convert(grabbedImage);

                final Mat resized = new Mat();
                resize(matImage, resized, new Size((int) (matImage.size().width() / scale), (int) (matImage.size().height() / scale)));
                cvtColor(resized, resized, CV_BGR2GRAY);

                final List<Rect> eyes = detector.detectEyes(resized);

                final List<Boolean> used = new ArrayList<>();
                for (int i = 0; i < eyes.size(); i++) {
                    used.add(false);
                }

                final List<Rect> resultFaces = new ArrayList<>();

                for (int i = 0; i < eyes.size() - 1; i++) {
                    if (used.get(i)) {
                        continue;
                    }
                    for (int j = i + 1; j < eyes.size(); j++) {
                        if (used.get(j)) {
                            continue;
                        }
                        final Rect eye1 = eyes.get(i);
                        final Rect eye2 = eyes.get(j);

                        final double angle = faceAngle(eye1, eye2);
                        final Rect faceArea = faceArea(resized, eye1, eye2);

                        final Mat rotated = Utils.rotate(new Mat(resized.clone(), faceArea), Math.toDegrees(angle));

                        final List<Rect> faces = detector.detectFaces(rotated);
                        if (!CollectionUtils.isEmpty(faces)) {
                            final Rect face = recalc(faces.get(0), faceArea, angle);

                            //draw(eye1, matImage, 0.0, Scalar.RED);
                            //draw(eye2, matImage, 0.0, Scalar.RED);

                            System.out.println("------------------------------------------");

                            used.set(i, true);
                            used.set(j, true);

                            if (resultFaces.stream()
                                    .noneMatch(fc -> Utils.contains(fc, face))) {
                                resultFaces.add(face);
                                draw(face, matImage, angle, Scalar.BLUE);
                            }

                        }
                    }
                }

                final Mat normalSizedResult = new Mat();
                resize(matImage, normalSizedResult, new Size(matImage.size().width(), matImage.size().height()));

                mainForm.getImage().setIcon(new ImageIcon(Utils.convertToBufferedImage(normalSizedResult)));
                mainForm.getMainFrame().pack();

                System.out.println("Faces found: " + resultFaces.size() + ", took: " + (System.currentTimeMillis() - start));
            }
            grabber.stop();
            mainForm.getMainFrame().dispose();
            //Utils.safeDelete(Paths.get(frontalClassifierLocation));

        } catch (final Exception e) {
            Logger.getLogger(FaceRecognizerApplication.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private Rect faceArea(Mat image, Rect eye1, Rect eye2) {
        Rect left;
        if (eye1.x() < eye2.x()) {
            left = eye1;
        } else {
            left = eye2;
        }

        final int maxWidth = Math.max(eye1.width(), eye2.width());
        final int maxHeight = Math.max(eye1.height(), eye2.height());

        final int faceX = Math.max(0, left.x() - 2 * maxWidth);
        final int faceY = Math.max(0, left.y() - 4 * maxHeight);

        final int faceWidth = Math.max(0, Math.min(7 * maxWidth, image.size().width() - faceX));
        final int faceHeight = Math.max(0, Math.min(9 * maxHeight, image.size().height() - faceY));

        return new Rect(faceX, faceY, faceWidth, faceHeight);
    }

    private double faceAngle(Rect eye1, Rect eye2) {
        Rect left, right;
        if (eye1.x() < eye2.x()) {
            left = eye1;
            right = eye2;
        } else {
            left = eye2;
            right = eye1;
        }

        final int rightCenterX = right.x() + right.width() / 2;
        final int rightCenterY = right.y() + right.height() / 2;

        final int leftCenterX = left.x() + left.width() / 2;
        final int leftCenterY = left.y() + left.height() / 2;

        return Math.atan((double) (rightCenterY - leftCenterY) / (double) (rightCenterX - leftCenterX));
    }

    private Rect recalc(Rect face, Rect faceArea, double angle) {
        final int x = face.x();
        final int y = face.y();

        final int faceWidth = faceArea.width();
        final int faceHeight = faceArea.height();

        final double W = faceWidth * cos(angle) + Math.abs(faceHeight * sin(angle));

        double f;

        int newX = 0, newY = 0;

        if (angle <= 0) {
            angle = Math.toRadians(90) + angle;

            final double l = Math.sqrt((Math.pow(faceWidth * cos(angle) - y, 2) + (W - x) * (W - x)));
            final double b = Math.abs(Math.atan((faceWidth * cos(angle) - y) / (W - x)));

            if (y <= faceWidth * cos(angle)) {
                f = Math.toRadians(90) - angle - b;
                newX = (int) (faceArea.x() + faceWidth - l * cos(f));
                newY = (int) (faceArea.y() + l * sin(f));
            }

            if (y > faceWidth * cos(angle)) {
                f = angle - b;
                newX = (int) (faceArea.x() + faceWidth - l * sin(f));
                newY = (int) (faceArea.y() + l * cos(f));
            }

        } else {

            final double l = Math.sqrt((Math.pow(faceHeight * cos(angle) - y, 2) + (W - x) * (W - x)));
            final double b = Math.abs(Math.atan((faceHeight * cos(angle) - y) / (W - x)));

            if (y <= faceHeight * cos(angle)) {
                f = Math.toRadians(90) - angle - b;
                newX = (int) (faceArea.x() + faceWidth - l * sin(f));
                newY = (int) (faceArea.y() + faceHeight - l * cos(f));
            }

            if (y > faceHeight * cos(angle)) {
                f = angle - b;
                newX = (int) (faceArea.x() + faceWidth - l * cos(f));
                newY = (int) (faceArea.y() + faceHeight - l * sin(f));
            }
        }

        return new Rect((int) (newX * scale), (int) (newY * scale), (int) (face.width() * scale), (int) (face.width() * scale));
    }

    private void draw(Rect face, Mat image, double angle, Scalar color) {

        if (face.x() < 0 || face.y() < 0) {
            return;
        }

        final int x = face.x();
        final int y = face.y();
        final int w = face.width();
        final int h = face.height();

        final Point ru = new Point((int) (x + w * cos(angle)), (int) (y + w * sin(angle)));
        final Point ld = new Point((int) (x - h * sin(angle)), (int) (y + h * cos(angle)));
        final Point rd = new Point((int) (ld.x() + w * cos(angle)), (int) (ld.y() + w * sin(angle)));

        line(image, new Point(x, y), ru, color, 3, CV_AA, 0);
        line(image, new Point(x, y), ld, color, 3, CV_AA, 0);
        line(image, ld, rd, color, 3, CV_AA, 0);
        line(image, ru, rd, color, 3, CV_AA, 0);
    }
}
